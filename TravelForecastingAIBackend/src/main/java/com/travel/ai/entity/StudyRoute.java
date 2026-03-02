package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研学教育路线表
 */
@Data
@TableName("ai_study_routes")
public class StudyRoute implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 路线名称 */
    private String name;

    /** 路线描述 */
    private String description;

    /** 适合年龄段: primary/middle/high/college/all */
    private String ageGroup;

    /** 主题: geography/history/ecology/culture */
    private String theme;

    /** 路线天数 */
    private Integer days;

    /** 路线详情(JSON) */
    private String routeDetail;

    /** 学习目标(JSON) */
    private String learningObjectives;

    /** 封面图片URL */
    private String coverImage;

    /** 排序 */
    private Integer sortOrder;

    /** 是否启用 */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
