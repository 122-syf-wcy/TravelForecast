package com.travel.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("system_logs")
public class SystemLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("log_time")
    private LocalDateTime logTime;

    @TableField("log_level")
    private String logLevel;

    @TableField("module")
    private String module;

    @TableField("message")
    private String message;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("ip_address")
    private String ipAddress;
}

