package com.travel.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.user.User;
import com.travel.exception.BusinessException;
import com.travel.mapper.user.UserMapper;
import com.travel.service.admin.AdminMerchantService;
import com.travel.dto.admin.MerchantStats;
import com.travel.dto.admin.MerchantListItem;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.mapper.merchant.MerchantAuditLogMapper;
import com.travel.entity.merchant.MerchantAuditLog;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.service.merchant.MerchantProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@Service
public class AdminMerchantServiceImpl implements AdminMerchantService {

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:syf120508@foxmail.com}")
    private String adminMailTo;

    @Autowired
    private MerchantProfileService merchantProfileService;

    @Autowired
    private MerchantProfileMapper merchantProfileMapper;

    @Autowired
    private MerchantAuditLogMapper merchantAuditLogMapper;

    @Override
    public Page<User> pagePendingMerchants(long page, long size) {
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getRole, "merchant").eq(User::getStatus, "pending");
        return userMapper.selectPage(p, qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long userId, Long adminUserId, String reason) {
        // 审核前必填校验
        assertProfileRequiredFilled(userId);
        User u = userMapper.selectById(userId);
        if (u == null || !"merchant".equals(u.getRole())) {
            throw new BusinessException("商家不存在");
        }
        u.setStatus("active");
        userMapper.updateById(u);
        // 自动创建空白商家资料（若不存在）
        if (merchantProfileService.getByUserId(userId) == null) {
            merchantProfileService.createEmptyForUser(userId);
        }
        // 写入审核日志
        MerchantAuditLog log = new MerchantAuditLog();
        log.setUserId(userId);
        log.setAdminUserId(adminUserId);
        log.setAction("approve");
        log.setFromStatus("pending");
        log.setToStatus("active");
        log.setRemark(reason == null ? "审核通过" : reason);
        merchantAuditLogMapper.insert(log);

        notifyAdmin("商家审核通过", "商家账号已审核通过：" + u.getUsername());
        // 邮件通知商家
        notifyUser(u.getEmail(), "商家审核通过", "您的商家账号已审核通过，欢迎使用！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long userId, Long adminUserId, String reason) {
        User u = userMapper.selectById(userId);
        if (u == null || !"merchant".equals(u.getRole())) {
            throw new BusinessException("商家不存在");
        }
        u.setStatus("inactive");
        userMapper.updateById(u);
        // 写入审核日志
        MerchantAuditLog log = new MerchantAuditLog();
        log.setUserId(userId);
        log.setAdminUserId(adminUserId);
        log.setAction("reject");
        log.setFromStatus("pending");
        log.setToStatus("inactive");
        log.setRemark(reason == null ? "审核拒绝" : reason);
        merchantAuditLogMapper.insert(log);

        notifyAdmin("商家审核拒绝", "商家账号已被拒绝：" + u.getUsername());
        // 邮件通知商家（含拒绝理由）
        notifyUser(u.getEmail(), "商家审核未通过", "您的商家审核未通过，原因：" + (reason == null ? "未提供" : reason));
    }

    @Override
    public MerchantStats stats() {
        LambdaQueryWrapper<User> base = new LambdaQueryWrapper<>();
        base.eq(User::getRole, "merchant");

        long pending = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "merchant").eq(User::getStatus, "pending"));
        long approved = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "merchant").eq(User::getStatus, "active"));
        long rejected = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "merchant").eq(User::getStatus, "inactive"));
        long today = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "merchant")
                .ge(User::getCreatedAt, java.time.LocalDate.now().atStartOfDay()));

        return new MerchantStats(pending, approved, rejected, today);
    }

    @Override
    public Page<MerchantListItem> list(long page, long size, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> uq = new LambdaQueryWrapper<>();
        uq.eq(User::getRole, "merchant");
        if (keyword != null && !keyword.isEmpty()) {
            uq.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        Page<User> users = userMapper.selectPage(userPage, uq);

        Page<MerchantListItem> result = new Page<>(page, size, users.getTotal());
        result.setRecords(users.getRecords().stream().map(u -> {
            MerchantProfile mp = merchantProfileService.getByUserId(u.getUserId());
            MerchantListItem item = new MerchantListItem();
            item.setUserId(u.getUserId());
            item.setUsername(u.getUsername());
            item.setNickname(u.getNickname());
            item.setEmail(u.getEmail());
            item.setPhone(u.getPhone());
            item.setAvatar(u.getAvatar());
            item.setStatus(u.getStatus());
            if (mp != null) {
                item.setProfileId(mp.getId());
                item.setBusinessName(mp.getBusinessName());
                item.setCategory(null); // category字段已移除
                item.setContact(mp.getContactName());
                item.setAddress(mp.getAddress());
                item.setCreatedAt(mp.getCreatedAt());
            }
            return item;
        }).toList());

        return result;
    }

    @Override
    public void assertProfileRequiredFilled(Long userId) {
        // 注释掉严格的资料检查，允许管理员先通过审核，商家后续再完善资料
        // 这样商家注册后只需填写基本注册信息，管理员就可以审核通过
        
        // 可选：如果需要简单检查，可以只检查用户基本信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 商家资料可以为空，允许通过审核后再完善
        // MerchantProfile mp = merchantProfileService.getByUserId(userId);
        // if (mp == null ||
        //         isBlank(mp.getBusinessName()) ||
        //         isBlank(mp.getCategory()) ||
        //         isBlank(mp.getContact()) ||
        //         isBlank(mp.getPhone()) ||
        //         isBlank(mp.getLicenseNo())) {
        //     throw new BusinessException("商家资料未完善：请填写商户名称、类型、联系人、电话、营业执照号");
        // }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void notifyAdmin(String subject, String content) {
        if (mailSender == null) return; // 未配置邮件时忽略
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(adminMailTo);
            msg.setSubject(subject);
            msg.setText(content);
            mailSender.send(msg);
        } catch (Exception ignored) {}
    }

    private void notifyUser(String to, String subject, String content) {
        if (mailSender == null) return;
        if (to == null || to.isEmpty()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(content);
            mailSender.send(msg);
        } catch (Exception ignored) {}
    }
}


