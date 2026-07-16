package com.travel.ai.interceptor;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT Token 拦截器。
 * 行为规范：
 * 1. OPTIONS 预检请求直接放行；
 * 2. 未携带 Token 的请求：放行（默认由上游网关白名单/下游 Controller 控制）；
 * 3. 携带 Token 但解析失败：返回 401，而不是默默放行；
 * 4. Token 合法：解析用户信息注入 request 属性。
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(tokenHeader);
        if (token == null || token.isEmpty()) {
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
            log.warn("JWT验证失败，拒绝请求: uri={}, error={}", request.getRequestURI(), e.getMessage());
            writeUnauthorized(response, "Token 无效或已过期");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(JSON.toJSONString(body));
    }
}
