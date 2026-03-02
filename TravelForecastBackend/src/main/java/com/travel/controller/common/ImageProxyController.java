package com.travel.controller.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 图片代理控制器
 * 用于在本地开发环境下提供图片访问
 * 支持：
 * 1. /images/{filename} - 本地图片
 * 2. /images/proxy?url=... - 代理远程OSS URL
 */
@Slf4j
@RestController
@RequestMapping("/images")
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        org.springframework.web.bind.annotation.RequestMethod.GET,
        org.springframework.web.bind.annotation.RequestMethod.OPTIONS
}, maxAge = 3600)
public class ImageProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 代理远程OSS URL
     * 使用示例: /images/proxy?url=https://smarttourism717.oss-cn-beijing.aliyuncs.com/...
     */
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            log.info("📥 代理图片请求 - 原始URL: {}", url);
            
            // 验证URL是否来自允许的源
            if (!url.contains("aliyuncs.com") && !url.startsWith("http")) {
                log.warn("⚠️ 不允许的URL源: {}", url);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // OSS签名URL问题分析：
            // 1. OSS生成的签名URL中，Signature参数值是URL编码的（%2B代表+，%3D代表=）
            // 2. 前端用encodeURIComponent编码整个URL，%2B变成%252B
            // 3. Spring @RequestParam自动解码一次，%252B变回%2B
            // 4. 现在URL中的%2B需要被解码为+才能正确发送给OSS
            // 
            // 解决方案：对URL进行一次解码，让%2B变成+，然后发送请求
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            log.info("🔓 解码后的OSS URL: {}", decodedUrl);
            
            java.net.URL ossUrl = new java.net.URL(decodedUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) ossUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            // 设置User-Agent，有些OSS配置可能需要
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            log.info("📡 OSS响应状态码: {}", responseCode);
            
            if (responseCode != 200) {
                // 读取错误信息
                String errorMsg = "";
                try (java.io.InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        errorMsg = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } catch (Exception ignored) {}
                log.warn("❌ OSS返回非200状态码: {}, URL: {}, 错误: {}", responseCode, decodedUrl, errorMsg);
                return ResponseEntity.status(HttpStatus.valueOf(responseCode)).build();
            }
            
            // 读取图片数据
            byte[] imageData;
            try (java.io.InputStream inputStream = connection.getInputStream();
                 java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                imageData = outputStream.toByteArray();
            } finally {
                connection.disconnect();
            }
            if (imageData == null || imageData.length == 0) {
                log.warn("❌ 从OSS获取图片失败，返回空数据: {}", url);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            String contentType = getContentTypeFromUrl(url);
            if (contentType != null) {
                headers.setContentType(MediaType.parseMediaType(contentType));
            }
            // 缓存设置
            headers.setCacheControl("public, max-age=86400");
            headers.setContentLength(imageData.length);
            
            // 添加 CORS 头
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "*");
            
            log.info("✅ 成功代理图片，大小: {} bytes, Content-Type: {}", imageData.length, contentType);
            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("❌ 代理图片失败: {}, 错误: {}", url, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 处理特定格式的图片请求
     * 支持路径：/images/xxx.jpg -> public/images/xxx.jpg
     * 如果本地找不到，尝试从OSS获取
     */
    @GetMapping("/{subpath:.+}")
    public ResponseEntity<?> getImage(@PathVariable String subpath) {
        try {
            // 解码 URL 编码的路径
            String decodedPath = URLDecoder.decode(subpath, StandardCharsets.UTF_8.name());
            
            // 跳过 proxy 请求（已由上面的方法处理）
            if (decodedPath.startsWith("proxy")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            log.info("请求图片: {}", decodedPath);
            
            // 尝试从多个位置查找本地图片
            File imageFile = findImageFile(decodedPath);
            
            if (imageFile != null && imageFile.exists() && imageFile.isFile()) {
                log.info("成功从本地加载图片: {}", imageFile.getAbsolutePath());
                Resource resource = new FileSystemResource(imageFile);
                
                HttpHeaders headers = new HttpHeaders();
                String contentType = getContentType(decodedPath);
                if (contentType != null) {
                    headers.setContentType(MediaType.parseMediaType(contentType));
                }
                // 添加缓存控制头
                headers.setCacheControl("public, max-age=86400");
                
                // 添加 CORS 头
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "*");
                
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            }
            
            // 本地找不到，尝试从OSS获取
            log.info("本地图片未找到，尝试从OSS获取: {}", decodedPath);
            String ossUrl = "https://smarttourism717.oss-cn-beijing.aliyuncs.com/scenic" + 
                           (decodedPath.startsWith("/") ? decodedPath : "/" + decodedPath);
            
            try {
                byte[] imageData = restTemplate.getForObject(ossUrl, byte[].class);
                if (imageData != null && imageData.length > 0) {
                    log.info("成功从OSS获取图片，大小: {} bytes, URL: {}", imageData.length, ossUrl);
                    
                    HttpHeaders headers = new HttpHeaders();
                    String contentType = getContentType(decodedPath);
                    if (contentType != null) {
                        headers.setContentType(MediaType.parseMediaType(contentType));
                    }
                    headers.setCacheControl("public, max-age=86400");
                    headers.setContentLength(imageData.length);
                    headers.add("Access-Control-Allow-Origin", "*");
                    headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
                    headers.add("Access-Control-Allow-Headers", "*");
                    
                    return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
                }
            } catch (Exception ossError) {
                log.warn("从OSS获取图片失败: {}, 错误: {}", ossUrl, ossError.getMessage());
            }
            
            log.warn("图片未找到: {}", decodedPath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("获取图片失败: {}", subpath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 查找图片文件
     * 按优先级搜索多个潜在位置
     */
    private File findImageFile(String filename) {
        // 清理文件名，防止路径遍历攻击
        String cleanFilename = filename.replaceAll("\\.\\.[\\\\/]", "")
                                       .replaceAll("^[\\\\/]+", "");
        
        // 多个潜在的图片位置，按优先级排列
        String[] possiblePaths = {
                // 1. 项目根目录下的 web/public/images/
                "./web/public/images/" + cleanFilename,
                "./web/public/images" + cleanFilename,
                
                // 2. public/images/
                "./public/images/" + cleanFilename,
                "./public/images" + cleanFilename,
                "public/images/" + cleanFilename,
                "public/images" + cleanFilename,
                
                // 3. 项目根目录下的 images/
                "./images/" + cleanFilename,
                "./images" + cleanFilename,
                
                // 4. Maven 目标目录
                "target/classes/static/images/" + cleanFilename,
                "target/classes/static/images" + cleanFilename,
                
                // 5. 绝对路径（Windows 和 Unix 风格）
                "f:\\网页系统开发\\旅游预测\\web\\public\\images\\" + cleanFilename,
                "/home/user/travel-prediction/web/public/images/" + cleanFilename
        };
        
        for (String path : possiblePaths) {
            try {
                File file = new File(path);
                if (file.exists() && file.isFile() && file.canRead()) {
                    log.debug("在 {} 找到图片文件", file.getAbsolutePath());
                    return file;
                }
            } catch (Exception e) {
                log.debug("检查路径失败: {}, 原因: {}", path, e.getMessage());
            }
        }
        
        log.warn("无法在任何位置找到图片: {}", cleanFilename);
        return null;
    }

    /**
     * 根据文件扩展名获取 Content-Type
     */
    private String getContentType(String filename) {
        if (filename == null) {
            return null;
        }
        
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerName.endsWith(".ico")) {
            return "image/x-icon";
        }
        
        return "application/octet-stream";
    }

    /**
     * 根据 URL 获取 Content-Type
     */
    private String getContentTypeFromUrl(String url) {
        if (url == null) {
            return null;
        }
        
        // 移除查询参数
        String urlWithoutParams = url.split("\\?")[0];
        return getContentType(urlWithoutParams);
    }
}
