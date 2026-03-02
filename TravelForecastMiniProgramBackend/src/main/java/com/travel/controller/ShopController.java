package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.*;
import com.travel.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ProductMapper productMapper;
    private final CartMapper cartMapper;
    private final MpOrderMapper orderMapper;
    private final MpOrderItemMapper orderItemMapper;
    private final OssProxyController ossProxy;

    // ---- 商品 ----
    @GetMapping("/products")
    public Result<List<Product>> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, "ACTIVE");
        if (category != null && !category.isBlank()) {
            qw.eq(Product::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Product::getName, keyword);
        }
        qw.orderByAsc(Product::getSortOrder);
        List<Product> list = productMapper.selectList(qw);
        list.forEach(this::proxyProductImage);
        return Result.success(list);
    }

    @GetMapping("/products/{id}")
    public Result<Product> productDetail(@PathVariable Long id) {
        Product p = productMapper.selectById(id);
        if (p != null) proxyProductImage(p);
        return Result.success(p);
    }

    private void proxyProductImage(Product p) {
        if (p != null && p.getImageUrl() != null) {
            p.setImageUrl(ossProxy.toProxyUrl(p.getImageUrl()));
        }
    }

    // ---- 购物车 ----
    @GetMapping("/cart")
    public Result<List<Map<String, Object>>> getCart(@RequestParam Long userId) {
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId);
        List<Cart> carts = cartMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Cart c : carts) {
            Map<String, Object> item = new HashMap<>();
            item.put("cartId", c.getId());
            item.put("quantity", c.getQuantity());
            Product p = productMapper.selectById(c.getProductId());
            if (p != null) {
                proxyProductImage(p);
                item.put("product", p);
            }
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping("/cart/add")
    public Result<String> addToCart(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long productId = Long.valueOf(body.get("productId").toString());
        int quantity = body.containsKey("quantity") ? Integer.parseInt(body.get("quantity").toString()) : 1;

        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId).eq(Cart::getProductId, productId);
        Cart exist = cartMapper.selectOne(qw);
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cartMapper.insert(cart);
        }
        return Result.success("已加入购物车");
    }

    @DeleteMapping("/cart/{id}")
    public Result<String> removeFromCart(@PathVariable Long id) {
        cartMapper.deleteById(id);
        return Result.success("已移除");
    }

    // ---- 订单 ----
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> getOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<MpOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(MpOrder::getUserId, userId);
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            qw.eq(MpOrder::getStatus, status);
        }
        qw.orderByDesc(MpOrder::getCreatedAt);
        List<MpOrder> orders = orderMapper.selectList(qw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (MpOrder o : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("order", o);
            LambdaQueryWrapper<MpOrderItem> iqw = new LambdaQueryWrapper<>();
            iqw.eq(MpOrderItem::getOrderId, o.getId());
            map.put("items", orderItemMapper.selectList(iqw));
            result.add(map);
        }
        return Result.success(result);
    }

    @PostMapping("/orders")
    public Result<MpOrder> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

        String orderNo = "MP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));

        BigDecimal total = BigDecimal.ZERO;
        List<MpOrderItem> orderItems = new ArrayList<>();
        for (Map<String, Object> item : items) {
            MpOrderItem oi = new MpOrderItem();
            oi.setProductId(item.containsKey("productId") ? Long.valueOf(item.get("productId").toString()) : null);
            oi.setProductName(item.getOrDefault("productName", "").toString());
            oi.setProductImage(item.containsKey("productImage") ? item.get("productImage").toString() : null);
            BigDecimal price = new BigDecimal(item.get("price").toString());
            int qty = item.containsKey("quantity") ? Integer.parseInt(item.get("quantity").toString()) : 1;
            oi.setPrice(price);
            oi.setQuantity(qty);
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
            orderItems.add(oi);
        }

        MpOrder order = new MpOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus("pending");
        orderMapper.insert(order);

        for (MpOrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }

        return Result.success(order);
    }

    @PutMapping("/orders/{id}/status")
    public Result<String> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        MpOrder order = orderMapper.selectById(id);
        if (order == null) return Result.error("订单不存在");
        String newStatus = body.get("status");
        order.setStatus(newStatus);
        if ("paid".equals(newStatus)) order.setPaymentTime(LocalDateTime.now());
        if ("cancelled".equals(newStatus)) order.setCancelTime(LocalDateTime.now());
        if ("completed".equals(newStatus)) order.setCompleteTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.success("状态已更新");
    }

    @DeleteMapping("/orders/{id}")
    public Result<String> deleteOrder(@PathVariable Long id) {
        orderMapper.deleteById(id);
        return Result.success("已删除");
    }
}
