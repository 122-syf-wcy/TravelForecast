package com.travel.entity.auth;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验证码记录表
 */
@Data
@TableName("captcha_records")
public class CaptchaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 验证码键（唯一标识）
     */
    private String captchaKey;

    /**
     * 验证码内容
     */
    private String captchaCode;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 是否已使用
     */
    private Boolean used;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

