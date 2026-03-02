package com.travel.controller.common;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.travel.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传Controller
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {
    
    private final OSS ossClient;
    
    @Value("${oss.bucket:travel-prediction}")
    private String bucketName;
    
    @Value("${oss.domain:https://travel-prediction.oss-cn-beijing.aliyuncs.com}")
    private String ossDomain;
    
    /**
     * 上传头像
     * POST /upload/avatar
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        
        if (userId == null) {
            return Result.error("未登录");
        }
        
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }
        
        // 验证文件大小（2MB）
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("文件大小不能超过2MB");
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
            
            // 生成唯一文件名：avatars/userId/uuid.jpg
            String objectName = String.format("avatars/%d/%s%s", userId, UUID.randomUUID(), suffix);
            
            log.info("上传头像: userId={}, filename={}, size={}", userId, originalFilename, file.getSize());
            
            // 上传到OSS
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
                ossClient.putObject(putObjectRequest);
            }
            
            // 生成访问URL
            String url = ossDomain + "/" + objectName;
            
            log.info("头像上传成功: userId={}, url={}", userId, url);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", originalFilename);
            
            return Result.success(result, "上传成功");
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("OSS上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传通用文件
     * POST /upload/file
     */
    @PostMapping("/file")
    public Result<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "files") String folder,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        
        if (userId == null) {
            return Result.error("未登录");
        }
        
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        // 验证文件大小（10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("文件大小不能超过10MB");
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            
            // 生成唯一文件名
            String objectName = String.format("%s/%d/%s%s", folder, userId, UUID.randomUUID(), suffix);
            
            log.info("上传文件: userId={}, filename={}, size={}", userId, originalFilename, file.getSize());
            
            // 上传到OSS
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
                ossClient.putObject(putObjectRequest);
            }
            
            // 生成访问URL
            String url = ossDomain + "/" + objectName;
            
            log.info("文件上传成功: userId={}, url={}", userId, url);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", originalFilename);
            
            return Result.success(result, "上传成功");
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("OSS上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}

