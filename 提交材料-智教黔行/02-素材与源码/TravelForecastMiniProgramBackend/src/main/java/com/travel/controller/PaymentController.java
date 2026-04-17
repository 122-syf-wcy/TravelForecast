package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.MpOrder;
import com.travel.entity.UserPoints;
import com.travel.mapper.MpOrderMapper;
import com.travel.mapper.UserPointsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 微信支付控制器
 *
 * 支付流程：
 * 1. 小程序下单后调用 /api/pay/create 获取预支付参数
 * 2. 小程序端使用参数调用 wx.requestPayment()
 * 3. 支付完成后微信服务器回调 /api/pay/notify
 * 4. 也支持黔豆（积分）兑换支付
 *
 * 配置说明：
 * - 需要在 application.yml 中配置 wechat.pay.mch-id 和 wechat.pay.api-key
 * - 未配置时使用模拟支付模式（直接标记为已支付）
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final MpOrderMapper orderMapper;
    private final UserPointsMapper pointsMapper;

    @Value("${wechat.pay.mch-id:}")
    private String mchId;

    @Value("${wechat.pay.api-key:}")
    private String apiKey;

    @Value("${wechat.appid:}")
    private String appId;

    /**
     * 创建支付 - 返回微信支付参数
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createPayment(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Long userId = Long.valueOf(body.get("userId").toString());

        MpOrder order = orderMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (!order.getUserId().equals(userId)) return Result.error("无权操作此订单");
        if (!"pending".equals(order.getStatus())) return Result.error("订单状态不允许支付");

        Map<String, Object> result = new HashMap<>();

        if (mchId != null && !mchId.isBlank() && apiKey != null && !apiKey.isBlank()) {
            // 真实微信支付模式：生成预支付参数
            try {
                Map<String, String> payParams = generateWxPayParams(order);
                result.put("mode", "wechat");
                result.put("payParams", payParams);
                log.info("微信支付参数生成成功: orderId={}, orderNo={}", orderId, order.getOrderNo());
            } catch (Exception e) {
                log.error("微信支付参数生成失败: {}", e.getMessage());
                return Result.error("支付参数生成失败: " + e.getMessage());
            }
        } else {
            // 开发模式：直接标记为已支付
            order.setStatus("paid");
            order.setPaymentTime(LocalDateTime.now());
            orderMapper.updateById(order);
            result.put("mode", "dev");
            result.put("message", "开发模式：订单已直接标记为已支付");
            log.info("开发模式支付: orderId={}, orderNo={}", orderId, order.getOrderNo());
        }

        result.put("orderId", orderId);
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", order.getTotalAmount());
        return Result.success(result);
    }

    /**
     * 微信支付回调通知
     */
    @PostMapping("/notify")
    public String payNotify(@RequestBody String xmlData) {
        log.info("收到微信支付回调");

        try {
            // 解析回调XML中的订单号
            String orderNo = extractXmlValue(xmlData, "out_trade_no");
            String resultCode = extractXmlValue(xmlData, "result_code");

            if ("SUCCESS".equals(resultCode) && orderNo != null) {
                MpOrder order = orderMapper.selectOne(
                    new LambdaQueryWrapper<MpOrder>().eq(MpOrder::getOrderNo, orderNo)
                );

                if (order != null && "pending".equals(order.getStatus())) {
                    order.setStatus("paid");
                    order.setPaymentTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                    log.info("订单支付成功: orderNo={}", orderNo);
                }
            }

            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            log.error("处理支付回调失败: {}", e.getMessage());
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
    }

    /**
     * 黔豆（积分）支付
     */
    @PostMapping("/points")
    public Result<Map<String, Object>> payWithPoints(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Long userId = Long.valueOf(body.get("userId").toString());

        MpOrder order = orderMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (!order.getUserId().equals(userId)) return Result.error("无权操作此订单");
        if (!"pending".equals(order.getStatus())) return Result.error("订单状态不允许支付");

        // 计算所需黔豆（1元 = 10黔豆）
        int requiredPoints = order.getTotalAmount().multiply(new java.math.BigDecimal("10")).intValue();

        UserPoints up = pointsMapper.selectOne(
            new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, userId)
        );

        if (up == null || (up.getTotalPoints() - up.getUsedPoints()) < requiredPoints) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("requiredPoints", requiredPoints);
            result.put("availablePoints", up != null ? (up.getTotalPoints() - up.getUsedPoints()) : 0);
            result.put("message", "黔豆不足");
            return Result.success(result);
        }

        // 扣除黔豆
        up.setUsedPoints(up.getUsedPoints() + requiredPoints);
        pointsMapper.updateById(up);

        // 更新订单状态
        order.setStatus("paid");
        order.setPaymentTime(LocalDateTime.now());
        order.setRemark("黔豆支付：" + requiredPoints + "黔豆");
        orderMapper.updateById(order);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("usedPoints", requiredPoints);
        result.put("remainingPoints", up.getTotalPoints() - up.getUsedPoints());
        result.put("message", "黔豆支付成功");
        log.info("黔豆支付成功: orderId={}, points={}", orderId, requiredPoints);
        return Result.success(result);
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> payStatus(@RequestParam Long orderId) {
        MpOrder order = orderMapper.selectById(orderId);
        if (order == null) return Result.error("订单不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("paymentTime", order.getPaymentTime());
        return Result.success(result);
    }

    // ---- 辅助方法 ----

    private Map<String, String> generateWxPayParams(MpOrder order) throws Exception {
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String prepayId = "wx_prepay_" + order.getOrderNo(); // 实际应调用微信统一下单API获取

        // 构建签名参数
        Map<String, String> params = new TreeMap<>();
        params.put("appId", appId);
        params.put("timeStamp", timeStamp);
        params.put("nonceStr", nonceStr);
        params.put("package", "prepay_id=" + prepayId);
        params.put("signType", "HMAC-SHA256");

        // 生成签名
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("key=").append(apiKey);

        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = hmacSha256.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        params.put("paySign", hexString.toString().toUpperCase());

        return params;
    }

    private String extractXmlValue(String xml, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            String value = xml.substring(start + startTag.length(), end);
            // 去除 CDATA
            if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
                value = value.substring(9, value.length() - 3);
            }
            return value;
        }
        return null;
    }
}
