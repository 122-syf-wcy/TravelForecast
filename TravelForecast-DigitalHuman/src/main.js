/**
 * 智教黔行 - 前端主入口
 * 集成地图、数字人、语音交互与WebSocket通信
 */

import * as THREE from 'three';
import { AvatarController, ANIMATION_STATES } from './core/AvatarController.js';
import { AudioProcessor } from './services/AudioProcessor.js';
import { WebSocketService, MESSAGE_TYPES, CONNECTION_STATE } from './services/WebSocketService.js';

// 配置参数
const CONFIG = {
    // 地图中心（六盘水）
    center: [104.83, 26.59],
    zoom: 18,
    pitch: 60,

    // 数字人位置
    modelPosition: [104.83, 26.5895], // 微调纬度，让模型在屏幕上往下一点
    modelAltitude: 0,
    modelUrl: './rpm_avatar.glb',

    // WebSocket地址
    wsUrl: 'ws://localhost:8000/ws/avatar'
};

class App {
    constructor() {
        this.map = null;
        this.avatarController = null;
        this.audioProcessor = null;
        this.wsService = null;
        this.clock = new THREE.Clock(); // 添加时钟用于计算deltaTime

        this.isRecording = false;

        // UI元素
        this.ui = {
            status: document.getElementById('status'),
            statusText: document.getElementById('status-text'),
            chatHistory: document.getElementById('chat-history'),
            micBtn: document.getElementById('mic-btn'),
            input: document.getElementById('text-input'),
            sendBtn: document.getElementById('send-btn'),
            visualizer: document.getElementById('audio-visualizer')
        };

        this.init();
    }

    async init() {
        try {
            this.updateStatus('正在初始化地图...', true);
            await this.initMap();

            this.updateStatus('正在连接服务...', true);
            this.initServices();

            this.updateStatus('正在加载数字人...', true);
            // 数字人加载在地图自定义图层中处理

            this.initUI();

        } catch (error) {
            console.error('初始化失败:', error);
            this.updateStatus('初始化失败: ' + error.message, false);
        }
    }

    /**
     * 初始化地图
     */
    initMap() {
        return new Promise((resolve) => {
            this.map = new maplibregl.Map({
                container: 'map',
                // 使用 CartoDB 栅格瓦片，加载速度更快且无需Token
                style: {
                    "version": 8,
                    "sources": {
                        "carto-light": {
                            "type": "raster",
                            "tiles": [
                                "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                                "https://b.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                                "https://c.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                                "https://d.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
                            ],
                            "tileSize": 256,
                            "attribution": "&copy; CartoDB"
                        }
                    },
                    "layers": [
                        {
                            "id": "carto-light",
                            "type": "raster",
                            "source": "carto-light",
                            "minzoom": 0,
                            "maxzoom": 19
                        }
                    ]
                },
                center: CONFIG.center,
                zoom: CONFIG.zoom,
                pitch: CONFIG.pitch,
                bearing: 0,
                antialias: true
            });

            this.map.on('load', () => {
                console.log('🗺️ 地图加载完成');
                this.addAvatarLayer();
                resolve();
            });
        });
    }

    /**
     * 初始化服务
     */
    initServices() {
        // 1. 音频处理器
        this.audioProcessor = new AudioProcessor({
            onSpeechStart: () => {
                console.log('检测到语音开始');
                this.ui.visualizer.classList.add('active');
            },
            onSpeechEnd: () => {
                console.log('检测到语音结束');
                this.ui.visualizer.classList.remove('active');
                // 自动停止录音并发送
                if (this.isRecording) {
                    this.stopRecording();
                }
            },
            onVolumeChange: (volume) => {
                this.updateVisualizer(volume);
            }
        });

        // 2. WebSocket服务
        this.wsService = new WebSocketService({
            url: CONFIG.wsUrl
        });

        // 绑定WS事件
        this.wsService.on('onConnect', () => {
            this.addSystemMessage('服务器已连接');
        });

        this.wsService.on('onDisconnect', () => {
            this.addSystemMessage('服务器断开连接');
        });

        this.wsService.on('onTextOutput', (data) => {
            // 移除打字动画
            this.removeTypingIndicator();

            // 只在完整回复时添加消息（避免重复显示部分内容）
            if (data && !data.isPartial && data.text) {
                console.log('[AI回复]', data.text.slice(0, 50) + (data.text.length > 50 ? '...' : ''));
                this.addMessage(data.text, 'ai');
                this.updateStatus('黔小游已就位', false);
            }
        });

        // 处理状态消息
        this.wsService.on('onMessage', (message) => {
            if (message.type === 'status') {
                const { status, message: msg } = message.data || {};
                if (status === 'thinking') {
                    this.showTypingIndicator();
                    this.updateStatus('正在思考...', true);
                } else if (status === 'synthesizing') {
                    this.updateStatus('正在合成语音...', true);
                } else if (status === 'idle') {
                    this.removeTypingIndicator();
                    this.updateStatus('黔小游已就位', false);
                }
            }
        });

        this.wsService.on('onAudioOutput', async (audioData) => {
            // 播放音频
            // TODO: 将音频数据传给LipSyncEngine
            console.log('收到音频数据', audioData.byteLength);

            // 临时：直接播放
            const blob = new Blob([audioData], { type: 'audio/mp3' }); // 假设是MP3
            const audio = new Audio(URL.createObjectURL(blob));

            // 启动口型同步（这里简化处理，实际应该用AudioContext分析）
            if (this.avatarController) {
                this.avatarController.startLipSync(audio);
            }

            await audio.play();

            audio.onended = () => {
                if (this.avatarController) {
                    this.avatarController.stopLipSync();
                }
            };
        });

        this.wsService.on('onViseme', (visemeData) => {
            // 如果有精确的Viseme数据，传给控制器
            if (this.avatarController) {
                this.avatarController.getLipSyncEngine().addVisemeData(visemeData);
            }
        });

        // 连接WS
        this.wsService.connect();
    }

    /**
     * 添加数字人图层
     */
    addAvatarLayer() {
        const customLayer = {
            id: '3d-model',
            type: 'custom',
            renderingMode: '3d',

            onAdd: (map, gl) => {
                this.camera = new THREE.Camera();
                this.scene = new THREE.Scene();

                // 灯光
                const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
                this.scene.add(ambientLight);
                const dirLight = new THREE.DirectionalLight(0xffffff, 1.5);
                dirLight.position.set(100, 100, 100);
                this.scene.add(dirLight);

                // 初始化渲染器
                this.renderer = new THREE.WebGLRenderer({
                    canvas: map.getCanvas(),
                    context: gl,
                    antialias: true
                });
                this.renderer.autoClear = false;

                // 初始化数字人控制器
                this.avatarController = new AvatarController(this.scene, {
                    modelScale: 100, // RPM模型需要较大的缩放
                    lipSyncMode: 'spectrum' // 默认使用频谱模式，兼容性更好
                });

                this.avatarController.onLoadComplete = () => {
                    this.updateStatus('黔小游已就位', false);
                    this.addSystemMessage('你好！我是黔小游，你的研学导师。');
                };

                this.avatarController.loadModel(CONFIG.modelUrl);
            },

            render: (gl, matrix) => {
                // 坐标同步
                const mercator = maplibregl.MercatorCoordinate.fromLngLat(
                    CONFIG.modelPosition,
                    CONFIG.modelAltitude
                );
                const scale = mercator.meterInMercatorCoordinateUnits();

                const modelMatrix = new THREE.Matrix4()
                    .makeTranslation(mercator.x, mercator.y, mercator.z)
                    .scale(new THREE.Vector3(scale, -scale, scale));

                const m = new THREE.Matrix4().fromArray(matrix);
                this.camera.projectionMatrix = m.multiply(modelMatrix);

                // 更新控制器
                if (this.avatarController) {
                    const delta = Math.min(this.clock.getDelta(), 0.1); // 限制最大帧间隔为0.1秒，防止跳变
                    this.avatarController.update(delta);
                    this.map.triggerRepaint();
                }

                this.renderer.resetState();
                this.renderer.render(this.scene, this.camera);
            }
        };

        this.map.addLayer(customLayer);
    }

    /**
     * 初始化UI事件
     */
    initUI() {
        // 麦克风按钮
        this.ui.micBtn.addEventListener('click', () => {
            if (this.isRecording) {
                this.stopRecording();
            } else {
                this.startRecording();
            }
        });

        // 发送按钮
        this.ui.sendBtn.addEventListener('click', () => {
            this.sendMessage();
        });

        // 回车发送
        this.ui.input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });
    }

    /**
     * 开始录音
     */
    async startRecording() {
        if (!this.audioProcessor) return;

        // 首次需要初始化
        if (!this.audioProcessor.audioContext) {
            await this.audioProcessor.init();
        }

        const success = this.audioProcessor.startRecording();
        if (success) {
            this.isRecording = true;
            this.ui.micBtn.classList.add('recording');
            this.ui.micBtn.innerHTML = '⏹️';
            this.updateStatus('正在聆听...', true);
        }
    }

    /**
     * 停止录音
     */
    async stopRecording() {
        if (!this.isRecording || !this.audioProcessor) return;

        this.isRecording = false;
        this.ui.micBtn.classList.remove('recording');
        this.ui.micBtn.innerHTML = '🎤';
        this.updateStatus('正在处理...', true);

        const audioBlob = await this.audioProcessor.stopRecording();
        if (audioBlob) {
            // 发送音频到服务器
            this.wsService.sendAudio(audioBlob);
            this.addMessage('（发送语音...）', 'user');
        }
    }

    /**
     * 发送文本消息
     */
    sendMessage() {
        const text = this.ui.input.value.trim();
        if (!text) return;

        this.wsService.sendText(text);
        this.addMessage(text, 'user');
        this.ui.input.value = '';
    }

    /**
     * 显示打字动画
     */
    showTypingIndicator() {
        // 如果已存在，先移除
        this.removeTypingIndicator();

        const div = document.createElement('div');
        div.className = 'typing-indicator';
        div.id = 'typing-indicator';
        div.innerHTML = '<span></span><span></span><span></span>';
        this.ui.chatHistory.appendChild(div);
        this.ui.chatHistory.scrollTop = this.ui.chatHistory.scrollHeight;
    }

    /**
     * 移除打字动画
     */
    removeTypingIndicator() {
        const indicator = document.getElementById('typing-indicator');
        if (indicator) {
            indicator.remove();
        }
    }

    /**
     * 添加聊天消息
     */
    addMessage(text, type) {
        const div = document.createElement('div');
        div.className = `message ${type}`;
        // 使用 marked 解析 Markdown，并处理换行
        div.innerHTML = marked.parse(text);
        this.ui.chatHistory.appendChild(div);
        this.ui.chatHistory.scrollTop = this.ui.chatHistory.scrollHeight;
    }

    /**
     * 添加系统消息
     */
    addSystemMessage(text) {
        const div = document.createElement('div');
        div.className = 'message system';
        div.textContent = text;
        this.ui.chatHistory.appendChild(div);
        this.ui.chatHistory.scrollTop = this.ui.chatHistory.scrollHeight;
    }

    /**
     * 更新状态栏
     */
    updateStatus(text, isLoading) {
        this.ui.statusText.textContent = text;
        if (isLoading) {
            this.ui.status.classList.add('loading');
        } else {
            this.ui.status.classList.remove('loading');
        }
    }

    /**
     * 更新可视化器
     */
    updateVisualizer(volume) {
        const scale = 1 + volume * 2;
        this.ui.visualizer.style.transform = `scale(${scale})`;
        this.ui.visualizer.style.opacity = volume > 0.01 ? 1 : 0.3;
    }
}

// 启动应用
window.app = new App();
