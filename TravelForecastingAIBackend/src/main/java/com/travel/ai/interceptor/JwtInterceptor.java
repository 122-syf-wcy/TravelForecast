package com.travel.ai.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT Token 拦截器
 * 验证来自前端或业务后端的请求Token
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.token-prefix:Bearer}")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(tokenHeader);
        if (token == null || token.isEmpty()) {
            // 无Token时允许访问（部分接口不需要认证）
            // 具体权限控制由各Controller自行判断
            return true;
        }

        try {
            if (token.startsWith(tokenPrefix + " ")) {
                token = token.substring(tokenPrefix.length() + 1);
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 将用户信息放入request属性
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            if (userId != null) {
                request.setAttribute("userId", userId);
            }
            if (username != null) {
                request.setAttribute("username", username);
            }

            log.debug("JWT验证通过: userId={}, username={}", userId, username);
            return true;

        } catch (Exception e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            // Token无效时仍允许访问，但不设置用户信息
            return true;
        }
    }
}
