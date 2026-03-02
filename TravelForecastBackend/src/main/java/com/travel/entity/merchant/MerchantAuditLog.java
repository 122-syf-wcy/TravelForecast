package com.travel.entity.merchant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("merchant_audit_logs")
public class MerchantAuditLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId; // 商家用户ID

    private Long adminUserId; // 管理员用户ID

    private String action; // approve / reject

    private String remark; // 备注

    private String fromStatus;

    private String toStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}


