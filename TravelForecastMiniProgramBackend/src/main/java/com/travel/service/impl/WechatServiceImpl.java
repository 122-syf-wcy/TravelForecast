package com.travel.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.travel.dto.WechatLoginRequest;
import com.travel.dto.WechatLoginResponse;
import com.travel.dto.WechatSessionResponse;
import com.travel.dto.WechatUserInfoResponse;
import com.travel.entity.User;
import com.travel.entity.WechatUser;
import com.travel.mapper.UserMapper;
import com.travel.mapper.WechatUserMapper;
import com.travel.service.OssService;
import com.travel.service.WechatService;
import com.travel.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatServiceImpl implements WechatService {

    private final UserMapper userMapper;
    private final WechatUserMapper wechatUserMapper;
    private final OssService ossService;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${wechat.miniapp.appid}")
    private String appId;

    @Value("${wechat.miniapp.appsecret}")
    private String appSecret;

    @Value("${oss.publicDomain}")
    private String ossPublicDomain;

    @Value("${app.base-url:http://localhost:8082}")
    private String appBaseUrl;

    @Override
    public WechatLoginResponse wechatLogin(WechatLoginRequest request) throws Exception {
        log.info("开始微信小程序登录，code={}", request.getCode());

        // 1. 使用 code 调用微信 API 获取 openid 和 session_key
        WechatSessionResponse sessionResponse = exchangeCodeForSession(request.getCode());
        
        if (!sessionResponse.isSuccess()) {
            log.error("微信API调用失败: errcode={}, errmsg={}",
                    sessionResponse.getErrcode(), sessionResponse.getErrmsg());
            throw new RuntimeException("微信登录失败: " + sessionResponse.getErrmsg());
        }

        String openid = sessionResponse.getOpenid();
        String unionid = sessionResponse.getUnionid();
        String sessionKey = sessionResponse.getSessionKey();
        log.info("获取到openid: {}", openid);

        // 2. 提取用户昵称和头像（优先使用前端直接传递的，其次尝试解密 encryptedData）
        String nickname = null;
        String avatarUrl = null;

        // 方式1：前端通过新版组件（chooseAvatar + nickname input）直接传递
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            nickname = request.getNickname();
            log.info("从请求中获取到昵称: {}", nickname);
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            avatarUrl = request.getAvatarUrl();
            log.info("从请求中获取到头像URL");
        }

        // 方式2：尝试解密 encryptedData 获取用户信息（兼容旧版 wx.getUserProfile / wx.getUserInfo）
        if ((nickname == null || avatarUrl == null)
                && request.getEncryptedData() != null && !request.getEncryptedData().isBlank()
                && request.getIv() != null && !request.getIv().isBlank()
                && sessionKey != null) {
            try {
                String decryptedData = decryptUserData(request.getEncryptedData(), sessionKey, request.getIv());
                log.info("解密用户数据成功: {}", decryptedData);
                JSONObject userInfo = JSON.parseObject(decryptedData);
                if (nickname == null && userInfo.containsKey("nickName")) {
                    nickname = userInfo.getString("nickName");
                }
                if (avatarUrl == null && userInfo.containsKey("avatarUrl")) {
                    avatarUrl = userInfo.getString("avatarUrl");
                }
            } catch (Exception e) {
                log.warn("解密用户数据失败，将使用默认值: {}", e.getMessage());
            }
        }

        // 如果仍然没有昵称，使用默认值
        if (nickname == null || nickname.isBlank()) {
            nickname = "微信用户";
        }

        // 3. 查找或创建用户
        WechatUser wechatUser = wechatUserMapper.selectByOpenid(openid);
        User user;
        
        if (wechatUser == null) {
            // 新用户：创建用户和微信用户关联
            user = createWechatUser(openid, nickname, avatarUrl);
            userMapper.insert(user);
            log.info("创建新的系统用户: userId={}, nickname={}", user.getUserId(), nickname);
            
            wechatUser = new WechatUser();
            wechatUser.setUserId(user.getUserId());
            wechatUser.setOpenid(openid);
            wechatUser.setUnionid(unionid);
            wechatUser.setNickname(nickname);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                wechatUser.setAvatarUrl(avatarUrl);
            }
            wechatUser.setStatus("ACTIVE");
            wechatUser.setLastLoginTime(LocalDateTime.now());
            wechatUser.setSessionKey(sessionKey);
            wechatUserMapper.insert(wechatUser);
            log.info("创建微信用户关联: wechatUserId={}, openid={}", wechatUser.getId(), openid);
        } else {
            // 已存在微信用户：检查 users 表中是否有对应记录
            Long oldUserId = wechatUser.getUserId();
            user = userMapper.selectById(oldUserId);
            if (user == null) {
                // users 表中没有记录，创建新用户
                user = createWechatUser(openid, nickname, avatarUrl);
                userMapper.insert(user);
                wechatUser.setUserId(user.getUserId());
                log.info("创建新用户并更新微信用户关联: oldUserId={}, newUserId={}", 
                    oldUserId, user.getUserId());
            }
            
            // 如果前端传了新的昵称或头像，更新已有用户的信息
            boolean userUpdated = false;
            if (nickname != null && !nickname.isBlank() && !"微信用户".equals(nickname)) {
                user.setNickname(nickname);
                wechatUser.setNickname(nickname);
                userUpdated = true;
                log.info("更新用户昵称: userId={}, nickname={}", user.getUserId(), nickname);
            }
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatar(avatarUrl);
                wechatUser.setAvatarUrl(avatarUrl);
                userUpdated = true;
                log.info("更新用户头像: userId={}", user.getUserId());
            }
            if (userUpdated) {
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
            
            // 更新微信用户登录信息
            wechatUser.setLastLoginTime(LocalDateTime.now());
            wechatUser.setSessionKey(sessionKey);
            wechatUserMapper.updateById(wechatUser);
        }

        // 4. 登录时检查头像是否已上传到 OSS，如果还是微信URL则自动转存
        String currentAvatar = user.getAvatar();
        if (currentAvatar != null && !currentAvatar.isBlank()
                && !currentAvatar.startsWith(ossPublicDomain)
                && !currentAvatar.startsWith(appBaseUrl)) {
            try {
                String ossAvatarUrl = ossService.uploadAvatarFromUrl(currentAvatar, user.getUserId());
                user.setAvatar(ossAvatarUrl);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);

                wechatUser.setAvatarUrl(ossAvatarUrl);
                wechatUser.setUpdatedAt(LocalDateTime.now());
                wechatUserMapper.updateById(wechatUser);
                log.info("登录时自动将头像转存OSS: userId={}", user.getUserId());
            } catch (Exception e) {
                log.warn("登录时头像转存OSS失败，使用原头像: {}", e.getMessage());
            }
        }

        // 5. 生成 JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        long expiresIn = 7 * 24 * 3600;

        // 头像使用后端代理 URL，绕过 OSS 外部访问限制
        String avatarProxyUrl = buildAvatarProxyUrl(user.getUserId(), user.getAvatar());

        log.info("微信登录成功: userId={}, nickname={}, hasAvatar={}", 
                user.getUserId(), user.getNickname(), avatarProxyUrl != null);

        return new WechatLoginResponse(
            token,
            user.getUserId(),
            user.getNickname(),
            avatarProxyUrl,
            openid,
            unionid,
            user.getRole(),
            System.currentTimeMillis(),
            System.currentTimeMillis() + expiresIn * 1000
        );
    }

    private WechatSessionResponse exchangeCodeForSession(String code) {
        try {
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code
            );
            log.debug("调用微信API: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            log.debug("微信API响应: {}", response);
            return JSON.parseObject(response, WechatSessionResponse.class);
        } catch (Exception e) {
            log.error("调用微信jscode2session接口失败", e);
            WechatSessionResponse error = new WechatSessionResponse();
            error.setErrcode(-1);
            error.setErrmsg(e.getMessage());
            return error;
        }
    }

    /**
     * 创建微信用户（支持传入昵称和头像）
     */
    private User createWechatUser(String openid, String nickname, String avatarUrl) {
        User user = new User();
        user.setUsername("wx_" + openid.substring(0, 8));
        // 微信用户不需要密码登录，设置一个随机占位密码以满足数据库 NOT NULL 约束
        user.setPassword("WECHAT_USER_" + java.util.UUID.randomUUID().toString().replace("-", ""));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : "微信用户");
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatar(avatarUrl);
        }
        user.setRole("USER");
        user.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        user.setLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    @Override
    public String decryptUserData(String encryptedData, String sessionKey, String iv) throws Exception {
        byte[] dataByte = Base64.getDecoder().decode(encryptedData);
        byte[] keyByte = Base64.getDecoder().decode(sessionKey);
        byte[] ivByte = Base64.getDecoder().decode(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec spec = new SecretKeySpec(keyByte, "AES");
        IvParameterSpec params = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, spec, params);

        byte[] resultByte = cipher.doFinal(dataByte);
        return new String(resultByte, StandardCharsets.UTF_8);
    }

    @Override
    public boolean verifySignature(String rawData, String signature, String sessionKey) {
        try {
            String str = rawData + sessionKey;
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public WechatUserInfoResponse updateUserInfo(Long userId, String openid, String nickname, String avatarUrl,
                                                 Integer gender, String country, String province, String city, String language) throws Exception {
        WechatUser wechatUser = null;
        if (openid != null && !openid.isBlank()) {
            wechatUser = wechatUserMapper.selectByOpenid(openid);
        }
        if (wechatUser == null && userId != null) {
            wechatUser = wechatUserMapper.selectByUserId(userId);
        }

        String finalAvatarUrl = avatarUrl;
        if (finalAvatarUrl != null && !finalAvatarUrl.isBlank()) {
            finalAvatarUrl = ossService.uploadAvatarFromUrl(finalAvatarUrl, userId);
        }

        if (wechatUser != null) {
            if (nickname != null && !nickname.isBlank()) {
                wechatUser.setNickname(nickname);
            }
            if (finalAvatarUrl != null && !finalAvatarUrl.isBlank()) {
                wechatUser.setAvatarUrl(finalAvatarUrl);
            }
            if (gender != null) {
                wechatUser.setGender(gender);
            }
            if (country != null) {
                wechatUser.setCountry(country);
            }
            if (province != null) {
                wechatUser.setProvince(province);
            }
            if (city != null) {
                wechatUser.setCity(city);
            }
            if (language != null) {
                wechatUser.setLanguage(language);
            }
            wechatUser.setUpdatedAt(LocalDateTime.now());
            wechatUserMapper.updateById(wechatUser);
        }

        Long finalUserId = userId;
        if (finalUserId == null && wechatUser != null) {
            finalUserId = wechatUser.getUserId();
        }
        if (finalUserId != null) {
            User user = userMapper.selectById(finalUserId);
            if (user != null) {
                if (nickname != null && !nickname.isBlank()) {
                    user.setNickname(nickname);
                }
                if (finalAvatarUrl != null && !finalAvatarUrl.isBlank()) {
                    user.setAvatar(finalAvatarUrl);
                }
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
        // 头像使用后端代理 URL
        String avatarProxyUrl = buildAvatarProxyUrl(finalUserId, finalAvatarUrl);

        return new WechatUserInfoResponse(
            finalUserId,
            nickname,
            avatarProxyUrl
        );
    }

    @Override
    public WechatUserInfoResponse uploadAvatarFile(MultipartFile file, Long userId, String nickname) throws Exception {
        // 1. 上传头像文件到 OSS
        String ossAvatarUrl = ossService.uploadAvatarFile(file, userId);
        log.info("头像文件上传OSS成功: userId={}, ossUrl={}", userId, ossAvatarUrl);

        // 2. 更新 users 表
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setAvatar(ossAvatarUrl);
            if (nickname != null && !nickname.isBlank()) {
                user.setNickname(nickname);
            }
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 3. 更新 wechat_users 表
        WechatUser wechatUser = wechatUserMapper.selectByUserId(userId);
        if (wechatUser != null) {
            wechatUser.setAvatarUrl(ossAvatarUrl);
            if (nickname != null && !nickname.isBlank()) {
                wechatUser.setNickname(nickname);
            }
            wechatUser.setUpdatedAt(LocalDateTime.now());
            wechatUserMapper.updateById(wechatUser);
        }

        // 4. 返回代理 URL
        String avatarProxyUrl = buildAvatarProxyUrl(userId, ossAvatarUrl);
        String finalNickname = nickname;
        if ((finalNickname == null || finalNickname.isBlank()) && user != null) {
            finalNickname = user.getNickname();
        }

        return new WechatUserInfoResponse(userId, finalNickname, avatarProxyUrl);
    }

    @Override
    public byte[] getAvatarImage(Long userId) {
        if (userId == null) return null;
        User user = userMapper.selectById(userId);
        if (user == null || user.getAvatar() == null || user.getAvatar().isBlank()) return null;
        return ossService.downloadFromOss(user.getAvatar());
    }

    /**
     * 构建头像代理 URL：http://localhost:8082/api/auth/wechat/avatar/{userId}
     */
    private String buildAvatarProxyUrl(Long userId, String avatar) {
        if (userId == null || avatar == null || avatar.isBlank()) return null;
        return appBaseUrl + "/api/auth/wechat/avatar/" + userId;
    }
}
