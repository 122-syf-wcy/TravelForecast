package com.travel.entity.content;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感词实体
 */
@Data
@TableName("sensitive_words")
public class SensitiveWord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词
     */
    private String word;

    /**
     * 分类：general/political/vulgar/illegal/ads
     */
    private String category;

    /**
     * 等级：1-一般，2-中等，3-严重
     */
    private Integer level;

    /**
     * 处理方式：replace替换/block拦截
     */
    private String action;

    /**
     * 替换文本
     */
    private String replacement;

    /**
     * 状态：active/inactive
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

