package com.travel.service.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.travel.dto.order.TicketOrderDTO;
import com.travel.entity.order.TicketOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 门票订单服务接口
 */
public interface TicketOrderService {
    
    /**
     * 创建门票订单
     * @param dto 订单信息
     * @param userId 用户ID
     * @return 创建的订单
     */
    TicketOrder createOrder(TicketOrderDTO dto, Long userId);
    
    /**
     * 获取用户的订单列表
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 订单分页列表
     */
    IPage<TicketOrder> getUserOrders(Long userId, String status, Integer page, Integer size);
    
    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    TicketOrder getOrderDetail(Long orderId);
    
    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean cancelOrder(Long orderId, Long userId);
    
    /**
     * 获取商家的订单列表
     * @param scenicId 景区ID
     * @param status 订单状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param keyword 关键词（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 订单分页列表
     */
    IPage<TicketOrder> getMerchantOrders(Long scenicId, String status, LocalDate startDate, 
                                         LocalDate endDate, String keyword, Integer page, Integer size);
    
    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param status 新状态
     * @return 是否成功
     */
    boolean updateOrderStatus(Long orderId, String status);
    
    /**
     * 批量核销门票
     * @param orderIds 订单ID列表
     * @return 核销结果（成功数量，失败数量）
     */
    Map<String, Integer> verifyTickets(List<Long> orderIds);
    
    /**
     * 获取订单统计数据
     * @param scenicId 景区ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 统计数据
     */
    Map<String, Object> getStatistics(Long scenicId, LocalDate startDate, LocalDate endDate);
}

