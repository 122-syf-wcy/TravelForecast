package com.travel.dto.order;

import lombok.Data;
import java.time.LocalDate;

/**
 * 创建门票订单DTO
 */
@Data
public class TicketOrderDTO {
    
    /**
     * 景区ID
     */
    private Long scenicId;
    
    /**
     * 票种类型
     */
    private String ticketType;
    
    /**
     * 购票数量
     */
    private Integer ticketCount;
    
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
}

