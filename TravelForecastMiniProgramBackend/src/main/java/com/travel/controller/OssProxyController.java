package com.travel.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

/**
 * OSS 图片代理 —— 后端从 OSS 拉取图片返回给前端，绕过防盗链
 */
@Slf4j
@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
public class OssProxyController {

    private final OSS ossClient;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.publicDomain}")
    private String publicDomain;

    /**
     * 代理访问 OSS 图片
     * GET /api/oss/proxy?key=banners/202601/1_banner_xxx.jpg
     */
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String key) {
        try {
            OSSObject obj = ossClient.getObject(bucket, key);
            try (InputStream is = obj.getObjectContent()) {
                byte[] data = is.readAllBytes();

                HttpHeaders headers = new HttpHeaders();
                String contentType = obj.getObjectMetadata().getContentType();
                if (contentType != null) {
                    headers.setContentType(MediaType.parseMediaType(contentType));
                } else {
                    headers.setContentType(guessMediaType(key));
                }
                headers.setCacheControl("public, max-age=86400");
                headers.setContentLength(data.length);

                return new ResponseEntity<>(data, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.warn("OSS代理失败: key={}, error={}", key, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 将 OSS 完整 URL 转为代理路径的 key
     */
    public String toProxyUrl(String ossUrl) {
        if (ossUrl == null || ossUrl.isBlank()) return ossUrl;
        if (publicDomain == null || !ossUrl.startsWith(publicDomain)) return ossUrl;
        String prefix = publicDomain.endsWith("/") ? publicDomain : publicDomain + "/";
        String key = ossUrl.substring(prefix.length());
        return "/api/oss/proxy?key=" + key;
    }

    private MediaType guessMediaType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
