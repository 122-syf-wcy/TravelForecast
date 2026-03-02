package com.travel.interceptor.monitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求统计拦截器 - 收集真实的QPS、响应时间、错误率数据
 */
@Component
public class RequestStatsInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "requestStartTime";

    // 保留最近 120 秒的请求记录（时间戳 + 耗时）
    private static final int WINDOW_SECONDS = 120;

    private final ConcurrentLinkedDeque<RequestRecord> records = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) return;

        long duration = System.currentTimeMillis() - startTime;
        int status = response.getStatus();
        boolean isError = status >= 400;

        totalRequests.incrementAndGet();
        if (isError) {
            totalErrors.incrementAndGet();
        }

        records.addLast(new RequestRecord(System.currentTimeMillis(), duration, isError));
        purgeOldRecords();
    }

    /**
     * 获取当前每秒请求数（最近5秒平均）
     */
    public int getCurrentQps() {
        long now = System.currentTimeMillis();
        long cutoff = now - 5000;
        long count = records.stream().filter(r -> r.timestamp >= cutoff).count();
        return (int) (count / 5);
    }

    /**
     * 获取峰值QPS（滑动窗口内每秒最大请求数）
     */
    public int getPeakQps() {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_SECONDS * 1000L;
        int maxQps = 0;
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            long secStart = cutoff + i * 1000L;
            long secEnd = secStart + 1000;
            int count = (int) records.stream()
                    .filter(r -> r.timestamp >= secStart && r.timestamp < secEnd)
                    .count();
            maxQps = Math.max(maxQps, count);
        }
        return maxQps;
    }

    /**
     * 获取错误率(%)
     */
    public double getErrorRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (totalErrors.get() * 100.0) / total;
    }

    /**
     * 获取最近60秒每秒QPS历史
     */
    public int[] getQpsHistory60s() {
        long now = System.currentTimeMillis();
        int[] history = new int[61];
        for (int i = 0; i <= 60; i++) {
            long secStart = now - (60 - i) * 1000L;
            long secEnd = secStart + 1000;
            history[i] = (int) records.stream()
                    .filter(r -> r.timestamp >= secStart && r.timestamp < secEnd)
                    .count();
        }
        return history;
    }

    /**
     * 获取响应时间分布
     * 返回 [<100ms, 100-200ms, 200-500ms, 500ms-1s, 1-3s, >3s]
     */
    public int[] getResponseTimeDistribution() {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_SECONDS * 1000L;

        int[] dist = new int[6];
        records.stream().filter(r -> r.timestamp >= cutoff).forEach(r -> {
            if (r.durationMs < 100) dist[0]++;
            else if (r.durationMs < 200) dist[1]++;
            else if (r.durationMs < 500) dist[2]++;
            else if (r.durationMs < 1000) dist[3]++;
            else if (r.durationMs < 3000) dist[4]++;
            else dist[5]++;
        });
        return dist;
    }

    /**
     * 获取平均响应时间(ms)
     */
    public double getAvgResponseTime() {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_SECONDS * 1000L;
        return records.stream()
                .filter(r -> r.timestamp >= cutoff)
                .mapToLong(r -> r.durationMs)
                .average()
                .orElse(0.0);
    }

    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * 获取总错误数
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }

    private void purgeOldRecords() {
        long cutoff = System.currentTimeMillis() - WINDOW_SECONDS * 1000L;
        while (!records.isEmpty() && records.peekFirst().timestamp < cutoff) {
            records.pollFirst();
        }
    }

    private static class RequestRecord {
        final long timestamp;
        final long durationMs;
        final boolean isError;

        RequestRecord(long timestamp, long durationMs, boolean isError) {
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.isError = isError;
        }
    }
}
