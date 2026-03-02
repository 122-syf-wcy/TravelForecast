package com.travel.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为日志表
 */
@Data
@TableName("user_behavior_logs")
public class UserBehaviorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 行为类型：page_view/click/search/bookmark/share等
     */
    private String behaviorType;

    /**
     * 模块名称
     */
    private String module;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 停留时长（秒）
     */
    private Integer duration;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

