/**
 * 口型同步引擎 (LipSyncEngine)
 * 智教黔行 - 3D数字人研学导师
 * 
 * 支持两种模式：
 * 1. 高精度模式：基于Viseme事件驱动（需要TTS元数据）
 * 2. 低延迟模式：基于音频频谱分析（本地处理）
 */

// ARKit兼容的BlendShapes标准名称
const BLENDSHAPES = {
    jawOpen: 'jawOpen',
    mouthFunnel: 'mouthFunnel',
    mouthPucker: 'mouthPucker',
    mouthLeft: 'mouthLeft',
    mouthRight: 'mouthRight',
    mouthSmileLeft: 'mouthSmileLeft',
    mouthSmileRight: 'mouthSmileRight',
    mouthFrownLeft: 'mouthFrownLeft',
    mouthFrownRight: 'mouthFrownRight',
    mouthDimpleLeft: 'mouthDimpleLeft',
    mouthDimpleRight: 'mouthDimpleRight',
    mouthStretchLeft: 'mouthStretchLeft',
    mouthStretchRight: 'mouthStretchRight',
    mouthRollLower: 'mouthRollLower',
    mouthRollUpper: 'mouthRollUpper',
    mouthShrugLower: 'mouthShrugLower',
    mouthShrugUpper: 'mouthShrugUpper',
    mouthPressLeft: 'mouthPressLeft',
    mouthPressRight: 'mouthPressRight',
    mouthLowerDownLeft: 'mouthLowerDownLeft',
    mouthLowerDownRight: 'mouthLowerDownRight',
    mouthUpperUpLeft: 'mouthUpperUpLeft',
    mouthUpperUpRight: 'mouthUpperUpRight',
    mouthClose: 'mouthClose'
};

// Viseme ID 到 BlendShapes 的映射表（Azure Speech标准）
const VISEME_MAP = {
    0: { name: 'Silence', shapes: {} },
    1: { name: 'AE_AX_AH', shapes: { jawOpen: 0.8, mouthFunnel: 0.3 } },
    2: { name: 'AA', shapes: { jawOpen: 0.9, mouthFunnel: 0.1 } },
    3: { name: 'AO', shapes: { jawOpen: 0.7, mouthPucker: 0.3 } },
    4: { name: 'EY_EH_UH', shapes: { jawOpen: 0.5, mouthSmileLeft: 0.4, mouthSmileRight: 0.4 } },
    5: { name: 'ER', shapes: { jawOpen: 0.4, mouthPucker: 0.5 } },
    6: { name: 'IY_IH_Y', shapes: { jawOpen: 0.3, mouthSmileLeft: 0.6, mouthSmileRight: 0.6, mouthStretchLeft: 0.3, mouthStretchRight: 0.3 } },
    7: { name: 'W_UW', shapes: { mouthPucker: 1.0, jawOpen: 0.4 } },
    8: { name: 'OW', shapes: { mouthPucker: 0.8, jawOpen: 0.5 } },
    9: { name: 'AW', shapes: { jawOpen: 0.7, mouthPucker: 0.4 } },
    10: { name: 'OY', shapes: { mouthPucker: 0.6, jawOpen: 0.5, mouthSmileLeft: 0.2, mouthSmileRight: 0.2 } },
    11: { name: 'AY', shapes: { jawOpen: 0.6, mouthSmileLeft: 0.3, mouthSmileRight: 0.3 } },
    12: { name: 'H', shapes: { jawOpen: 0.3 } },
    13: { name: 'R', shapes: { mouthPucker: 0.4, jawOpen: 0.3 } },
    14: { name: 'L', shapes: { jawOpen: 0.4, mouthFunnel: 0.2 } },
    15: { name: 'S_Z', shapes: { jawOpen: 0.2, mouthSmileLeft: 0.3, mouthSmileRight: 0.3, mouthStretchLeft: 0.2, mouthStretchRight: 0.2 } },
    16: { name: 'SH_CH_JH_ZH', shapes: { mouthPucker: 0.5, jawOpen: 0.3 } },
    17: { name: 'TH_DH', shapes: { jawOpen: 0.3, mouthFunnel: 0.4 } },
    18: { name: 'F_V', shapes: { mouthRollLower: 0.6, jawOpen: 0.2 } },
    19: { name: 'D_T_N', shapes: { jawOpen: 0.3, mouthClose: 0.1 } },
    20: { name: 'K_G_NG', shapes: { jawOpen: 0.4 } },
    21: { name: 'P_B_M', shapes: { mouthRollUpper: 0.6, mouthRollLower: 0.6, mouthClose: 0.8 } }
};

// 中文音素映射（简化版，用于频谱分析模式）
const CHINESE_PHONEME_MAP = {
    // 韵母
    'a': { jawOpen: 0.9, mouthFunnel: 0.2 },
    'o': { jawOpen: 0.6, mouthPucker: 0.7 },
    'e': { jawOpen: 0.5, mouthSmileLeft: 0.3, mouthSmileRight: 0.3 },
    'i': { jawOpen: 0.2, mouthSmileLeft: 0.6, mouthSmileRight: 0.6 },
    'u': { mouthPucker: 1.0, jawOpen: 0.3 },
    'ü': { mouthPucker: 0.8, jawOpen: 0.2, mouthSmileLeft: 0.2, mouthSmileRight: 0.2 },
    // 闭合音
    'b': { mouthClose: 0.9, mouthRollLower: 0.5 },
    'p': { mouthClose: 0.9, mouthRollLower: 0.5 },
    'm': { mouthClose: 1.0 },
    // 默认
    'default': { jawOpen: 0.4 }
};

export class LipSyncEngine {
    constructor(options = {}) {
        // 配置参数
        this.smoothingFactor = options.smoothingFactor || 0.15; // 平滑因子α
        this.mode = options.mode || 'auto'; // 'viseme', 'spectrum', 'auto'
        this.sampleRate = options.sampleRate || 44100;

        // 状态
        this.currentWeights = {};
        this.targetWeights = {};
        this.visemeQueue = [];
        this.isPlaying = false;

        // 音频分析器（用于频谱模式）
        this.audioContext = null;
        this.analyser = null;
        this.dataArray = null;

        // 回调
        this.onWeightsUpdate = options.onWeightsUpdate || null;

        // 初始化所有权重为0
        this._resetWeights();

        console.log('🎤 LipSyncEngine 初始化完成，模式:', this.mode);
    }

    /**
     * 初始化音频分析器（频谱模式）
     */
    async initAudioAnalyser() {
        if (this.audioContext) return;

        try {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 256;
            this.analyser.smoothingTimeConstant = 0.8;

            const bufferLength = this.analyser.frequencyBinCount;
            this.dataArray = new Uint8Array(bufferLength);

            console.log('📊 音频分析器初始化成功');
        } catch (error) {
            console.error('❌ 音频分析器初始化失败:', error);
        }
    }

    /**
     * 连接音频源到分析器
     * @param {HTMLAudioElement|MediaStream} source 音频源
     */
    connectAudioSource(source) {
        if (!this.audioContext) {
            console.error('请先调用 initAudioAnalyser()');
            return;
        }

        let sourceNode;
        if (source instanceof HTMLAudioElement) {
            sourceNode = this.audioContext.createMediaElementSource(source);
        } else if (source instanceof MediaStream) {
            sourceNode = this.audioContext.createMediaStreamSource(source);
        } else {
            console.error('不支持的音频源类型');
            return;
        }

        sourceNode.connect(this.analyser);
        this.analyser.connect(this.audioContext.destination);

        console.log('🔗 音频源已连接');
    }

    /**
     * 添加Viseme事件到队列（高精度模式）
     * @param {Array} visemeData Viseme数据数组 [{id, audioOffset}, ...]
     */
    addVisemeData(visemeData) {
        if (Array.isArray(visemeData)) {
            this.visemeQueue.push(...visemeData);
            this.visemeQueue.sort((a, b) => a.audioOffset - b.audioOffset);
        }
        console.log(`📦 添加 ${visemeData.length} 个Viseme事件`);
    }

    /**
     * 清空Viseme队列
     */
    clearVisemeQueue() {
        this.visemeQueue = [];
        this._resetWeights();
    }

    /**
     * 根据当前时间更新口型（Viseme模式）
     * @param {number} currentTime 当前音频时间（秒）
     */
    updateFromViseme(currentTime) {
        const currentTimeMs = currentTime * 1000;

        // 查找当前时间应该显示的Viseme
        let activeViseme = null;
        for (let i = 0; i < this.visemeQueue.length; i++) {
            if (this.visemeQueue[i].audioOffset <= currentTimeMs) {
                activeViseme = this.visemeQueue[i];
            } else {
                break;
            }
        }

        if (activeViseme) {
            const mapping = VISEME_MAP[activeViseme.id] || VISEME_MAP[0];
            this.targetWeights = { ...mapping.shapes };
        } else {
            this.targetWeights = {};
        }

        this._smoothUpdate();
    }

    /**
     * 从音频频谱分析更新口型（低延迟模式）
     */
    updateFromSpectrum() {
        if (!this.analyser || !this.dataArray) return;

        this.analyser.getByteFrequencyData(this.dataArray);

        // 计算各频段能量
        const lowFreq = this._getFrequencyEnergy(0, 8);      // 低频 (~200Hz) - 元音
        const midFreq = this._getFrequencyEnergy(8, 32);     // 中频 - 共振峰
        const highFreq = this._getFrequencyEnergy(32, 64);   // 高频 (~4kHz) - 辅音

        // 总能量（用于判断是否有声音）
        const totalEnergy = (lowFreq + midFreq + highFreq) / 3;

        if (totalEnergy < 10) {
            // 静音状态
            this.targetWeights = {};
        } else {
            // 根据频谱特征映射口型
            const normalizedLow = Math.min(lowFreq / 200, 1);
            const normalizedMid = Math.min(midFreq / 180, 1);
            const normalizedHigh = Math.min(highFreq / 150, 1);

            this.targetWeights = {
                // 下颌开合主要由低频决定（降低幅度，更自然）
                jawOpen: normalizedLow * 0.35,
                // 嘴型由中频决定
                mouthFunnel: normalizedMid * 0.2,
                // 嘴唇收缩由混合频率决定
                mouthPucker: (normalizedLow * 0.3 + normalizedMid * 0.4) * 0.25,
                // 高频影响嘴角（微笑）
                mouthSmileLeft: normalizedHigh * 0.15,
                mouthSmileRight: normalizedHigh * 0.15
            };
        }

        this._smoothUpdate();
    }

    /**
     * 每帧调用的主更新函数
     * @param {number} deltaTime 帧间隔时间（秒）
     * @param {number} audioCurrentTime 当前音频播放时间（秒，可选）
     */
    update(deltaTime, audioCurrentTime = null) {
        if (this.mode === 'viseme' && audioCurrentTime !== null) {
            this.updateFromViseme(audioCurrentTime);
        } else if (this.mode === 'spectrum') {
            this.updateFromSpectrum();
        } else if (this.mode === 'auto') {
            // 自动模式：有Viseme数据时用Viseme，否则用频谱
            if (this.visemeQueue.length > 0 && audioCurrentTime !== null) {
                this.updateFromViseme(audioCurrentTime);
            } else {
                this.updateFromSpectrum();
            }
        }

        // 触发回调
        if (this.onWeightsUpdate) {
            this.onWeightsUpdate(this.currentWeights);
        }

        return this.currentWeights;
    }

    /**
     * 获取当前BlendShape权重
     */
    getWeights() {
        return { ...this.currentWeights };
    }

    /**
     * 设置模式
     * @param {'viseme'|'spectrum'|'auto'} mode 
     */
    setMode(mode) {
        this.mode = mode;
        console.log('🔄 LipSync模式切换为:', mode);
    }

    /**
     * 设置平滑因子
     * @param {number} factor 0-1之间的值，越小越平滑
     */
    setSmoothingFactor(factor) {
        this.smoothingFactor = Math.max(0.05, Math.min(0.5, factor));
    }

    // ==================== 私有方法 ====================

    /**
     * 重置所有权重为0
     */
    _resetWeights() {
        this.currentWeights = {};
        this.targetWeights = {};
        Object.keys(BLENDSHAPES).forEach(key => {
            this.currentWeights[key] = 0;
            this.targetWeights[key] = 0;
        });
    }

    /**
     * 平滑插值更新当前权重
     * V_current = V_last × (1 - α) + V_target × α
     */
    _smoothUpdate() {
        const alpha = this.smoothingFactor;

        Object.keys(BLENDSHAPES).forEach(key => {
            const target = this.targetWeights[key] || 0;
            const current = this.currentWeights[key] || 0;
            this.currentWeights[key] = current * (1 - alpha) + target * alpha;

            // 清理极小值
            if (Math.abs(this.currentWeights[key]) < 0.001) {
                this.currentWeights[key] = 0;
            }
        });
    }

    /**
     * 计算指定频段的平均能量
     */
    _getFrequencyEnergy(startBin, endBin) {
        if (!this.dataArray) return 0;

        let sum = 0;
        const count = endBin - startBin;
        for (let i = startBin; i < endBin && i < this.dataArray.length; i++) {
            sum += this.dataArray[i];
        }
        return sum / count;
    }

    /**
     * 销毁引擎，释放资源
     */
    destroy() {
        if (this.audioContext) {
            this.audioContext.close();
            this.audioContext = null;
        }
        this.analyser = null;
        this.dataArray = null;
        this._resetWeights();
        console.log('🔌 LipSyncEngine 已销毁');
    }
}

// 导出常量供外部使用
export { BLENDSHAPES, VISEME_MAP, CHINESE_PHONEME_MAP };
