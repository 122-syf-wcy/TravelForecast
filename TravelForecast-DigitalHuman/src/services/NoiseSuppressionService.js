/**
 * 音频降噪服务 (NoiseSuppressionService)
 * 智教黔行 - 3D数字人研学导师
 * 
 * 基于 RNNoise 的 WebAssembly 降噪模块
 * 适用于户外研学场景的背景噪音抑制
 */

/**
 * RNNoise 降噪处理器
 * 
 * 使用说明：
 * 1. 需要加载 rnnoise-wasm 库
 * 2. 在麦克风音频流接入前调用 process()
 * 3. 支持实时流处理
 */
class NoiseSuppressionService {
    constructor() {
        // 降噪状态
        this.enabled = true;
        this.initialized = false;

        // RNNoise 模块
        this.rnnoiseModule = null;
        this.denoiseState = null;

        // 音频处理参数
        this.frameSize = 480;  // RNNoise 需要 480 样本帧 (10ms @ 48kHz)
        this.sampleRate = 48000;

        // 输入输出缓冲区
        this.inputBuffer = new Float32Array(this.frameSize);
        this.outputBuffer = new Float32Array(this.frameSize);

        // 处理统计
        this.framesProcessed = 0;
        this.vadProbability = 0;
    }

    /**
     * 初始化降噪模块
     * @returns {Promise<boolean>} 是否初始化成功
     */
    async init() {
        if (this.initialized) {
            return true;
        }

        try {
            // 动态加载 RNNoise WASM 模块
            // 使用 CDN 或本地部署的 rnnoise-wasm
            const rnnoiseUrl = 'https://cdn.jsdelivr.net/npm/rnnoise-wasm@0.3.0/dist/rnnoise.js';

            // 尝试加载模块
            await this._loadRNNoiseModule(rnnoiseUrl);

            this.initialized = true;
            console.log('[NoiseSuppression] 初始化成功');
            return true;

        } catch (error) {
            console.warn('[NoiseSuppression] 初始化失败，将使用原始音频:', error);
            this.initialized = false;
            return false;
        }
    }

    /**
     * 加载 RNNoise WASM 模块
     * @private
     */
    async _loadRNNoiseModule(url) {
        return new Promise((resolve, reject) => {
            // 检查是否已加载
            if (window.RNNoise) {
                this.rnnoiseModule = window.RNNoise;
                this._initDenoiseState();
                resolve();
                return;
            }

            // 动态加载脚本
            const script = document.createElement('script');
            script.src = url;
            script.onload = () => {
                if (window.RNNoise) {
                    this.rnnoiseModule = window.RNNoise;
                    this._initDenoiseState();
                    resolve();
                } else {
                    reject(new Error('RNNoise module not found after loading'));
                }
            };
            script.onerror = () => reject(new Error('Failed to load RNNoise script'));
            document.head.appendChild(script);
        });
    }

    /**
     * 初始化降噪状态
     * @private
     */
    _initDenoiseState() {
        if (this.rnnoiseModule && this.rnnoiseModule.create) {
            this.denoiseState = this.rnnoiseModule.create();
            console.log('[NoiseSuppression] 降噪状态已创建');
        }
    }

    /**
     * 处理音频帧进行降噪
     * @param {Float32Array} inputFrame 输入音频帧 (480 样本)
     * @returns {Float32Array} 降噪后的音频帧
     */
    processFrame(inputFrame) {
        if (!this.enabled || !this.initialized || !this.denoiseState) {
            return inputFrame;
        }

        try {
            // 确保输入长度正确
            if (inputFrame.length !== this.frameSize) {
                console.warn(`[NoiseSuppression] 帧大小不匹配: ${inputFrame.length} != ${this.frameSize}`);
                return inputFrame;
            }

            // 复制输入到缓冲区
            this.inputBuffer.set(inputFrame);

            // 调用 RNNoise 处理
            // RNNoise 返回 VAD 概率 (0-1)
            this.vadProbability = this.rnnoiseModule.process(
                this.denoiseState,
                this.inputBuffer,
                this.outputBuffer
            );

            this.framesProcessed++;

            // 返回降噪后的音频
            return new Float32Array(this.outputBuffer);

        } catch (error) {
            console.error('[NoiseSuppression] 处理帧时出错:', error);
            return inputFrame;
        }
    }

    /**
     * 创建 AudioWorklet 处理器
     * 用于 Web Audio API 集成
     * @param {AudioContext} audioContext
     * @returns {Promise<AudioWorkletNode>}
     */
    async createWorkletNode(audioContext) {
        // AudioWorklet 处理器代码
        const processorCode = `
            class NoiseSuppressionProcessor extends AudioWorkletProcessor {
                constructor() {
                    super();
                    this.enabled = true;
                    this.port.onmessage = (e) => {
                        if (e.data.type === 'enable') {
                            this.enabled = e.data.value;
                        }
                    };
                }
                
                process(inputs, outputs, parameters) {
                    const input = inputs[0];
                    const output = outputs[0];
                    
                    if (input.length > 0 && output.length > 0) {
                        for (let channel = 0; channel < input.length; channel++) {
                            const inputData = input[channel];
                            const outputData = output[channel];
                            
                            if (this.enabled) {
                                // 简单的噪声门限处理（备用方案）
                                const threshold = 0.01;
                                for (let i = 0; i < inputData.length; i++) {
                                    if (Math.abs(inputData[i]) < threshold) {
                                        outputData[i] = 0;
                                    } else {
                                        outputData[i] = inputData[i];
                                    }
                                }
                            } else {
                                outputData.set(inputData);
                            }
                        }
                    }
                    return true;
                }
            }
            registerProcessor('noise-suppression-processor', NoiseSuppressionProcessor);
        `;

        // 创建 Blob URL
        const blob = new Blob([processorCode], { type: 'application/javascript' });
        const url = URL.createObjectURL(blob);

        try {
            // 注册 AudioWorklet
            await audioContext.audioWorklet.addModule(url);

            // 创建节点
            const node = new AudioWorkletNode(audioContext, 'noise-suppression-processor');

            console.log('[NoiseSuppression] AudioWorklet 节点已创建');
            return node;

        } finally {
            URL.revokeObjectURL(url);
        }
    }

    /**
     * 创建 ScriptProcessor 节点（备用方案）
     * 用于不支持 AudioWorklet 的浏览器
     * @param {AudioContext} audioContext
     * @param {number} bufferSize 缓冲区大小
     * @returns {ScriptProcessorNode}
     */
    createScriptProcessorNode(audioContext, bufferSize = 4096) {
        const processor = audioContext.createScriptProcessor(bufferSize, 1, 1);

        processor.onaudioprocess = (event) => {
            const inputData = event.inputBuffer.getChannelData(0);
            const outputData = event.outputBuffer.getChannelData(0);

            if (this.enabled && this.initialized) {
                // 分帧处理
                for (let i = 0; i < inputData.length; i += this.frameSize) {
                    const frame = inputData.slice(i, i + this.frameSize);
                    if (frame.length === this.frameSize) {
                        const processed = this.processFrame(frame);
                        outputData.set(processed, i);
                    } else {
                        outputData.set(frame, i);
                    }
                }
            } else {
                outputData.set(inputData);
            }
        };

        console.log('[NoiseSuppression] ScriptProcessor 节点已创建');
        return processor;
    }

    /**
     * 获取 VAD 概率
     * @returns {number} 语音活动概率 (0-1)
     */
    getVADProbability() {
        return this.vadProbability;
    }

    /**
     * 启用/禁用降噪
     * @param {boolean} enabled
     */
    setEnabled(enabled) {
        this.enabled = enabled;
        console.log(`[NoiseSuppression] ${enabled ? '已启用' : '已禁用'}`);
    }

    /**
     * 获取处理统计
     * @returns {Object}
     */
    getStats() {
        return {
            framesProcessed: this.framesProcessed,
            vadProbability: this.vadProbability,
            enabled: this.enabled,
            initialized: this.initialized
        };
    }

    /**
     * 销毁降噪模块
     */
    destroy() {
        if (this.denoiseState && this.rnnoiseModule && this.rnnoiseModule.destroy) {
            this.rnnoiseModule.destroy(this.denoiseState);
            this.denoiseState = null;
        }
        this.initialized = false;
        console.log('[NoiseSuppression] 已销毁');
    }
}

export default NoiseSuppressionService;
