package com.travel.controller.user;

import com.travel.common.Result;
import com.travel.dto.user.PasswordChangeRequest;
import com.travel.entity.user.User;
import com.travel.entity.user.UserSession;
import com.travel.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取指定用户信息
     */
    @GetMapping("/{userId}")
    public Result<User> getUserById(
            @PathVariable Long userId,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权查看其他用户信息");
        }

        log.info("获取用户信息: userId={}", userId);
        User user = userService.getUserById(userId);

        if (user != null) {
            user.setPassword(null);
        }

        return Result.success(user);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("获取当前用户信息: userId={}", userId);
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    public Result<User> updateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            log.warn("权限检查失败: userId={} != currentUserId={}", userId, currentUserId);
            return Result.error("无权修改其他用户信息");
        }

        log.info("更新用户信息: userId={}, updates={}", userId, updates.keySet());

        if (updates.containsKey("newPassword") && updates.containsKey("currentPassword")) {
            String currentPassword = (String) updates.get("currentPassword");
            String newPassword = (String) updates.get("newPassword");

            try {
                userService.changePassword(userId, currentPassword, newPassword);
                log.info("密码修改成功: userId={}", userId);
            } catch (Exception e) {
                return Result.error(e.getMessage());
            }

            updates.remove("currentPassword");
            updates.remove("newPassword");
        }

        User user = userService.updateUser(userId, updates);
        return Result.success(user, "更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/{userId}/password")
    public Result<Void> changePassword(
            @PathVariable Long userId,
            @RequestBody PasswordChangeRequest request,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权修改其他用户密码");
        }

        log.info("用户修改密码: userId={}", userId);
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success(null, "密码修改成功");
    }

    /**
     * 获取用户会话列表
     */
    @GetMapping("/{userId}/sessions")
    public Result<List<Map<String, Object>>> getUserSessions(
            @PathVariable Long userId,
            @RequestAttribute(value = "userId", required = false) Long currentUserId,
            HttpServletRequest request) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权查看其他用户会话");
        }

        log.info("获取用户会话列表: userId={}", userId);
        List<UserSession> sessions = userService.getUserSessions(userId);

        String currentIp = request.getRemoteAddr();
        String currentUserAgent = request.getHeader("User-Agent");

        List<Map<String, Object>> result = sessions.stream().map(session -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", session.getId());
            map.put("ipAddress", session.getIpAddress());
            map.put("userAgent", session.getUserAgent());
            map.put("loginAt", session.getLoginAt());
            map.put("expireAt", session.getExpiresAt());

            boolean isCurrent = Objects.equals(session.getIpAddress(), currentIp)
                && Objects.equals(session.getUserAgent(), currentUserAgent);
            map.put("isCurrent", isCurrent);

            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 撤销指定会话
     */
    @DeleteMapping("/{userId}/sessions/{sessionId}")
    public Result<Void> revokeSession(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权撤销其他用户会话");
        }

        log.info("撤销用户会话: userId={}, sessionId={}", userId, sessionId);
        userService.revokeSession(userId, sessionId);
        return Result.success(null, "会话已撤销");
    }

    /**
     * 更新用户偏好
     */
    @PutMapping("/{userId}/preferences")
    public Result<Void> updatePreferences(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> preferences,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权修改其他用户偏好");
        }

        log.info("更新用户偏好: userId={}", userId);
        userService.updatePreferences(userId, preferences);
        return Result.success(null, "偏好已更新");
    }

    /**
     * 获取用户偏好
     */
    @GetMapping("/{userId}/preferences")
    public Result<Map<String, Object>> getPreferences(
            @PathVariable Long userId,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权查看其他用户偏好");
        }

        log.info("获取用户偏好: userId={}", userId);
        Map<String, Object> preferences = userService.getPreferences(userId);
        return Result.success(preferences);
    }

    /**
     * 更新隐私设置
     */
    @PutMapping("/{userId}/privacy")
    public Result<Void> updatePrivacy(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> privacy,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权修改其他用户隐私设置");
        }

        log.info("更新隐私设置: userId={}", userId);
        userService.updatePrivacy(userId, privacy);
        return Result.success(null, "隐私设置已更新");
    }

    /**
     * 获取隐私设置
     */
    @GetMapping("/{userId}/privacy")
    public Result<Map<String, Object>> getPrivacy(
            @PathVariable Long userId,
            @RequestAttribute(value = "userId", required = false) Long currentUserId) {

        if (!userId.equals(currentUserId)) {
            return Result.error("无权查看其他用户隐私设置");
        }

        log.info("获取隐私设置: userId={}", userId);
        Map<String, Object> privacy = userService.getPrivacy(userId);
        return Result.success(privacy);
    }
}
