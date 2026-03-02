package com.travel.entity.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 紧急救援表
 */
@Data
@TableName("emergency_rescue")
public class EmergencyRescue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 救援ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 求救用户ID
     */
    private Long userId;

    /**
     * 景区ID
     */
    private Long scenicId;

    /**
     * 救援类型：MEDICAL(医疗),LOST(走失),ACCIDENT(事故),OTHER(其他)
     */
    private String rescueType;

    /**
     * 救援状态：PENDING(待处理),PROCESSING(处理中),COMPLETED(已完成),CANCELLED(已取消)
     */
    private String status;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 具体位置描述
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
     * 详细描述
     */
    private String description;

    /**
     * 紧急程度：URGENT(紧急),NORMAL(一般)
     */
    private String emergencyLevel;

    /**
     * 处理人ID
     */
    private Long handlerId;

    /**
     * 处理人姓名
     */
    private String handlerName;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 处理备注
     */
    private String handleNotes;

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
     * 软删除时间 (NULL=未删除, 时间戳=已删除)
     */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}

