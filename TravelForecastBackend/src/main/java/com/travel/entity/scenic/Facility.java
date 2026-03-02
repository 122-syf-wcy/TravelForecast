package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设施实体类
 */
@Data
@TableName("facilities")
public class Facility {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 设施编码
     */
    private String facilityCode;
    
    /**
     * 设施名称
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
     * 设施类型（停车场/餐厅/卫生间/服务中心/医疗点/其他）
     */
    private String category;
    
    /**
     * 设施描述
     */
    private String description;
    
    /**
     * 设施位置
     */
    private String location;
    
    /**
     * 纬度
     */
    private BigDecimal latitude;
    
    /**
     * 经度
     */
    private BigDecimal longitude;
    
    /**
     * 容量/规模
     */
    private Integer capacity;
    
    /**
     * 开放时间
     */
    private String openingHours;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 设施缩略图
     */
    private String thumbnail;
    
    /**
     * 设施图片（JSON）
     */
    private String images;
    
    /**
     * 设施特色/服务项目（JSON）
     */
    private String features;
    
    /**
     * 收费信息
     */
    private String priceInfo;
    
    /**
     * 状态（normal正常/maintenance维护中/closed关闭）
     */
    private String status;
    
    /**
     * 是否免费
     */
    private Boolean isFree;
    
    /**
     * 评分
     */
    private BigDecimal rating;
    
    /**
     * 评价数量
     */
    private Integer reviewCount;
    
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

