package com.travel.service.common.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.travel.service.common.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * OSS文件上传服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OSS oss;

    @Value("${oss.bucket:smarttourism717}")
    private String bucket;

    @Value("${oss.publicDomain:https://smarttourism717.oss-cn-beijing.aliyuncs.com}")
    private String publicDomain;

    @Override
    public Map<String, String> uploadScenicImage(MultipartFile file, Long scenicId, String imageType) throws Exception {
        String suffix = extractSuffix(file.getOriginalFilename());
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        
        // 景区图片路径：scenic/images/{scenicId}/{imageType}/{yyyyMM}/时间戳.jpg
        String objectName = String.format("scenic/images/%d/%s/%s/%d%s", 
                scenicId, imageType.toLowerCase(), yyyyMM, ts, suffix);

        return uploadFile(file, objectName);
    }

    @Override
    public Map<String, String> uploadScenicVideo(MultipartFile file, Long scenicId, String title) throws Exception {
        String suffix = extractSuffix(file.getOriginalFilename());
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        
        // 使用时间戳作为文件名，避免中文编码问题
        // 景区视频路径：scenic/videos/{scenicId}/{yyyyMM}/video_{时间戳}.mp4
        String objectName = String.format("scenic/videos/%d/%s/video_%d%s", 
                scenicId, yyyyMM, ts, suffix);

        return uploadFile(file, objectName);
    }

    @Override
    public Map<String, String> uploadVideoCover(MultipartFile file, Long scenicId) throws Exception {
        String suffix = extractSuffix(file.getOriginalFilename());
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        
        // 视频封面路径：scenic/videos/{scenicId}/covers/{yyyyMM}/时间戳.jpg
        String objectName = String.format("scenic/videos/%d/covers/%s/%d%s", 
                scenicId, yyyyMM, ts, suffix);

        return uploadFile(file, objectName);
    }

    @Override
    public boolean deleteFile(String objectName) {
        try {
            oss.deleteObject(bucket, objectName);
            log.info("删除OSS文件成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("删除OSS文件失败: {}, error: {}", objectName, e.getMessage());
            return false;
        }
    }

    @Override
    public String generateSignedUrl(String objectName, int expireHours) {
        try {
            Date expireDate = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * expireHours);
            URL signedUrl = oss.generatePresignedUrl(bucket, objectName, expireDate);
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("生成签名URL失败: {}, error: {}", objectName, e.getMessage());
            return null;
        }
    }

    @Override
    public String extractObjectNameFromUrl(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            
            // 尝试从阿里云OSS URL中提取
            int idx = decodedUrl.indexOf(".aliyuncs.com/");
            if (idx > 0) {
                String key = decodedUrl.substring(idx + ".aliyuncs.com/".length());
                int queryIdx = key.indexOf('?');
                if (queryIdx > 0) {
                    key = key.substring(0, queryIdx);
                }
                return key;
            }
            
            // 尝试从自定义域名中提取
            if (decodedUrl.startsWith(publicDomain)) {
                String key = decodedUrl.substring(publicDomain.length());
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                int queryIdx = key.indexOf('?');
                if (queryIdx > 0) {
                    key = key.substring(0, queryIdx);
                }
                return key;
            }
            
            return null;
        } catch (Exception e) {
            log.error("提取objectName失败: {}, error: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 通用文件上传方法
     */
    private Map<String, String> uploadFile(MultipartFile file, String objectName) throws Exception {
        log.info("上传文件: filename={}, objectName={}, size={} bytes", 
                file.getOriginalFilename(), objectName, file.getSize());
        log.info("OSS配置: bucket={}, domain={}", bucket, publicDomain);
        
        // 设置元数据
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = guessContentType(file.getOriginalFilename());
        metadata.setContentType(contentType);
        metadata.setContentDisposition("inline");
        metadata.setContentLength(file.getSize());
        
        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            log.info("开始上传到OSS...");
            oss.putObject(bucket, objectName, inputStream, metadata);
            log.info("OSS putObject调用完成");
        } catch (Exception e) {
            log.error("OSS上传失败: {}", e.getMessage(), e);
            throw new Exception("OSS上传失败: " + e.getMessage(), e);
        }
        
        // 验证文件是否存在
        boolean exists = oss.doesObjectExist(bucket, objectName);
        log.info("验证文件是否存在: bucket={}, objectName={}, exists={}", bucket, objectName, exists);
        
        if (!exists) {
            throw new Exception("文件上传后验证失败：OSS上不存在该文件");
        }
        
        log.info("文件上传成功并验证通过: bucket={}, objectName={}", bucket, objectName);
        
        // 生成URL
        String url = publicDomain.endsWith("/") 
                ? publicDomain + objectName 
                : publicDomain + "/" + objectName;
        
        // 生成7天有效期的签名URL
        String signedUrl = generateSignedUrl(objectName, 24 * 7);
        
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("signedUrl", signedUrl);
        result.put("objectName", objectName);
        
        return result;
    }

    /**
     * 提取文件后缀
     */
    private String extractSuffix(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".jpg";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            return ".jpg";
        }
        return filename.substring(lastDot);
    }

    /**
     * 根据文件名猜测ContentType
     */
    private String guessContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        
        String lower = filename.toLowerCase();
        
        // 图片类型
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        
        // 视频类型
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".wmv")) return "video/x-ms-wmv";
        if (lower.endsWith(".flv")) return "video/x-flv";
        if (lower.endsWith(".webm")) return "video/webm";
        
        return "application/octet-stream";
    }
}

