package com.travel.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.travel.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OSS oss;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.publicDomain}")
    private String publicDomain;

    @Override
    public String uploadAvatarFromUrl(String avatarUrl, Long userId) throws Exception {
        if (avatarUrl == null || avatarUrl.isBlank()) return avatarUrl;
        if (publicDomain != null && avatarUrl.startsWith(publicDomain)) return avatarUrl;

        String suffix = extractSuffix(avatarUrl);
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        String objectName = String.format("mp/avatar/%s/%s/avatar_%d%s",
            userId == null ? "guest" : String.valueOf(userId), yyyyMM, ts, suffix);

        URL url = new URL(avatarUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);

        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = connection.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = guessContentType(avatarUrl);
        }
        metadata.setContentType(contentType);

        int length = connection.getContentLength();
        if (length > 0) {
            metadata.setContentLength(length);
        }
        metadata.setContentDisposition("inline");

        try (InputStream inputStream = connection.getInputStream()) {
            oss.putObject(bucket, objectName, inputStream, metadata);
        }

        // 尝试设置公共读，如果 Bucket 开启了「阻止公共访问」或 RAM 无权限则会失败，不影响上传
        try {
            oss.setObjectAcl(bucket, objectName, CannedAccessControlList.PublicRead);
        } catch (Exception e) {
            log.warn("设置对象ACL为公共读失败（将通过签名URL访问）: {}", e.getMessage());
        }
        log.info("头像上传OSS成功: {}", objectName);

        return publicDomain.endsWith("/")
            ? publicDomain + objectName
            : publicDomain + "/" + objectName;
    }

    @Override
    public String uploadAvatarFile(MultipartFile file, Long userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        String objectName = String.format("mp/avatar/%s/%s/avatar_%d%s",
                userId == null ? "guest" : String.valueOf(userId), yyyyMM, ts, suffix);

        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = guessContentType(originalFilename != null ? originalFilename : "avatar.jpg");
        }
        metadata.setContentType(contentType);
        metadata.setContentLength(file.getSize());
        metadata.setContentDisposition("inline");

        try (InputStream inputStream = file.getInputStream()) {
            oss.putObject(bucket, objectName, inputStream, metadata);
        }

        try {
            oss.setObjectAcl(bucket, objectName, CannedAccessControlList.PublicRead);
        } catch (Exception e) {
            log.warn("设置对象ACL为公共读失败（将通过代理访问）: {}", e.getMessage());
        }
        log.info("头像文件上传OSS成功: {}", objectName);

        return publicDomain.endsWith("/")
                ? publicDomain + objectName
                : publicDomain + "/" + objectName;
    }

    @Override
    public String toSignedUrl(String ossUrl) {
        if (ossUrl == null || ossUrl.isBlank()) return ossUrl;
        // 不是 OSS 地址，原样返回
        if (publicDomain == null || !ossUrl.startsWith(publicDomain)) return ossUrl;

        // 从完整 URL 提取 objectKey，例如：mp/avatar/24/202602/avatar_xxx.jpg
        String prefix = publicDomain.endsWith("/") ? publicDomain : publicDomain + "/";
        String objectKey = ossUrl.substring(prefix.length());

        // 签名有效期 7 天（与 JWT token 有效期一致）
        Date expiration = new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000);
        try {
            URL signedUrl = oss.generatePresignedUrl(bucket, objectKey, expiration);
            return signedUrl.toString();
        } catch (Exception e) {
            log.warn("生成OSS签名URL失败，返回原地址: {}", e.getMessage());
            return ossUrl;
        }
    }

    private String extractSuffix(String url) {
        int q = url.indexOf('?');
        String clean = q >= 0 ? url.substring(0, q) : url;
        int lastDot = clean.lastIndexOf('.');
        if (lastDot < 0) return ".jpg";
        String suffix = clean.substring(lastDot);
        if (suffix.length() > 6) return ".jpg";
        return suffix;
    }

    @Override
    public byte[] downloadFromOss(String ossUrl) {
        if (ossUrl == null || publicDomain == null || !ossUrl.startsWith(publicDomain)) return null;

        String prefix = publicDomain.endsWith("/") ? publicDomain : publicDomain + "/";
        String objectKey = ossUrl.substring(prefix.length());

        try {
            OSSObject ossObject = oss.getObject(bucket, objectKey);
            try (InputStream is = ossObject.getObjectContent()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("从OSS下载文件失败: key={}, error={}", objectKey, e.getMessage());
            return null;
        }
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
