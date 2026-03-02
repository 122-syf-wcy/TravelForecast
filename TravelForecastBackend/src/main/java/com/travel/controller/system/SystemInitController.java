package com.travel.controller.system;

import com.travel.common.Result;
import com.travel.service.system.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 系统初始化控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/system/init")
@RequiredArgsConstructor
public class SystemInitController {

    private final SystemConfigService systemConfigService;

    /**
     * 初始化系统配置
     * POST /admin/system/init/config
     */
    @PostMapping("/config")
    public Result<Void> initSystemConfig() {
        try {
            // 基本设置
            systemConfigService.setConfig("system_name", "六盘水旅游管理系统", "系统名称");
            systemConfigService.setConfig("system_icp", "黔ICP备XXXXXXXX号", "备案号");
            systemConfigService.setConfig("contact_email", "admin@liupanshui.gov.cn", "联系邮箱");
            systemConfigService.setConfig("system_logo", "", "系统Logo");
            
            // 业务配置
            systemConfigService.setConfig("predict_days", "7", "客流预测周期（天）");
            systemConfigService.setConfig("refresh_interval", "5", "数据刷新间隔（分钟）");
            systemConfigService.setConfig("auto_approve_merchant", "false", "商户审核自动通过");
            
            // 安全设置
            systemConfigService.setConfig("min_password_length", "8", "密码最小长度");
            systemConfigService.setConfig("login_lock_threshold", "5", "登录失败锁定次数");
            systemConfigService.setConfig("session_timeout", "30", "会话超时时间（分钟）");
            systemConfigService.setConfig("enable_captcha", "true", "启用验证码");
            
            log.info("系统配置初始化成功");
            return Result.success(null, "系统配置初始化成功");
        } catch (Exception e) {
            log.error("系统配置初始化失败", e);
            return Result.error("初始化失败: " + e.getMessage());
        }
    }
}

