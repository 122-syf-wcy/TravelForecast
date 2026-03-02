package com.travel.interceptor;

import com.travel.common.OperationLog;
import com.travel.entity.system.SystemLog;
import com.travel.mapper.system.SystemLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志AOP切面
 * 自动记录带有 @OperationLog 注解的方法调用
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SystemLogMapper systemLogMapper;

    /**
     * 方法成功执行后记录日志
     */
    @AfterReturning("@annotation(operationLog)")
    public void afterReturning(JoinPoint joinPoint, OperationLog operationLog) {
        try {
            saveLog(operationLog, null);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 方法抛出异常时记录日志
     */
    @AfterThrowing(value = "@annotation(operationLog)", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, OperationLog operationLog, Throwable ex) {
        try {
            saveLog(operationLog, ex);
        } catch (Exception e) {
            log.error("记录异常操作日志失败", e);
        }
    }

    private void saveLog(OperationLog operationLog, Throwable ex) {
        SystemLog sysLog = new SystemLog();
        sysLog.setLogTime(LocalDateTime.now());
        sysLog.setModule(operationLog.module());

        if (ex != null) {
            sysLog.setLogLevel("error");
            sysLog.setMessage(operationLog.description() + " [失败: " + ex.getMessage() + "]");
        } else {
            sysLog.setLogLevel(operationLog.level());
            sysLog.setMessage(operationLog.description());
        }

        // 获取当前请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            sysLog.setIpAddress(getClientIp(request));

            // 从请求属性获取用户信息（由JwtInterceptor设置）
            Object userId = request.getAttribute("userId");
            Object username = request.getAttribute("username");
            if (userId != null) {
                sysLog.setUserId(Long.valueOf(userId.toString()));
            }
            if (username != null) {
                sysLog.setUsername(username.toString());
            }
        }

        systemLogMapper.insert(sysLog);
    }

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
