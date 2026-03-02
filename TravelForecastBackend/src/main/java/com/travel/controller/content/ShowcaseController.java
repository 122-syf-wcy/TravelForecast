package com.travel.controller.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.content.Showcase;
import com.travel.mapper.content.ShowcaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.aliyun.oss.OSS;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实景预览管理控制器（支持图片和视频）
 */
@Slf4j
@RestController
@RequestMapping("/content/showcases")
@RequiredArgsConstructor
public class ShowcaseController {
    private final ShowcaseMapper showcaseMapper;
    private final OSS oss;
    
    @Value("${oss.bucket:smarttourism717}")
    private String bucket;
    
    @Value("${oss.publicDomain:https://smarttourism717.oss-cn-beijing.aliyuncs.com}")
    private String publicDomain;

    /**
     * 从 URL 中提取 OSS 对象 Key
     */
    private String extractObjectKey(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            String key = url;
            if (url.startsWith("http")) {
                int idx = url.indexOf(".aliyuncs.com/");
                if (idx > 0) {
                    key = url.substring(idx + ".aliyuncs.com/".length());
                } else if (url.startsWith(publicDomain + "/")) {
                    key = url.substring(publicDomain.length() + 1);
                }
            }
            int queryIndex = key.indexOf("?");
            if (queryIndex > 0) {
                key = key.substring(0, queryIndex);
            }
            if (key.contains("%25")) {
                key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
            }
            return key;
        } catch (Exception e) {
            log.warn("提取 objectKey 失败: {}", url, e);
            return null;
        }
    }

    /**
     * 为 URL 生成签名
     */
    private String signIfNeeded(String url) {
        if (url == null || url.isEmpty()) return url;
        try {
            String key = extractObjectKey(url);
            if (key == null) return url;
            java.util.Date expire = new java.util.Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7);
            java.net.URL signed = oss.generatePresignedUrl(bucket, key, expire);
            return signed.toString();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 删除 OSS 文件
     */
    private void deleteOssFile(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            String objectKey = extractObjectKey(url);
            if (objectKey != null && !objectKey.isEmpty()) {
                if (oss.doesObjectExist(bucket, objectKey)) {
                    oss.deleteObject(bucket, objectKey);
                    log.info("✓ 删除 OSS 文件: {}", objectKey);
                }
            }
        } catch (Exception e) {
            log.error("删除 OSS 文件失败: {}", url, e);
        }
    }

    /**
     * 复制实景对象并签名 URL
     */
    private Showcase copyWithSignedUrl(Showcase s) {
        Showcase copy = new Showcase();
        copy.setId(s.getId());
        copy.setTitle(s.getTitle());
        copy.setType(s.getType());
        copy.setUrl(signIfNeeded(s.getUrl()));
        copy.setCover(signIfNeeded(s.getCover()));
        copy.setDescription(s.getDescription());
        copy.setSort(s.getSort());
        copy.setEnabled(s.getEnabled());
        copy.setCreatedAt(s.getCreatedAt());
        copy.setUpdatedAt(s.getUpdatedAt());
        return copy;
    }

    /**
     * 公开接口：获取启用的实景预览列表
     */
    @GetMapping("/public")
    public Result<List<Showcase>> listPublic() {
        List<Showcase> list = showcaseMapper.selectList(new LambdaQueryWrapper<Showcase>()
                .eq(Showcase::getEnabled, true)
                .orderByAsc(Showcase::getSort)
                .orderByAsc(Showcase::getId));
        List<Showcase> out = list.stream().map(this::copyWithSignedUrl).collect(Collectors.toList());
        return Result.success(out);
    }

    /**
     * 管理端：分页获取实景列表
     */
    @GetMapping
    public Result<Page<Showcase>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        Page<Showcase> p = showcaseMapper.selectPage(new Page<>(page, size), 
                new LambdaQueryWrapper<Showcase>()
                        .orderByAsc(Showcase::getSort)
                        .orderByAsc(Showcase::getId));
        List<Showcase> signedRecords = p.getRecords().stream()
                .map(this::copyWithSignedUrl)
                .collect(Collectors.toList());
        p.setRecords(signedRecords);
        log.info("traceId={} list showcases, count={}", traceId, p.getRecords().size());
        return Result.success(p);
    }

    /**
     * 获取单个实景详情
     */
    @GetMapping("/{id}")
    public Result<Showcase> detail(@PathVariable Long id) {
        Showcase s = showcaseMapper.selectById(id);
        if (s == null) return Result.success(null);
        return Result.success(copyWithSignedUrl(s));
    }

    /**
     * 新增实景
     */
    @PostMapping
    public Result<Showcase> create(@RequestBody Showcase s, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (s.getEnabled() == null) s.setEnabled(true);
        if (s.getType() == null) s.setType("image");
        if (s.getSort() == null) s.setSort(1);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        showcaseMapper.insert(s);
        log.info("traceId={} create showcase id={}", traceId, s.getId());
        return Result.success(s);
    }

    /**
     * 更新实景
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Showcase s, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        Showcase old = showcaseMapper.selectById(id);
        
        if (old != null) {
            // 检查媒体 URL 是否更换
            String oldKey = extractObjectKey(old.getUrl());
            String newKey = extractObjectKey(s.getUrl());
            if (newKey != null && oldKey != null && !newKey.equals(oldKey)) {
                deleteOssFile(old.getUrl());
                log.info("更新 Showcase[{}] 时删除旧媒体: {}", id, oldKey);
            }
            
            // 检查封面是否更换
            String oldCoverKey = extractObjectKey(old.getCover());
            String newCoverKey = extractObjectKey(s.getCover());
            if (newCoverKey != null && oldCoverKey != null && !newCoverKey.equals(oldCoverKey)) {
                deleteOssFile(old.getCover());
                log.info("更新 Showcase[{}] 时删除旧封面: {}", id, oldCoverKey);
            }
        }
        
        s.setId(id);
        s.setUpdatedAt(LocalDateTime.now());
        showcaseMapper.updateById(s);
        log.info("traceId={} update showcase id={}", traceId, id);
        return Result.success();
    }

    /**
     * 删除实景
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        Showcase s = showcaseMapper.selectById(id);
        
        if (s != null) {
            // 删除媒体文件
            if (s.getUrl() != null && !s.getUrl().isEmpty()) {
                deleteOssFile(s.getUrl());
            }
            // 删除封面文件
            if (s.getCover() != null && !s.getCover().isEmpty()) {
                deleteOssFile(s.getCover());
            }
        }
        
        showcaseMapper.deleteById(id);
        log.info("traceId={} delete showcase id={}", traceId, id);
        return Result.success();
    }
}
