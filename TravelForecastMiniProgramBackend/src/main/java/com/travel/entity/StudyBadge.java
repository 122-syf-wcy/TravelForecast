package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("mp_study_badge")
public class StudyBadge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String iconChar;
    private String color;
    private String description;
    private String conditionType;
    private Integer conditionValue;
    private Integer sortOrder;
}
