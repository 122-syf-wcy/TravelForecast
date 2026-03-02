package com.travel.controller.common;

import com.aliyun.oss.OSS;
import com.travel.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/oss")
@RequiredArgsConstructor
public class OssController {
    private final OSS oss;

    @Value("${oss.bucket:smarttourism717}")
    private String bucket;

    @Value("${oss.publicDomain:https://smarttourism717.oss-cn-beijing.aliyuncs.com}")
    private String publicDomain;
    // 简单内存限流（按 IP × action 分桶）
    private static final ConcurrentHashMap<String, RateBucket> RATE = new ConcurrentHashMap<>();
    private static final long ONE_MINUTE = 60_000L;
    private static class RateBucket { volatile long windowStart; volatile int count; }
    private static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            int idx = xf.indexOf(',');
            return idx > 0 ? xf.substring(0, idx).trim() : xf.trim();
        }
        return req.getRemoteAddr();
    }
    private static boolean hitLimit(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        RateBucket b = RATE.computeIfAbsent(key, k -> { RateBucket r = new RateBucket(); r.windowStart = now; r.count = 0; return r; });
        synchronized (b) {
            if (now - b.windowStart >= windowMs) { b.windowStart = now; b.count = 0; }
            b.count++;
            return b.count > limit;
        }
    }

    // 通用文件上传接口（活动、设施等）
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<java.util.Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                       @RequestAttribute(value = "userId", required = false) Long userId,
                                       HttpServletRequest request) throws Exception {
        String ip = clientIp(request);
        if (hitLimit("upload:" + ip, 30, ONE_MINUTE)) {
            return Result.error("Too many upload requests, please retry later.");
        }
        String traceId = request.getHeader("X-Trace-Id");
        log.info("traceId={} 通用文件上传 start, filename={}, userId={}, ip={}", traceId, file.getOriginalFilename(), userId, ip);
        
        String suffix = extractSuffix(file.getOriginalFilename());
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        String objectName = String.format("files/%s/%s/%d%s", userId != null ? userId : "public", yyyyMM, ts, suffix);
        
        log.info("traceId={} objectName={}", traceId, objectName);
        
        // 设置正确的 Content-Type
        com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
        String ct = guessContentTypeBySuffix(suffix);
        metadata.setContentType(ct);
        metadata.setContentDisposition("inline");
        oss.putObject(bucket, objectName, file.getInputStream(), metadata);
        
        log.info("traceId={} putObject ok, bucket={}", traceId, bucket);
        String url = publicDomain.endsWith("/") ? publicDomain + objectName : publicDomain + "/" + objectName;
        log.info("traceId={} publicUrl={}", traceId, url);
        
        java.util.Map<String, String> resp = new java.util.HashMap<>();
        resp.put("url", url);
        resp.put("objectName", objectName);
        
        log.info("traceId={} upload response prepared", traceId);
        return Result.success(resp);
    }
    
    // 上传轮播图：文件夹按模块归类 objectName = banners/yyyyMM/排序_标题_时间戳.jpg
    @PostMapping(value = "/upload/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<java.util.Map<String, String>> uploadBanner(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "title", required = false) String title,
                                       @RequestParam(value = "sort", required = false) Integer sort,
                                       @RequestAttribute(value = "role", required = false) String role,
                                       HttpServletRequest request) throws Exception {
        if (role == null || !"admin".equalsIgnoreCase(role)) {
            return Result.error("forbidden");
        }
        String ip = clientIp(request);
        if (hitLimit("upload:" + ip, 30, ONE_MINUTE)) {
            return Result.error("Too many upload requests, please retry later.");
        }
        String traceId = request.getHeader("X-Trace-Id");
        log.info("traceId={} 上传Banner start, filename={}, title={}, sort={}, ip={}", traceId, file.getOriginalFilename(), title, sort, ip);
        String suffix = extractSuffix(file.getOriginalFilename());
        String safeTitle = title == null ? "banner" : URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+","_");
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        String objectName = String.format("banners/%s/%s_%s_%d%s", yyyyMM, sort == null?"0":sort.toString(), safeTitle, ts, suffix);
        log.info("traceId={} objectName={}", traceId, objectName);
        // 设置正确的 Content-Type，避免浏览器下载而不是显示
        com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
        String ct = guessContentTypeBySuffix(suffix);
        metadata.setContentType(ct);
        metadata.setContentDisposition("inline");
        oss.putObject(bucket, objectName, file.getInputStream(), metadata);
        log.info("traceId={} putObject ok, bucket={}", traceId, bucket);
        String url = publicDomain.endsWith("/") ? publicDomain + objectName : publicDomain + "/" + objectName;
        log.info("traceId={} publicUrl={}", traceId, url);
        java.util.Date expire = new java.util.Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7);
        java.net.URL signed = oss.generatePresignedUrl(bucket, objectName, expire);
        log.info("traceId={} signedUrl generated", traceId);
        java.util.Map<String, String> resp = new java.util.HashMap<>();
        resp.put("url", url);
        resp.put("signedUrl", signed.toString());
        resp.put("objectName", objectName);
        log.info("traceId={} upload response prepared", traceId);
        return Result.success(resp);
    }

    // 根据对象名或完整URL生成临时签名地址，便于私有桶预览
    @GetMapping("/sign")
    public Result<String> sign(@RequestParam(value = "object", required = false) String object,
                               @RequestParam(value = "url", required = false) String url) {
        String key = object;
        if ((key == null || key.isEmpty()) && url != null) {
            try {
                String u = java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8);
                int idx = u.indexOf(".aliyuncs.com/");
                if (idx > 0) {
                    key = u.substring(idx + ".aliyuncs.com/".length() + u.substring(0, idx).lastIndexOf('/') - 0);
                } else {
                    // 若传入的就是以域名开头的我们自己的 publicDomain
                    int i2 = u.indexOf(publicDomain);
                    if (i2 >= 0) key = u.substring(publicDomain.length() + (publicDomain.endsWith("/") ? 0 : 1));
                }
            } catch (Exception ignored) {}
        }
        if (key == null || key.isEmpty()) return Result.error("object or url required");
        java.util.Date expire = new java.util.Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24);
        java.net.URL signed = oss.generatePresignedUrl(bucket, key, expire);
        return Result.success(signed.toString());
    }

    // 反向代理输出图片，绕过浏览器 Referer 防盗链限制
    @GetMapping("/proxy")
    public void proxy(@RequestParam(value = "object", required = false) String object,
                      @RequestParam(value = "url", required = false) String url,
                      HttpServletResponse response,
                      HttpServletRequest request) {
        try {
            String ip = clientIp(request);
            if (hitLimit("proxy:" + ip, 120, ONE_MINUTE)) { response.setStatus(429); return; }
            String traceId = request.getHeader("X-Trace-Id");
            String key = object;
            if ((key == null || key.isEmpty()) && url != null) {
                String u = java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8);
                int idx = u.indexOf(".aliyuncs.com/");
                if (idx > 0) {
                    key = u.substring(idx + ".aliyuncs.com/".length());
                    int q = key.indexOf('?');
                    if (q > 0) key = key.substring(0, q);
                }
            }
            if (key == null || key.isEmpty()) {
                response.setStatus(400);
                return;
            }

            com.aliyun.oss.model.OSSObject obj = oss.getObject(bucket, key);
            String contentType = obj.getObjectMetadata().getContentType();
            if (contentType == null || contentType.isEmpty()) contentType = guessContentTypeBySuffix(key);
            response.setHeader("Cache-Control", "public, max-age=604800"); // 7d
            response.setContentType(contentType);
            try (java.io.InputStream in = obj.getObjectContent();
                 java.io.OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }
        } catch (Exception e) {
            String traceId = request.getHeader("X-Trace-Id");
            log.warn("traceId={} OSS 图片代理失败: object={}, url={}, err={}", traceId, object, url, e.getMessage());
            try { response.setStatus(404); } catch (Exception ignored) {}
        }
    }

    private static String extractSuffix(String name) {
        if (name == null) return ".jpg";
        int i = name.lastIndexOf('.');
        if (i < 0) return ".jpg";
        return name.substring(i);
    }

    private static String guessContentTypeBySuffix(String suffix) {
        String s = suffix == null ? "" : suffix.toLowerCase();
        if (s.endsWith(".jpg") || s.endsWith(".jpeg")) return "image/jpeg";
        if (s.endsWith(".png")) return "image/png";
        if (s.endsWith(".webp")) return "image/webp";
        if (s.endsWith(".gif")) return "image/gif";
        if (s.endsWith(".mp4")) return "video/mp4";
        if (s.endsWith(".webm")) return "video/webm";
        if (s.endsWith(".mov")) return "video/quicktime";
        if (s.endsWith(".avi")) return "video/x-msvideo";
        return "application/octet-stream";
    }
    
    // 上传实景预览（图片或视频）
    @PostMapping(value = "/upload/showcase", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<java.util.Map<String, String>> uploadShowcase(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "type", required = false) String type,
                                       @RequestAttribute(value = "role", required = false) String role,
                                       HttpServletRequest request) throws Exception {
        if (role == null || !"admin".equalsIgnoreCase(role)) {
            return Result.error("forbidden");
        }
        String ip = clientIp(request);
        if (hitLimit("upload:" + ip, 30, ONE_MINUTE)) {
            return Result.error("Too many upload requests, please retry later.");
        }
        String traceId = request.getHeader("X-Trace-Id");
        log.info("traceId={} 上传Showcase start, filename={}, type={}, ip={}", traceId, file.getOriginalFilename(), type, ip);
        
        String suffix = extractSuffix(file.getOriginalFilename());
        String yyyyMM = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        long ts = System.currentTimeMillis();
        String folder = "video".equals(type) ? "showcases/videos" : "showcases/images";
        String objectName = String.format("%s/%s/%d%s", folder, yyyyMM, ts, suffix);
        
        log.info("traceId={} objectName={}", traceId, objectName);
        
        com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
        String ct = guessContentTypeBySuffix(suffix);
        metadata.setContentType(ct);
        metadata.setContentDisposition("inline");
        oss.putObject(bucket, objectName, file.getInputStream(), metadata);
        
        log.info("traceId={} putObject ok, bucket={}", traceId, bucket);
        String url = publicDomain.endsWith("/") ? publicDomain + objectName : publicDomain + "/" + objectName;
        log.info("traceId={} publicUrl={}", traceId, url);
        
        java.util.Date expire = new java.util.Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7);
        java.net.URL signed = oss.generatePresignedUrl(bucket, objectName, expire);
        
        java.util.Map<String, String> resp = new java.util.HashMap<>();
        resp.put("url", url);
        resp.put("signedUrl", signed.toString());
        resp.put("objectName", objectName);
        
        log.info("traceId={} showcase upload response prepared", traceId);
        return Result.success(resp);
    }
}


