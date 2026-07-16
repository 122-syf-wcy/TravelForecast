package com.travel.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.OperationLog;
import com.travel.common.Result;
import com.travel.entity.user.User;
import com.travel.dto.admin.MerchantStats;
import com.travel.dto.admin.MerchantListItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.service.admin.AdminMerchantService;
import com.travel.mapper.user.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.travel.service.merchant.MerchantProfileService;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.mapper.merchant.MerchantAuditLogMapper;
import com.travel.entity.merchant.MerchantAuditLog;
import com.travel.dto.admin.MerchantAuditLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 管理员端-商家审核
 * （后续可加上管理员登录的鉴权注解/拦截）
 */
@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    @Autowired
    private AdminMerchantService adminMerchantService;

    @Autowired
    private MerchantProfileService merchantProfileService;

    @Autowired
    private MerchantAuditLogMapper merchantAuditLogMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 待审核商家分页
     */
    @GetMapping("/pending")
    public Result<Page<User>> pending(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminMerchantService.pagePendingMerchants(page, size));
    }

    /**
     * 审核通过
     */
    @OperationLog(module = "商户管理", description = "审批商家入驻")
    @PostMapping("/approve/{userId}")
    public Result<Void> approve(@PathVariable Long userId, @RequestBody(required = false) com.travel.dto.admin.AuditDecisionRequest body, HttpServletRequest request) {
        // 审核前对资料必填项进行校验
        adminMerchantService.assertProfileRequiredFilled(userId);
        Long adminUserId = (Long) request.getAttribute("userId");
        String reason = body == null ? null : body.getReason();
        adminMerchantService.approve(userId, adminUserId, reason);
        return Result.success(null, "审核通过");
    }

    /**
     * 审核拒绝
     */
    @OperationLog(module = "商户管理", description = "拒绝商家入驻", level = "警告")
    @PostMapping("/reject/{userId}")
    public Result<Void> reject(@PathVariable Long userId, @RequestBody(required = false) com.travel.dto.admin.AuditDecisionRequest body, HttpServletRequest request) {
        Long adminUserId = (Long) request.getAttribute("userId");
        String reason = body == null ? null : body.getReason();
        adminMerchantService.reject(userId, adminUserId, reason);
        return Result.success(null, "已拒绝");
    }

    /**
     * 管理员查看商家资料
     */
    @GetMapping("/profile/{userId}")
    public Result<MerchantProfile> getProfile(@PathVariable Long userId) {
        return Result.success(merchantProfileService.getByUserId(userId));
    }

    /**
     * 管理员保存商家资料
     */
    @OperationLog(module = "商户管理", description = "保存商家资料")
    @PostMapping("/profile")
    public Result<MerchantProfile> saveProfile(@RequestBody MerchantProfile profile) {
        return Result.success(merchantProfileService.saveOrUpdate(profile));
    }

    /**
     * 审核操作日志查询
     */
    @GetMapping("/audit-logs")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<MerchantAuditLogVO>> auditLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long userId) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MerchantAuditLog> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<MerchantAuditLog> qw = new LambdaQueryWrapper<>();
        if (userId != null) {
            qw.eq(MerchantAuditLog::getUserId, userId);
        }
        qw.orderByDesc(MerchantAuditLog::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MerchantAuditLog> raw = merchantAuditLogMapper.selectPage(p, qw);
        // 映射为VO，补充管理员用户名/昵称
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MerchantAuditLogVO> voPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size, raw.getTotal());
        java.util.List<MerchantAuditLogVO> vos = raw.getRecords().stream().map(log -> {
            MerchantAuditLogVO vo = new MerchantAuditLogVO();
            vo.setId(log.getId());
            vo.setUserId(log.getUserId());
            vo.setAdminUserId(log.getAdminUserId());
            vo.setAction(log.getAction());
            vo.setRemark(log.getRemark());
            vo.setFromStatus(log.getFromStatus());
            vo.setToStatus(log.getToStatus());
            vo.setCreatedAt(log.getCreatedAt());
            return vo;
        }).toList();
        // 查询管理员用户名/昵称（批量）
        java.util.Set<Long> adminIds = new java.util.HashSet<>();
        for (MerchantAuditLogVO v : vos) { if (v.getAdminUserId() != null) adminIds.add(v.getAdminUserId()); }
        if (!adminIds.isEmpty()) {
            java.util.List<com.travel.entity.user.User> admins = userMapper.selectBatchIds(new java.util.ArrayList<>(adminIds));
            java.util.Map<Long, com.travel.entity.user.User> map = new java.util.HashMap<>();
            for (com.travel.entity.user.User a : admins) map.put(a.getUserId(), a);
            for (MerchantAuditLogVO v : vos) {
                com.travel.entity.user.User a = map.get(v.getAdminUserId());
                if (a != null) {
                    v.setAdminUsername(a.getUsername());
                    v.setAdminNickname(a.getNickname());
                }
            }
        }
        voPage.setRecords(vos);
        return Result.success(voPage);
    }

    /**
     * 商家统计卡片
     */
    @GetMapping("/stats")
    public Result<MerchantStats> stats() {
        return Result.success(adminMerchantService.stats());
    }

    /**
     * 商户综合列表（users + merchant_profiles 合并）
     */
    @GetMapping("/list")
    public Result<Page<MerchantListItem>> list(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword) {
        Page<MerchantListItem> p = adminMerchantService.list(page, size, keyword);
        return Result.success(p);
    }
}


