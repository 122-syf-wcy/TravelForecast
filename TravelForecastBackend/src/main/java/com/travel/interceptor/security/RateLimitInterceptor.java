package com.travel.interceptor.security;

import com.travel.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单基于内存的限流：按IP+路径，每分钟最多N次
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_REQUESTS = 60; // 每分钟最多60次（可按需调整，登录接口可单独更低）

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        String key = ip + ":" + path;

        int limit = path.startsWith("/auth/login") ? 10 : MAX_REQUESTS; // 登录更严格

        long now = Instant.now().getEpochSecond();
        Window w = buckets.computeIfAbsent(key, k -> new Window(now, 0));
        synchronized (w) {
            if (now - w.start >= WINDOW_SECONDS) {
                w.start = now;
                w.count = 0;
            }
            w.count++;
            if (w.count > limit) {
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
        }
        return true;
    }

    private static class Window {
        long start;
        int count;
        Window(long start, int count) { this.start = start; this.count = count; }
    }
}


