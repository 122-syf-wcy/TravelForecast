package com.travel.gateway.exception;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关全局异常处理器
 * 统一处理网关中的异常
 * 
 * @author Travel Gateway Team
 */
@Slf4j
@Component
@Order(-1) // 优先级高于默认异常处理器
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        if (ex instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) ex;
            HttpStatus status = (HttpStatus) statusException.getStatusCode();
            response.setStatusCode(status);
            result.put("code", status.value());
            result.put("message", statusException.getReason() != null 
                    ? statusException.getReason() 
                    : status.getReasonPhrase());
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            result.put("code", 500);
            result.put("message", "网关内部错误: " + ex.getMessage());
        }

        result.put("data", null);
        result.put("path", exchange.getRequest().getURI().getPath());

        log.error("网关异常处理: {} | 路径: {}", ex.getMessage(), 
                exchange.getRequest().getURI().getPath(), ex);

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}

