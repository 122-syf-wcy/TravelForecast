package com.travel.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户会话表
 */
@Data
@TableName("user_sessions")
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * JWT Token
     */
    private String token;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 登录时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginAt;

    /**
     * 过期时间
     */
    @TableField("expire_at")
    private LocalDateTime expiresAt;

    /**
     * 刷新令牌过期时间
     */
    @TableField("refresh_expire_at")
    private LocalDateTime refreshExpireAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

