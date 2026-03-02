package com.travel.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.order.TicketOrderDTO;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.order.TicketOrder;
import com.travel.entity.user.User;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.order.TicketOrderMapper;
import com.travel.mapper.user.UserMapper;
import com.travel.service.order.TicketOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 门票订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketOrderServiceImpl implements TicketOrderService {
    
    private final TicketOrderMapper ticketOrderMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    private final UserMapper userMapper;
    
    /**
     * 生成订单编号
     * @return 订单编号
     */
    private String generateOrderNo() {
        // 格式：TK + 时间戳 + 6位随机数
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Random random = new Random();
        int randomNum = random.nextInt(900000) + 100000;
        return "TK" + timestamp + randomNum;
    }
    
    /**
     * 根据票种计算票价
     * @param basePrice 基础价格
     * @param ticketType 票种类型
     * @return 实际价格
     */
    private BigDecimal calculatePrice(BigDecimal basePrice, String ticketType) {
        switch (ticketType) {
            case "child":
                // 儿童票半价
                return basePrice.multiply(new BigDecimal("0.5"));
            case "student":
                // 学生票7折
                return basePrice.multiply(new BigDecimal("0.7"));
            case "elder":
                // 老人票半价
                return basePrice.multiply(new BigDecimal("0.5"));
            case "adult":
            default:
                return basePrice;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder createOrder(TicketOrderDTO dto, Long userId) {
        // 1. 验证景区是否存在
        ScenicSpot scenic = scenicSpotMapper.selectById(dto.getScenicId());
        if (scenic == null) {
            throw new RuntimeException("景区不存在");
        }
        
        // 2. 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 计算价格
        BigDecimal basePrice;
        try {
            // price字段是String类型，需要转换
            String priceStr = scenic.getPrice();
            if (priceStr != null && !priceStr.isEmpty()) {
                // 提取数字部分（去除"¥"等符号）
                priceStr = priceStr.replaceAll("[^0-9.]", "");
                basePrice = new BigDecimal(priceStr);
            } else {
                basePrice = new BigDecimal("50.00"); // 默认价格
            }
        } catch (Exception e) {
            basePrice = new BigDecimal("50.00"); // 转换失败使用默认价格
        }
        BigDecimal unitPrice = calculatePrice(basePrice, dto.getTicketType());
        BigDecimal totalAmount = unitPrice.multiply(new BigDecimal(dto.getTicketCount()));
        
        // 4. 创建订单
        TicketOrder order = new TicketOrder();
        order.setOrderNo(generateOrderNo());
        order.setScenicId(dto.getScenicId());
        order.setScenicName(scenic.getName());
        order.setUserId(userId);
        order.setUserName(user.getUsername());
        order.setUserPhone(user.getPhone());
        order.setTicketType(dto.getTicketType());
        order.setTicketCount(dto.getTicketCount());
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(totalAmount);
        order.setVisitDate(dto.getVisitDate());
        order.setVisitorName(dto.getVisitorName());
        order.setVisitorPhone(dto.getVisitorPhone());
        order.setStatus("pending"); // 待支付
        
        // 5. 生成条形码（用于景区扫码检票）
        order.setBarcode(generateBarcode());
        
        ticketOrderMapper.insert(order);
        
        return order;
    }
    
    /**
     * 生成条形码（13位EAN-13格式）
     * 格式：前缀(2位) + 景区编码(3位) + 时间戳后6位 + 随机数(2位)
     */
    private String generateBarcode() {
        StringBuilder barcode = new StringBuilder();
        // 前缀：69表示中国
        barcode.append("69");
        // 时间戳后8位
        String timestamp = String.valueOf(System.currentTimeMillis());
        barcode.append(timestamp.substring(timestamp.length() - 8));
        // 随机数3位
        barcode.append(String.format("%03d", (int)(Math.random() * 1000)));
        return barcode.toString();
    }
    
    @Override
    public IPage<TicketOrder> getUserOrders(Long userId, String status, Integer page, Integer size) {
        Page<TicketOrder> pageParam = new Page<>(page, size);
        
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketOrder::getUserId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TicketOrder::getStatus, status);
        }
        wrapper.orderByDesc(TicketOrder::getCreatedAt);
        
        return ticketOrderMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public TicketOrder getOrderDetail(Long orderId) {
        return ticketOrderMapper.selectById(orderId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("只能取消待支付订单");
        }
        
        order.setStatus("cancelled");
        order.setCancelledTime(LocalDateTime.now());
        
        return ticketOrderMapper.updateById(order) > 0;
    }
    
    @Override
    public IPage<TicketOrder> getMerchantOrders(Long scenicId, String status, LocalDate startDate, 
                                                LocalDate endDate, String keyword, Integer page, Integer size) {
        Page<TicketOrder> pageParam = new Page<>(page, size);
        
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<>();
        if (scenicId != null) {
            wrapper.eq(TicketOrder::getScenicId, scenicId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TicketOrder::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(TicketOrder::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(TicketOrder::getCreatedAt, endDate.atTime(23, 59, 59));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TicketOrder::getOrderNo, keyword)
                    .or().like(TicketOrder::getVisitorName, keyword)
                    .or().like(TicketOrder::getVisitorPhone, keyword));
        }
        wrapper.orderByDesc(TicketOrder::getCreatedAt);
        
        return ticketOrderMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(Long orderId, String status) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        order.setStatus(status);
        
        // 根据状态设置相应的时间
        switch (status) {
            case "paid":
                order.setPaymentTime(LocalDateTime.now());
                break;
            case "used":
                order.setUsedTime(LocalDateTime.now());
                break;
            case "cancelled":
                order.setCancelledTime(LocalDateTime.now());
                break;
            case "refunded":
                order.setRefundedTime(LocalDateTime.now());
                break;
        }
        
        return ticketOrderMapper.updateById(order) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> verifyTickets(List<Long> orderIds) {
        int success = 0;
        int failed = 0;
        
        for (Long orderId : orderIds) {
            try {
                TicketOrder order = ticketOrderMapper.selectById(orderId);
                if (order == null) {
                    log.warn("核销失败：订单 {} 不存在", orderId);
                    failed++;
                    continue;
                }
                
                if (!"paid".equals(order.getStatus())) {
                    log.warn("核销失败：订单 {} 状态为 {}，不是已支付状态", orderId, order.getStatus());
                    failed++;
                    continue;
                }
                
                order.setStatus("used");
                order.setUsedTime(LocalDateTime.now());
                
                if (ticketOrderMapper.updateById(order) > 0) {
                    log.info("核销成功：订单 {}", orderId);
                    success++;
                } else {
                    log.warn("核销失败：订单 {} 更新失败", orderId);
                    failed++;
                }
            } catch (Exception e) {
                log.error("核销异常：订单 {} - {}", orderId, e.getMessage());
                failed++;
            }
        }
        
        Map<String, Integer> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }
    
    @Override
    public Map<String, Object> getStatistics(Long scenicId, LocalDate startDate, LocalDate endDate) {
        return ticketOrderMapper.getStatistics(scenicId, startDate, endDate);
    }
}

