package com.travel.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_configs")
public class SiteConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 唯一配置键，如 landing */
    private String configKey;
    /** JSON 字符串 */
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    public static final String KEY_LANDING = "landing";
}


