package com.travel.interceptor.security;

import com.travel.exception.BusinessException;
import com.travel.utils.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 拦截所有需要认证的请求，验证JWT Token
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.token-prefix:Bearer}")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 处理OPTIONS请求（预检请求）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 从请求头获取Token
        String token = getTokenFromRequest(request);
        
        // 如果没有Token
        if (token == null || token.isEmpty()) {
            // GET请求允许放行（游客可以查看）
            if ("GET".equals(request.getMethod())) {
                log.debug("GET请求未携带Token，允许放行: {}", request.getRequestURI());
                return true;
            }
            // POST、PUT、DELETE等修改类请求必须携带Token
            log.warn("请求未携带Token: {} {}", request.getMethod(), request.getRequestURI());
            throw new BusinessException(401, "未登录，请先登录");
        }

        // 有Token，进行验证
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Token无效: {}", token);
                throw new BusinessException(401, "Token无效或已过期");
            }

            // Token有效，从Token中获取用户信息并设置到请求属性中
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            
            // 将用户信息存储到request中，供后续使用
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            
            log.info("Token验证成功，用户：{}, userId={}, 请求：{} {}", username, userId, request.getMethod(), request.getRequestURI());
            return true;
            
        } catch (Exception e) {
            log.error("Token验证失败：{}", e.getMessage());
            throw new BusinessException(401, "Token验证失败：" + e.getMessage());
        }
    }

    /**
     * 从请求头中获取Token
     * 
     * @param request HTTP请求
     * @return Token字符串（去除Bearer前缀）
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(header);
        
        if (bearerToken != null && bearerToken.startsWith(tokenPrefix + " ")) {
            return bearerToken.substring(tokenPrefix.length() + 1);
        }
        
        return null;
    }
}

