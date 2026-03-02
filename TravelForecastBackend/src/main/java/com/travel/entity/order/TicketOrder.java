package com.travel.entity.order;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门票订单实体类
 */
@Data
@TableName("ticket_orders")
public class TicketOrder {
    
    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单编号（唯一）
     */
    private String orderNo;
    
    /**
     * 景区ID
     */
    private Long scenicId;
    
    /**
     * 景区名称（冗余字段，方便查询）
     */
    private String scenicName;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名称（冗余）
     */
    private String userName;
    
    /**
     * 用户手机号（冗余）
     */
    private String userPhone;
    
    /**
     * 票种类型: adult-成人票, child-儿童票, student-学生票, elder-老人票
     */
    private String ticketType;
    
    /**
     * 购票数量
     */
    private Integer ticketCount;
    
    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;
    
    /**
     * 总金额（元）
     */
    private BigDecimal totalAmount;
    
    /**
     * 参观日期
     */
    private LocalDate visitDate;
    
    /**
     * 游客姓名
     */
    private String visitorName;
    
    /**
     * 游客联系电话
     */
    private String visitorPhone;
    
    /**
     * 订单状态: pending-待支付, paid-已支付, used-已使用, cancelled-已取消, refunded-已退款
     */
    private String status;
    
    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;
    
    /**
     * 使用时间（核销时间）
     */
    private LocalDateTime usedTime;
    
    /**
     * 取消时间
     */
    private LocalDateTime cancelledTime;
    
    /**
     * 退款时间
     */
    private LocalDateTime refundedTime;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 条形码（用于景区扫码检票）
     */
    private String barcode;
    
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
     * 是否删除 0-未删除 1-已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}

