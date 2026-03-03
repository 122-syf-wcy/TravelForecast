package com.travel.gateway.filter;

import com.travel.gateway.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 网关请求/响应日志过滤器
 * 记录所有API调用的详细信息
 * 
 * @author Travel Gateway Team
 */
@Slf4j
@Component
public class GatewayLogFilter implements GlobalFilter, Ordered {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayProperties.getLog().isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String remoteAddress = getRemoteAddress(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String userId = request.getHeaders().getFirst("X-User-Id");

        long startTime = System.currentTimeMillis();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 记录请求信息
        if (gatewayProperties.getLog().isLogRequest()) {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("[请求] {} | {} | {}", timestamp, method, path);
            log.info("客户端IP: {} | 用户ID: {}", remoteAddress, userId != null ? userId : "未登录");
            log.info("User-Agent: {}", userAgent);
            log.info("═══════════════════════════════════════════════════════════");
        }

        // 包装响应以记录响应信息
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (gatewayProperties.getLog().isLogResponse()) {
                    return DataBufferUtils.join(body)
                            .doOnNext(dataBuffer -> {
                                try {
                                    byte[] content = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(content);
                                    
                                    long duration = System.currentTimeMillis() - startTime;
                                    int statusCode = getStatusCode().value();
                                    String responseBody = new String(content, StandardCharsets.UTF_8);

                                    log.info("═══════════════════════════════════════════════════════════");
                                    log.info(" [响应] {} | {} | {} | 耗时: {}ms", 
                                            timestamp, method, path, duration);
                                    log.info(" 状态码: {} | 响应长度: {} bytes", statusCode, content.length);
                                    log.info(" 响应体: {}", responseBody.length() > 500 
                                            ? responseBody.substring(0, 500) + "..." 
                                            : responseBody);
                                    log.info("═══════════════════════════════════════════════════════════");
                                } catch (Exception e) {
                                    log.warn("记录响应日志失败: {}", e.getMessage());
                                }
                            })
                            .then(super.writeWith(body));
                } else {
                    // 只记录响应时间和状态码
                    return super.writeWith(body).doOnSuccess(v -> {
                        long duration = System.currentTimeMillis() - startTime;
                        int statusCode = getStatusCode().value();
                        log.debug("📤 [响应] {} | {} | {} | 耗时: {}ms | 状态码: {}", 
                                timestamp, method, path, duration, statusCode);
                    });
                }
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 获取客户端真实IP
     */
    private String getRemoteAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // 优先级最低，最后执行
    }
}

