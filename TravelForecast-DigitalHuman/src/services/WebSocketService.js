/**
 * WebSocket通信服务 (WebSocketService)
 * 智教黔行 - 3D数字人研学导师
 * 
 * 功能：
 * - 全双工实时通信
 * - 二进制音频流传输
 * - JSON控制信令处理
 * - 断线自动重连
 * - 心跳保活机制
 */

// 消息类型定义
export const MESSAGE_TYPES = {
    // 控制类
    CONFIG: 'config',           // 配置设置
    INTERRUPT: 'interrupt',     // 打断说话
    HEARTBEAT: 'heartbeat',     // 心跳

    // 数据类
    AUDIO_INPUT: 'audio_input', // 音频输入
    AUDIO_OUTPUT: 'audio_output', // 音频输出
    TEXT_INPUT: 'text_input',   // 文本输入
    TEXT_OUTPUT: 'text_output', // 文本输出
    VISEME: 'viseme',           // 口型数据

    // 状态类
    STATUS: 'status',           // 状态更新
    ERROR: 'error',             // 错误信息

    // 业务类
    PREDICTION: 'prediction',   // 预测结果
    SAFETY_ALERT: 'safety_alert' // 安全预警
};

// 连接状态
export const CONNECTION_STATE = {
    DISCONNECTED: 'disconnected',
    CONNECTING: 'connecting',
    CONNECTED: 'connected',
    RECONNECTING: 'reconnecting'
};

// 默认配置
const DEFAULT_CONFIG = {
    url: 'ws://localhost:8000/ws/avatar',
    reconnectInterval: 3000,    // 重连间隔（毫秒）
    maxReconnectAttempts: 5,    // 最大重连次数
    heartbeatInterval: 30000,   // 心跳间隔（毫秒）
    messageTimeout: 10000       // 消息超时（毫秒）
};

export class WebSocketService {
    constructor(options = {}) {
        this.config = { ...DEFAULT_CONFIG, ...options };

        // WebSocket实例
        this.ws = null;
        this.state = CONNECTION_STATE.DISCONNECTED;

        // 重连相关
        this.reconnectAttempts = 0;
        this.reconnectTimer = null;

        // 心跳相关
        this.heartbeatTimer = null;
        this.lastPongTime = null;

        // 消息队列（用于断线时缓存）
        this.messageQueue = [];

        // 请求-响应映射（用于异步请求）
        this.pendingRequests = new Map();
        this.requestIdCounter = 0;

        // 事件回调
        this.eventHandlers = {
            onConnect: null,
            onDisconnect: null,
            onMessage: null,
            onError: null,
            onStateChange: null,
            onAudioOutput: null,
            onTextOutput: null,
            onViseme: null,
            onPrediction: null,
            onSafetyAlert: null
        };

        console.log('🔌 WebSocketService 初始化完成');
    }

    /**
     * 连接到服务器
     * @param {string} token 可选的认证令牌
     */
    connect(token = null) {
        if (this.state === CONNECTION_STATE.CONNECTED ||
            this.state === CONNECTION_STATE.CONNECTING) {
            console.log('⚠️ 已经在连接中或已连接');
            return;
        }

        this._setState(CONNECTION_STATE.CONNECTING);

        let url = this.config.url;
        if (token) {
            url += `?token=${encodeURIComponent(token)}`;
        }

        try {
            this.ws = new WebSocket(url);
            this.ws.binaryType = 'arraybuffer';

            this.ws.onopen = () => this._handleOpen();
            this.ws.onclose = (event) => this._handleClose(event);
            this.ws.onerror = (error) => this._handleError(error);
            this.ws.onmessage = (event) => this._handleMessage(event);

        } catch (error) {
            console.error('❌ WebSocket创建失败:', error);
            this._handleError(error);
        }
    }

    /**
     * 断开连接
     */
    disconnect() {
        this._clearTimers();
        this.reconnectAttempts = 0;

        if (this.ws) {
            this.ws.close(1000, '用户主动断开');
            this.ws = null;
        }

        this._setState(CONNECTION_STATE.DISCONNECTED);
        console.log('📴 已断开连接');
    }

    /**
     * 发送JSON消息
     * @param {string} type 消息类型
     * @param {Object} data 消息数据
     * @param {boolean} expectResponse 是否期待响应
     * @returns {Promise|void}
     */
    send(type, data = {}, expectResponse = false) {
        const message = {
            type,
            data,
            timestamp: Date.now()
        };

        if (expectResponse) {
            const requestId = ++this.requestIdCounter;
            message.requestId = requestId;

            return new Promise((resolve, reject) => {
                const timeout = setTimeout(() => {
                    this.pendingRequests.delete(requestId);
                    reject(new Error('请求超时'));
                }, this.config.messageTimeout);

                this.pendingRequests.set(requestId, { resolve, reject, timeout });
                this._sendRaw(JSON.stringify(message));
            });
        } else {
            this._sendRaw(JSON.stringify(message));
        }
    }

    /**
     * 发送二进制音频数据
     * @param {ArrayBuffer|Blob} audioData 
     */
    sendAudio(audioData) {
        if (audioData instanceof Blob) {
            audioData.arrayBuffer().then(buffer => {
                this._sendRaw(buffer);
            });
        } else {
            this._sendRaw(audioData);
        }
    }

    /**
     * 发送文本输入（聊天消息）
     * 注意：后端使用流式推送回复，不使用请求-响应模式
     * @param {string} text 
     */
    sendText(text) {
        this.send(MESSAGE_TYPES.TEXT_INPUT, { text }, false);
    }

    /**
     * 发送配置
     * @param {Object} config 
     */
    sendConfig(config) {
        this.send(MESSAGE_TYPES.CONFIG, config);
    }

    /**
     * 发送打断指令
     */
    sendInterrupt() {
        this.send(MESSAGE_TYPES.INTERRUPT);
    }

    /**
     * 注册事件处理器
     * @param {string} event 事件名
     * @param {Function} handler 处理函数
     */
    on(event, handler) {
        if (this.eventHandlers.hasOwnProperty(event)) {
            this.eventHandlers[event] = handler;
        } else {
            console.warn(`⚠️ 未知事件: ${event}`);
        }
    }

    /**
     * 获取当前连接状态
     */
    getState() {
        return this.state;
    }

    /**
     * 是否已连接
     */
    isConnected() {
        return this.state === CONNECTION_STATE.CONNECTED;
    }

    // ==================== 私有方法 ====================

    /**
     * 发送原始数据
     */
    _sendRaw(data) {
        if (this.state !== CONNECTION_STATE.CONNECTED) {
            // 缓存消息，等连接恢复后发送
            this.messageQueue.push(data);
            console.log('📦 消息已缓存，等待连接恢复');
            return;
        }

        try {
            this.ws.send(data);
        } catch (error) {
            console.error('❌ 消息发送失败:', error);
            this.messageQueue.push(data);
        }
    }

    /**
     * 处理连接打开
     */
    _handleOpen() {
        console.log('✅ WebSocket 连接成功');
        this._setState(CONNECTION_STATE.CONNECTED);
        this.reconnectAttempts = 0;

        // 发送缓存的消息
        this._flushMessageQueue();

        // 启动心跳
        this._startHeartbeat();

        if (this.eventHandlers.onConnect) {
            this.eventHandlers.onConnect();
        }
    }

    /**
     * 处理连接关闭
     */
    _handleClose(event) {
        console.log(`📴 WebSocket 连接关闭: code=${event.code}, reason=${event.reason}`);
        this._clearTimers();

        if (this.eventHandlers.onDisconnect) {
            this.eventHandlers.onDisconnect(event);
        }

        // 非正常关闭时尝试重连
        if (event.code !== 1000 && this.reconnectAttempts < this.config.maxReconnectAttempts) {
            this._scheduleReconnect();
        } else {
            this._setState(CONNECTION_STATE.DISCONNECTED);
        }
    }

    /**
     * 处理错误
     */
    _handleError(error) {
        console.error('❌ WebSocket 错误:', error);

        if (this.eventHandlers.onError) {
            this.eventHandlers.onError(error);
        }
    }

    /**
     * 处理收到的消息
     */
    _handleMessage(event) {
        // 二进制数据（音频）
        if (event.data instanceof ArrayBuffer) {
            if (this.eventHandlers.onAudioOutput) {
                this.eventHandlers.onAudioOutput(event.data);
            }
            return;
        }

        // JSON消息
        try {
            const message = JSON.parse(event.data);

            // 处理请求响应
            if (message.requestId && this.pendingRequests.has(message.requestId)) {
                const { resolve, timeout } = this.pendingRequests.get(message.requestId);
                clearTimeout(timeout);
                this.pendingRequests.delete(message.requestId);
                resolve(message);
                return;
            }

            // 处理心跳响应
            if (message.type === MESSAGE_TYPES.HEARTBEAT) {
                this.lastPongTime = Date.now();
                return;
            }

            // 分发到对应的处理器
            this._dispatchMessage(message);

        } catch (error) {
            console.error('❌ 消息解析失败:', error);
        }
    }

    /**
     * 分发消息到处理器
     */
    _dispatchMessage(message) {
        // 通用消息处理器
        if (this.eventHandlers.onMessage) {
            this.eventHandlers.onMessage(message);
        }

        // 类型特定处理器
        switch (message.type) {
            case MESSAGE_TYPES.TEXT_OUTPUT:
                if (this.eventHandlers.onTextOutput) {
                    this.eventHandlers.onTextOutput(message.data);
                }
                break;

            case MESSAGE_TYPES.VISEME:
                if (this.eventHandlers.onViseme) {
                    this.eventHandlers.onViseme(message.data);
                }
                break;

            case MESSAGE_TYPES.PREDICTION:
                if (this.eventHandlers.onPrediction) {
                    this.eventHandlers.onPrediction(message.data);
                }
                break;

            case MESSAGE_TYPES.SAFETY_ALERT:
                if (this.eventHandlers.onSafetyAlert) {
                    this.eventHandlers.onSafetyAlert(message.data);
                }
                break;

            case MESSAGE_TYPES.ERROR:
                console.error('🚨 服务器错误:', message.data);
                if (this.eventHandlers.onError) {
                    this.eventHandlers.onError(message.data);
                }
                break;
        }
    }

    /**
     * 设置状态
     */
    _setState(newState) {
        const oldState = this.state;
        this.state = newState;

        if (oldState !== newState && this.eventHandlers.onStateChange) {
            this.eventHandlers.onStateChange(newState, oldState);
        }
    }

    /**
     * 计划重连
     */
    _scheduleReconnect() {
        this._setState(CONNECTION_STATE.RECONNECTING);
        this.reconnectAttempts++;

        console.log(`🔄 将在 ${this.config.reconnectInterval}ms 后尝试重连 (${this.reconnectAttempts}/${this.config.maxReconnectAttempts})`);

        this.reconnectTimer = setTimeout(() => {
            this.connect();
        }, this.config.reconnectInterval);
    }

    /**
     * 启动心跳
     */
    _startHeartbeat() {
        this.heartbeatTimer = setInterval(() => {
            if (this.state === CONNECTION_STATE.CONNECTED) {
                this.send(MESSAGE_TYPES.HEARTBEAT);
            }
        }, this.config.heartbeatInterval);
    }

    /**
     * 清理定时器
     */
    _clearTimers() {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    /**
     * 发送缓存的消息
     */
    _flushMessageQueue() {
        while (this.messageQueue.length > 0) {
            const data = this.messageQueue.shift();
            try {
                this.ws.send(data);
            } catch (error) {
                console.error('❌ 缓存消息发送失败:', error);
                break;
            }
        }
    }

    /**
     * 销毁服务
     */
    destroy() {
        this.disconnect();
        this.messageQueue = [];
        this.pendingRequests.clear();
        console.log('🔌 WebSocketService 已销毁');
    }
}
