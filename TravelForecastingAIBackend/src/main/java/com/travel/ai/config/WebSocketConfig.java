package com.travel.ai.config;

import com.travel.ai.websocket.DigitalHumanWebSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置
 * 用于数字人WebSocket代理
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DigitalHumanWebSocket digitalHumanWebSocket;

    public WebSocketConfig(DigitalHumanWebSocket digitalHumanWebSocket) {
        this.digitalHumanWebSocket = digitalHumanWebSocket;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(digitalHumanWebSocket, "/ws/digital-human")
                .setAllowedOrigins("*");
    }

    /**
     * WebSocket容器配置
     * 增大缓冲区以支持音频二进制传输（TTS音频通常30-500KB）
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(1024 * 1024);  // 1MB 二进制缓冲
        container.setMaxTextMessageBufferSize(256 * 1024);      // 256KB 文本缓冲
        container.setMaxSessionIdleTimeout(300000L);             // 5分钟空闲超时
        container.setAsyncSendTimeout(30000L);                   // 30秒异步发送超时
        return container;
    }
}
