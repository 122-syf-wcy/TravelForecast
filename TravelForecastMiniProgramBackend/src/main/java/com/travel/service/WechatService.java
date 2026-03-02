package com.travel.service;

import com.travel.dto.WechatLoginRequest;
import com.travel.dto.WechatLoginResponse;
import com.travel.dto.WechatUserInfoResponse;
import org.springframework.web.multipart.MultipartFile;

public interface WechatService {
    WechatLoginResponse wechatLogin(WechatLoginRequest request) throws Exception;
    String decryptUserData(String encryptedData, String sessionKey, String iv) throws Exception;
    boolean verifySignature(String rawData, String signature, String sessionKey);

    WechatUserInfoResponse updateUserInfo(Long userId, String openid, String nickname, String avatarUrl,
                                          Integer gender, String country, String province, String city, String language) throws Exception;

    /**
     * 上传头像文件并更新用户信息（用于微信新版 chooseAvatar）
     */
    WechatUserInfoResponse uploadAvatarFile(MultipartFile file, Long userId, String nickname) throws Exception;

    /**
     * 获取用户头像图片字节数据（从 OSS 下载）
     */
    byte[] getAvatarImage(Long userId);
}
