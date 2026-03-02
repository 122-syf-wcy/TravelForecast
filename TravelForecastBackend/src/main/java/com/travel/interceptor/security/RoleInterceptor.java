package com.travel.interceptor.security;

import com.travel.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        // 仅对需要鉴权的路由进行角色判断（JWT拦截器先行保证已登录）
        if (uri.startsWith("/admin/")) {
            String role = (String) request.getAttribute("role");
            if (!"admin".equals(role)) {
                throw new BusinessException(403, "仅管理员可访问");
            }
        }

        if (uri.startsWith("/merchant/") || uri.startsWith("/business/")) {
            String role = (String) request.getAttribute("role");
            if (!"business".equals(role) && !"merchant".equals(role) && !"admin".equals(role)) {
                throw new BusinessException(403, "仅商家或管理员可访问");
            }
        }

        return true;
    }
}


