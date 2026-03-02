/**
 * 音频处理服务 (AudioProcessor)
 * 智教黔行 - 3D数字人研学导师
 * 
 * 功能：
 * - 麦克风音频采集
 * - VAD语音活动检测
 * - 音频编解码
 * - 降噪处理
 */

// VAD配置
const VAD_CONFIG = {
    energyThreshold: 0.01,      // 能量阈值
    silenceTimeout: 1500,        // 静音超时（毫秒）
    minSpeechDuration: 200,      // 最小语音持续时间（毫秒）
    frameDuration: 30            // 帧时长（毫秒）
};

export class AudioProcessor {
    constructor(options = {}) {
        this.config = { ...VAD_CONFIG, ...options };

        // 音频上下文
        this.audioContext = null;
        this.mediaStream = null;
        this.sourceNode = null;
        this.analyserNode = null;
        this.processorNode = null;

        // VAD状态
        this.isSpeaking = false;
        this.silenceStart = null;
        this.speechStart = null;

        // 录音数据
        this.audioChunks = [];
        this.isRecording = false;

        // 回调函数
        this.onSpeechStart = null;
        this.onSpeechEnd = null;
        this.onAudioData = null;
        this.onVolumeChange = null;
        this.onError = null;

        console.log('🎙️ AudioProcessor 初始化完成');
    }

    /**
     * 请求麦克风权限并初始化
     */
    async init() {
        try {
            // 请求麦克风权限
            this.mediaStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                    sampleRate: 16000
                }
            });

            // 创建音频上下文
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)({
                sampleRate: 16000
            });

            // 创建音频源节点
            this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream);

            // 创建分析器节点（用于VAD和可视化）
            this.analyserNode = this.audioContext.createAnalyser();
            this.analyserNode.fftSize = 256;
            this.analyserNode.smoothingTimeConstant = 0.5;

            // 连接节点
            this.sourceNode.connect(this.analyserNode);

            console.log('✅ 麦克风初始化成功');
            return true;
        } catch (error) {
            console.error('❌ 麦克风初始化失败:', error);
            if (this.onError) {
                this.onError(error);
            }
            return false;
        }
    }

    /**
     * 开始录音
     */
    startRecording() {
        if (!this.mediaStream) {
            console.error('请先调用 init() 初始化');
            return false;
        }

        this.audioChunks = [];
        this.isRecording = true;
        this.isSpeaking = false;
        this.silenceStart = null;
        this.speechStart = null;

        // 使用 MediaRecorder 录制音频
        try {
            this.mediaRecorder = new MediaRecorder(this.mediaStream, {
                mimeType: 'audio/webm;codecs=opus'
            });

            this.mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    this.audioChunks.push(event.data);
                    if (this.onAudioData) {
                        this.onAudioData(event.data);
                    }
                }
            };

            this.mediaRecorder.start(100); // 每100ms产生一个chunk

            // 启动VAD检测循环
            this._startVAD();

            console.log('🔴 开始录音');
            return true;
        } catch (error) {
            console.error('❌ 录音启动失败:', error);
            return false;
        }
    }

    /**
     * 停止录音
     * @returns {Promise<Blob>} 录音数据
     */
    async stopRecording() {
        if (!this.isRecording) return null;

        this.isRecording = false;
        this._stopVAD();

        return new Promise((resolve) => {
            if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
                this.mediaRecorder.onstop = () => {
                    const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
                    console.log('⏹️ 录音结束，大小:', Math.round(audioBlob.size / 1024), 'KB');
                    resolve(audioBlob);
                };
                this.mediaRecorder.stop();
            } else {
                resolve(null);
            }
        });
    }

    /**
     * 获取实时音量（0-1）
     */
    getVolume() {
        if (!this.analyserNode) return 0;

        const dataArray = new Uint8Array(this.analyserNode.frequencyBinCount);
        this.analyserNode.getByteFrequencyData(dataArray);

        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
            sum += dataArray[i];
        }

        return sum / (dataArray.length * 255);
    }

    /**
     * 获取频谱数据（用于可视化）
     * @returns {Uint8Array}
     */
    getFrequencyData() {
        if (!this.analyserNode) return new Uint8Array(0);

        const dataArray = new Uint8Array(this.analyserNode.frequencyBinCount);
        this.analyserNode.getByteFrequencyData(dataArray);
        return dataArray;
    }

    /**
     * 获取波形数据
     * @returns {Uint8Array}
     */
    getWaveformData() {
        if (!this.analyserNode) return new Uint8Array(0);

        const dataArray = new Uint8Array(this.analyserNode.fftSize);
        this.analyserNode.getByteTimeDomainData(dataArray);
        return dataArray;
    }

    /**
     * 将录音转换为Base64
     * @param {Blob} audioBlob 
     * @returns {Promise<string>}
     */
    async blobToBase64(audioBlob) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onloadend = () => {
                const base64 = reader.result.split(',')[1];
                resolve(base64);
            };
            reader.onerror = reject;
            reader.readAsDataURL(audioBlob);
        });
    }

    /**
     * 将Base64转换为AudioBuffer
     * @param {string} base64 
     * @returns {Promise<AudioBuffer>}
     */
    async base64ToAudioBuffer(base64) {
        const binaryString = atob(base64);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        return await this.audioContext.decodeAudioData(bytes.buffer);
    }

    /**
     * 播放音频
     * @param {Blob|ArrayBuffer|string} audio 音频数据
     * @returns {Promise<HTMLAudioElement>}
     */
    async playAudio(audio) {
        let audioUrl;

        if (audio instanceof Blob) {
            audioUrl = URL.createObjectURL(audio);
        } else if (typeof audio === 'string') {
            // 假设是Base64或URL
            if (audio.startsWith('data:') || audio.startsWith('http')) {
                audioUrl = audio;
            } else {
                audioUrl = `data:audio/wav;base64,${audio}`;
            }
        } else {
            throw new Error('不支持的音频格式');
        }

        const audioElement = new Audio(audioUrl);

        return new Promise((resolve, reject) => {
            audioElement.onended = () => {
                if (audio instanceof Blob) {
                    URL.revokeObjectURL(audioUrl);
                }
                resolve(audioElement);
            };
            audioElement.onerror = reject;
            audioElement.play();
        });
    }

    // ==================== VAD 相关方法 ====================

    /**
     * 启动VAD检测
     */
    _startVAD() {
        this.vadInterval = setInterval(() => {
            this._processVAD();
        }, this.config.frameDuration);
    }

    /**
     * 停止VAD检测
     */
    _stopVAD() {
        if (this.vadInterval) {
            clearInterval(this.vadInterval);
            this.vadInterval = null;
        }
    }

    /**
     * VAD处理逻辑
     */
    _processVAD() {
        const volume = this.getVolume();
        const now = Date.now();

        // 触发音量变化回调
        if (this.onVolumeChange) {
            this.onVolumeChange(volume);
        }

        if (volume > this.config.energyThreshold) {
            // 检测到语音
            this.silenceStart = null;

            if (!this.isSpeaking) {
                if (!this.speechStart) {
                    this.speechStart = now;
                } else if (now - this.speechStart > this.config.minSpeechDuration) {
                    // 确认语音开始
                    this.isSpeaking = true;
                    if (this.onSpeechStart) {
                        this.onSpeechStart();
                    }
                    console.log('🗣️ 检测到语音开始');
                }
            }
        } else {
            // 静音
            this.speechStart = null;

            if (this.isSpeaking) {
                if (!this.silenceStart) {
                    this.silenceStart = now;
                } else if (now - this.silenceStart > this.config.silenceTimeout) {
                    // 确认语音结束
                    this.isSpeaking = false;
                    if (this.onSpeechEnd) {
                        this.onSpeechEnd();
                    }
                    console.log('🤫 检测到语音结束');
                }
            }
        }
    }

    /**
     * 销毁处理器
     */
    destroy() {
        this._stopVAD();

        if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
            this.mediaRecorder.stop();
        }

        if (this.mediaStream) {
            this.mediaStream.getTracks().forEach(track => track.stop());
            this.mediaStream = null;
        }

        if (this.audioContext) {
            this.audioContext.close();
            this.audioContext = null;
        }

        this.audioChunks = [];
        console.log('🔌 AudioProcessor 已销毁');
    }
}
