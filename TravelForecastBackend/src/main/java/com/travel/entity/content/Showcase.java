package com.travel.entity.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实景预览实体（支持图片和视频）
 */
@Data
@TableName("showcases")
public class Showcase {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 标题 */
    private String title;
    
    /** 类型：image 或 video */
    private String type;
    
    /** 媒体URL（图片或视频地址） */
    private String url;
    
    /** 封面图URL（视频类型时使用） */
    private String cover;
    
    /** 描述 */
    private String description;
    
    /** 排序 */
    private Integer sort;
    
    /** 是否启用 */
    private Boolean enabled;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
    
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
