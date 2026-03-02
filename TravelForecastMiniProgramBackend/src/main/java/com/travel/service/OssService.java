package com.travel.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {
    String uploadAvatarFromUrl(String avatarUrl, Long userId) throws Exception;

    /**
     * 上传头像文件到 OSS（接收前端直接上传的文件）
     * @param file 头像文件
     * @param userId 用户ID
     * @return OSS 存储地址
     */
    String uploadAvatarFile(MultipartFile file, Long userId) throws Exception;

    /**
     * 将 OSS 存储地址转为带签名的可访问 URL
     */
    String toSignedUrl(String ossUrl);

    /**
     * 从 OSS 下载文件内容（通过 SDK 内部鉴权，绕过外部访问限制）
     * @return 文件字节数组，如果下载失败返回 null
     */
    byte[] downloadFromOss(String ossUrl);
}
