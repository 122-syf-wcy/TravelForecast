package com.travel.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.OperationLog;
import com.travel.common.Result;
import org.springframework.dao.DuplicateKeyException;
import com.travel.dto.user.ChangePasswordRequest;
import com.travel.dto.user.UpdateAccountRequest;
import com.travel.entity.user.User;
import com.travel.mapper.user.UserMapper;
import com.travel.utils.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final UserMapper userMapper;

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getAccountInfo(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error("未登录");
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getUserId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("avatar", user.getAvatar());
        info.put("role", user.getRole());
        info.put("createdAt", user.getCreatedAt());
        info.put("loginAt", user.getLoginAt());

        return Result.success(info);
    }

    /**
     * 更新管理员信息
     */
    @OperationLog(module = "账户管理", description = "更新账户信息")
    @PutMapping("/info")
    public Result<Void> updateAccountInfo(
            @RequestBody UpdateAccountRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        //判断是否登录
        if (userId == null) {
            return Result.error("未登录");
        }

        //获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 邮箱唯一性校验
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> emailCheck = new LambdaQueryWrapper<>();
            // 查询条件：邮箱相同但用户ID不同
            emailCheck.eq(User::getEmail, request.getEmail()).ne(User::getUserId, userId);
            if (userMapper.selectCount(emailCheck) > 0) {
                return Result.error("该邮箱已被其他用户使用");
            }
        }

        // 用户名唯一性校验
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<User> nameCheck = new LambdaQueryWrapper<>();
            // 查询条件：用户名相同但用户ID不同
            nameCheck.eq(User::getUsername, request.getUsername()).ne(User::getUserId, userId);
            if (userMapper.selectCount(nameCheck) > 0) {
                return Result.error("该用户名已被其他用户使用");
            }
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        // 更新时间
        user.setUpdatedAt(LocalDateTime.now());
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("uk_email")) {
                return Result.error("该邮箱已被其他用户使用");
            } else if (msg != null && msg.contains("uk_username")) {
                return Result.error("该用户名已被其他用户使用");
            }
            return Result.error("信息保存失败，存在重复数据");
        }

        log.info("管理员 {} 更新了账户信息", userId);
        return Result.success(null, "信息更新成功");
    }

    /**
     * 修改密码
     */
    @OperationLog(module = "账户管理", description = "修改管理员密码")
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            return Result.error("请输入当前密码");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return Result.error("请输入新密码");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("两次输入的密码不一致");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!PasswordUtil.matches(request.getCurrentPassword(), user.getPassword())) {
            return Result.error("当前密码不正确");
        }

        user.setPassword(PasswordUtil.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员 {} 修改了密码", userId);
        return Result.success(null, "密码修改成功");
    }
}
