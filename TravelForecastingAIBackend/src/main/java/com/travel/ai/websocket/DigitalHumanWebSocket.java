package com.travel.ai.websocket;

import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 数字人WebSocket代理
 * 
 * 将前端的WebSocket连接代理转发到旧数字人后端
 * 前端 ←→ AI服务(8081) ←→ 数字人后端(8000)
 * 
 * 支持文本消息(JSON)和二进制消息(音频TTS)的双向转发
 */
@Component
public class DigitalHumanWebSocket extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DigitalHumanWebSocket.class);

    @Value("${digital-human.ws-url:ws://localhost:8000/ws/avatar}")
    private String digitalHumanWsUrl;

    @Value("${digital-human.enabled:true}")
    private boolean enabled;

    // 管理前端Session到后端WebSocket的映射
    private final Map<String, WebSocket> backendConnections = new ConcurrentHashMap<>();
    private final OkHttpClient okHttpClient;

    public DigitalHumanWebSocket() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (!enabled) {
            log.warn("数字人WebSocket服务未启用");
            try {
                session.close(CloseStatus.SERVICE_RESTARTED);
            } catch (IOException e) {
                log.error("关闭session失败", e);
            }
            return;
        }

        String sessionId = session.getId();
        log.info("前端WebSocket连接建立: {}", sessionId);

        // 连接到后端数字人WebSocket
        Request request = new Request.Builder()
                .url(digitalHumanWsUrl)
                .build();

        okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("连接到数字人后端WebSocket: sessionId={}", sessionId);
                backendConnections.put(sessionId, webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // 将后端文本消息转发给前端（JSON: status/text_output等）
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(text));
                        log.debug("转发文本到前端: sessionId={}, length={}", sessionId, text.length());
                    }
                } catch (IOException e) {
                    log.error("转发文本到前端失败: {}", e.getMessage());
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                // 将后端二进制消息转发给前端（音频TTS数据）
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new BinaryMessage(bytes.toByteArray()));
                        log.debug("转发音频到前端: sessionId={}, size={} bytes", sessionId, bytes.size());
                    }
                } catch (IOException e) {
                    log.error("转发音频到前端失败: {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("数字人后端WebSocket连接失败: {}", t.getMessage());
                backendConnections.remove(sessionId);
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("{\"error\":\"数字人服务连接失败\"}"));
                    }
                } catch (IOException e) {
                    log.error("发送错误消息失败", e);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("数字人后端WebSocket关闭: code={}, reason={}", code, reason);
                backendConnections.remove(sessionId);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        WebSocket backendWs = backendConnections.get(sessionId);

        if (backendWs != null) {
            backendWs.send(message.getPayload());
            log.debug("转发文本到数字人后端: sessionId={}, length={}", sessionId, message.getPayloadLength());
        } else {
            log.warn("数字人后端连接不存在: sessionId={}", sessionId);
            try {
                session.sendMessage(new TextMessage("{\"error\":\"数字人服务未连接\"}"));
            } catch (IOException e) {
                log.error("发送错误消息失败", e);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 前端发来的二进制消息（如录音数据）转发到后端
        String sessionId = session.getId();
        WebSocket backendWs = backendConnections.get(sessionId);

        if (backendWs != null) {
            // 正确读取ByteBuffer：先rewind确保从头读取
            java.nio.ByteBuffer payload = message.getPayload();
            byte[] data = new byte[payload.remaining()];
            if (data.length == 0) {
                // remaining为0时尝试rewind重读
                payload.rewind();
                data = new byte[payload.remaining()];
            }
            if (data.length > 0) {
                payload.get(data);
                backendWs.send(ByteString.of(data, 0, data.length));
                log.debug("转发二进制到数字人后端: sessionId={}, size={} bytes", sessionId, data.length);
            } else {
                log.debug("收到空二进制消息，跳过转发: sessionId={}", sessionId);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        log.info("前端WebSocket连接关闭: {}, status={}", sessionId, status);

        WebSocket backendWs = backendConnections.remove(sessionId);
        if (backendWs != null) {
            backendWs.close(1000, "Frontend disconnected");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
    }
}
