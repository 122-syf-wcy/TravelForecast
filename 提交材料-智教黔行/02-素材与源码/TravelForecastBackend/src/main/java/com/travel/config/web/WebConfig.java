package com.travel.config.web;

import com.travel.interceptor.monitor.RequestStatsInterceptor;
import com.travel.interceptor.security.JwtInterceptor;
import com.travel.interceptor.security.RoleInterceptor;
import com.travel.interceptor.security.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 注册拦截器、配置跨域等
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private RequestStatsInterceptor requestStatsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有需要认证的接口
                .addPathPatterns(
                        "/v1/**",                // v1版本接口
                        "/reviews",              // 评价提交（POST）
                        "/reviews/*/like",       // 点赞
                        "/reviews/*/unlike",     // 取消点赞
                        "/reviews/*/reply",      // 回复评价
                        "/users/**",             // 用户管理
                        "/upload/**",            // 文件上传
                        "/oss/upload/**",        // OSS文件上传（需要认证）
                        "/dashboard/user-stats", // 用户统计
                        "/favorites/**",         // 收藏管理（添加、取消、检查）
                        "/tickets/**",           // 门票订单管理
                        "/merchant/contracts/**", // 商家合同管理
                        "/merchant/news/**",     // 商家新闻管理
                        "/merchant/reviews/**",  // 商家评论管理
                        "/merchant/activities/**", // 商家活动管理
                        "/merchant/facilities/**", // 商家设施管理
                        "/merchant/announcements/**", // 商家公告管理
                        "/merchant/todos/**",      // 商家待办事项管理
                        "/merchant/notifications/**", // 商家系统通知
                        "/user/notifications/**",  // 用户端系统通知（允许游客，但会尝试获取userId）
                        "/admin/merchant/contracts/**", // 管理员合同管理
                        "/admin/notifications/**", // 管理员系统通知管理
                        "/admin/account/**",     // 管理员账户管理
                        "/admin/system/**",      // 系统设置管理
                        "/admin/roles/**",       // 角色权限管理
                        "/admin/backup/**",      // 数据库备份管理
                        "/admin/system/init/**"  // 系统初始化
                )
                // 排除登录注册等接口（白名单）
                .excludePathPatterns(
                        "/captcha",
                        "/auth/login",
                        "/auth/login-config",
                        "/auth/register/**",
                        "/auth/status"
                );

        // 基于角色的路由访问控制
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/admin/**", "/merchant/**", "/business/**");

        // 全局限流（简单内存实现）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**");

        // 全局请求统计（QPS、响应时间等真实数据采集）
        registry.addInterceptor(requestStatsInterceptor)
                .addPathPatterns("/**");
    }
}

