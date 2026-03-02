package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 活动实体类
 */
@Data
@TableName("activities")
public class Activity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 活动编码
     */
    private String activityCode;
    
    /**
     * 活动名称
     */
    private String name;
    
    /**
     * 关联景区ID
     */
    private Long scenicId;
    
    /**
     * 商家ID
     */
    private Long merchantId;
    
    /**
     * 活动类型（节庆活动/文化活动/体育赛事/主题活动）
     */
    private String category;
    
    /**
     * 活动简介
     */
    private String description;
    
    /**
     * 活动详情（富文本）
     */
    private String content;
    
    /**
     * 封面图片URL
     */
    private String coverImage;
    
    /**
     * 活动图片集（JSON）
     */
    private String images;
    
    /**
     * 开始时间
     */
    private LocalDate startTime;
    
    /**
     * 结束时间
     */
    private LocalDate endTime;
    
    /**
     * 活动地点
     */
    private String location;
    
    /**
     * 参与人数限制（0表示不限）
     */
    private Integer participantLimit;
    
    /**
     * 当前参与人数
     */
    private Integer currentParticipants;
    
    /**
     * 报名开始时间
     */
    private LocalDate registrationStart;
    
    /**
     * 报名截止时间
     */
    private LocalDate registrationEnd;
    
    /**
     * 活动价格
     */
    private BigDecimal price;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 活动标签（JSON）
     */
    private String tags;
    
    /**
     * 状态（draft草稿/ongoing进行中/completed已完成/cancelled已取消）
     */
    private String status;
    
    /**
     * 是否已推广到新闻资讯
     */
    private Boolean isPromoted;
    
    /**
     * 推广时间
     */
    private LocalDateTime promotedAt;
    
    /**
     * 关联的新闻资讯ID
     */
    private Long newsId;
    
    /**
     * 浏览次数
     */
    private Integer views;
    
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
    
    /**
     * 软删除时间
     */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}

