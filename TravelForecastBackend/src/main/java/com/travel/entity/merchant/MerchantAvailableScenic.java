package com.travel.entity.merchant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家可申请景区实体类
 */
@Data
@TableName("merchant_available_scenics")
public class MerchantAvailableScenic {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 景区ID
     */
    @TableField("scenic_id")
    private Long scenicId;
    
    /**
     * 是否可申请
     */
    @TableField("is_available")
    private Boolean isAvailable;
    
    /**
     * 最大商家数量
     */
    @TableField("max_merchants")
    private Integer maxMerchants;
    
    /**
     * 当前商家数量
     */
    @TableField("current_merchants")
    private Integer currentMerchants;
    
    /**
     * 排序优先级
     */
    @TableField("priority")
    private Integer priority;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

