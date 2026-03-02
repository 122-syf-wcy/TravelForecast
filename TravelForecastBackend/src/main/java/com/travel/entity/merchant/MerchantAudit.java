package com.travel.entity.merchant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商户审核表
 */
@Data
@TableName("merchant_audit")
public class MerchantAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商户名称
     */
    private String businessName;

    /**
     * 商户类型
     */
    private String businessType;

    /**
     * 营业执照URL
     */
    private String businessLicense;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 审核状态：PENDING/APPROVED/REJECTED
     */
    private String status;

    /**
     * 审核备注
     */
    private String auditNote;

    /**
     * 审核人ID
     */
    private Long auditedBy;

    /**
     * 审核时间
     */
    private LocalDateTime auditedAt;

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

