package com.travel.controller;

import com.travel.common.Result;
import com.travel.dto.WechatLoginRequest;
import com.travel.dto.WechatLoginResponse;
import com.travel.dto.WechatUserInfoRequest;
import com.travel.dto.WechatUserInfoResponse;
import com.travel.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/auth/wechat")
@RequiredArgsConstructor
public class WechatLoginController {

    private final WechatService wechatService;

    /**
     * 微信小程序登录
     *
     * 小程序流程：
     * 1. 小程序调用 wx.login() 获取 code
     * 2. （可选）小程序通过 chooseAvatar + nickname input 获取用户头像和昵称
     * 3. 小程序将 code + nickname + avatarUrl 发送到此接口
     * 4. 后端使用 code 调用微信 API 获取 openid 和 session_key
     * 5. 后端创建或更新用户，返回 JWT token + 用户信息
     *
     * 请求示例：
     * POST /api/auth/wechat/login
     * {
     *   "code": "0813xxx...",
     *   "nickname": "用户昵称",
     *   "avatarUrl": "https://...",
     *   "encryptedData": "xxx...(可选，兼容旧版)",
     *   "iv": "xxx...(可选，兼容旧版)"
     * }
     */
    @PostMapping("/login")
    public Result<WechatLoginResponse> wechatLogin(@RequestBody WechatLoginRequest request) {
        try {
            log.info("处理微信登录请求，code={}, hasNickname={}, hasAvatar={}",
                    request.getCode(),
                    request.getNickname() != null && !request.getNickname().isBlank(),
                    request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank());
            
            WechatLoginResponse response = wechatService.wechatLogin(request);
            return Result.success(response, "微信登录成功");
        } catch (Exception e) {
            log.error("微信登录失败", e);
            return Result.error("微信登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/user-info")
    public Result<WechatUserInfoResponse> updateUserInfo(@RequestBody WechatUserInfoRequest request) {
        try {
            WechatUserInfoResponse response = wechatService.updateUserInfo(
                request.getUserId(),
                request.getOpenid(),
                request.getNickname(),
                request.getAvatarUrl(),
                request.getGender(),
                request.getCountry(),
                request.getProvince(),
                request.getCity(),
                request.getLanguage()
            );
            return Result.success(response, "用户信息更新成功");
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 头像文件上传接口：接收小程序 chooseAvatar 选择的头像文件
     * 上传到 OSS 并更新用户头像
     */
    @PostMapping("/upload-avatar")
    public Result<WechatUserInfoResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "nickname", required = false) String nickname) {
        try {
            WechatUserInfoResponse response = wechatService.uploadAvatarFile(file, userId, nickname);
            return Result.success(response, "头像上传成功");
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 头像代理接口：后端从 OSS 下载图片并返回给前端
     * 绕过 OSS 外部访问限制（防盗链/ACL 等导致的 403）
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        try {
            byte[] imageData = wechatService.getAvatarImage(userId);
            if (imageData == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(imageData);
        } catch (Exception e) {
            log.error("获取头像失败: userId={}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
