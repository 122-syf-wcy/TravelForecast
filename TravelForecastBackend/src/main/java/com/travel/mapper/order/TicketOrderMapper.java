package com.travel.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.order.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Map;

/**
 * 门票订单Mapper接口
 */
@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {
    
    /**
     * 获取订单统计数据
     * @param scenicId 景区ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 统计数据
     */
    @Select("<script>" +
            "SELECT " +
            "COUNT(*) as totalOrders, " +
            "COALESCE(SUM(total_amount), 0) as totalAmount, " +
            "COALESCE(SUM(ticket_count), 0) as totalTickets, " +
            "COUNT(CASE WHEN status = 'pending' THEN 1 END) as pendingOrders, " +
            "COUNT(CASE WHEN status = 'paid' THEN 1 END) as paidOrders, " +
            "COUNT(CASE WHEN status = 'used' THEN 1 END) as usedOrders, " +
            "COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelledOrders, " +
            "COUNT(CASE WHEN DATE(created_at) = CURDATE() THEN 1 END) as todayOrders, " +
            "COALESCE(SUM(CASE WHEN DATE(created_at) = CURDATE() THEN total_amount ELSE 0 END), 0) as todayAmount " +
            "FROM ticket_orders " +
            "WHERE deleted = 0 " +
            "<if test='scenicId != null'> AND scenic_id = #{scenicId} </if>" +
            "<if test='startDate != null'> AND DATE(created_at) &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND DATE(created_at) &lt;= #{endDate} </if>" +
            "</script>")
    Map<String, Object> getStatistics(@Param("scenicId") Long scenicId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);
}

