package com.travel.controller.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.content.Banner;
import com.travel.mapper.content.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.aliyun.oss.OSS;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/content/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerMapper bannerMapper;
    private final OSS oss;
    @Value("${oss.bucket:smarttourism717}")
    private String bucket;
    @Value("${oss.publicDomain:https://smarttourism717.oss-cn-beijing.aliyuncs.com}")
    private String publicDomain;
    private static final String PREFIX = "banners/";

    /**
     * 从 URL 中提取 OSS 对象 Key（去除签名参数，保持 URL 编码）
     */
    private String extractObjectKey(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        try {
            String key = imageUrl;
            
            // 如果是完整的 HTTP URL
            if (imageUrl.startsWith("http")) {
                int idx = imageUrl.indexOf(".aliyuncs.com/");
                if (idx > 0) {
                    key = imageUrl.substring(idx + ".aliyuncs.com/".length());
                } else if (imageUrl.startsWith(publicDomain + "/")) {
                    key = imageUrl.substring(publicDomain.length() + 1);
                } else if (imageUrl.startsWith(publicDomain)) {
                    key = imageUrl.substring(publicDomain.length());
                }
            }
            
            // 去除签名参数（? 后面的部分）
            int queryIndex = key.indexOf("?");
            if (queryIndex > 0) {
                key = key.substring(0, queryIndex);
            }
            
            // ⚠️ 重要：不要 URL 解码！OSS 中的文件名本身就是 URL 编码的
            // 如果 URL 中已经被二次编码（%25），需要解码一次
            if (key.contains("%25")) {
                key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            return key;
        } catch (Exception e) {
            log.warn("提取 objectKey 失败: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 为图片 URL 生成签名（用于私有桶或需要签名访问的场景）
     */
    private String signIfNeeded(String image) {
        if (image == null || image.isEmpty()) return image;
        try {
            String key = extractObjectKey(image);
            if (key == null) return image;
            
            // 生成 7 天有效期的签名 URL
            java.util.Date expire = new java.util.Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7);
            java.net.URL signed = oss.generatePresignedUrl(bucket, key, expire);
            return signed.toString();
        } catch (Exception e) {
            // 如果签名失败，返回原始 URL
            return image;
        }
    }

    /**
     * 删除 OSS 中的图片文件
     */
    private void deleteOssImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            log.warn("deleteOssImage: imageUrl 为空，跳过删除");
            return;
        }
        try {
            String objectKey = extractObjectKey(imageUrl);
            log.info("准备删除 OSS 文件 - 原始URL: {}, 提取的ObjectKey: {}", imageUrl, objectKey);
            
            if (objectKey != null && !objectKey.isEmpty()) {
                // 检查文件是否存在
                boolean exists = oss.doesObjectExist(bucket, objectKey);
                log.info("OSS文件存在性检查 - Bucket: {}, ObjectKey: {}, Exists: {}", bucket, objectKey, exists);
                
                if (exists) {
                    oss.deleteObject(bucket, objectKey);
                    log.info("✓ 成功删除 OSS 文件: {}", objectKey);
                } else {
                    log.warn("✗ OSS 文件不存在，无法删除: {}", objectKey);
                }
            } else {
                log.warn("✗ 提取的 objectKey 为空，无法删除。原始URL: {}", imageUrl);
            }
        } catch (Exception e) {
            log.error("✗ 删除 OSS 文件异常 - URL: {}, 错误: {}", imageUrl, e.getMessage(), e);
            // 删除失败不影响主流程
        }
    }

    // 前台读取：只取启用、按排序
    @GetMapping("/public")
    public Result<List<Banner>> listPublic() {
        List<Banner> list = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getEnabled, true)
                .orderByAsc(Banner::getSort).orderByAsc(Banner::getId));
        // 返回可访问的地址（创建新对象避免修改数据库对象）
        List<Banner> out = list.stream().map(b -> {
            Banner copy = new Banner();
            copy.setId(b.getId());
            copy.setTitle(b.getTitle());
            copy.setImage(signIfNeeded(b.getImage()));
            copy.setLink(b.getLink());
            copy.setSort(b.getSort());
            copy.setEnabled(b.getEnabled());
            return copy;
        }).collect(Collectors.toList());
        return Result.success(out);
    }

    // 管理端列表
    @GetMapping
    public Result<Page<Banner>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size,
                                     HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        Page<Banner> p = bannerMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort).orderByAsc(Banner::getId));
        // 创建新的记录列表，避免修改原始对象
        List<Banner> signedRecords = p.getRecords().stream().map(b -> {
            Banner copy = new Banner();
            copy.setId(b.getId());
            copy.setTitle(b.getTitle());
            copy.setImage(signIfNeeded(b.getImage()));
            copy.setLink(b.getLink());
            copy.setSort(b.getSort());
            copy.setEnabled(b.getEnabled());
            return copy;
        }).collect(Collectors.toList());
        p.setRecords(signedRecords);
        log.info("traceId={} list banners, count={}", traceId, p.getRecords()==null?0:p.getRecords().size());
        return Result.success(p);
    }

    // 详情（可选）
    @GetMapping("/{id}")
    public Result<Banner> detail(@PathVariable Long id, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        Banner b = bannerMapper.selectById(id);
        if (b == null) return Result.success(null);
        Banner copy = new Banner();
        copy.setId(b.getId());
        copy.setTitle(b.getTitle());
        copy.setImage(signIfNeeded(b.getImage()));
        copy.setLink(b.getLink());
        copy.setSort(b.getSort());
        copy.setEnabled(b.getEnabled());
        log.info("traceId={} get banner detail id={}", traceId, id);
        return Result.success(copy);
    }

    /**
     * 清理未被引用的 OSS Banner 文件（仅管理员调用，dryRun 默认 true）
     */
    @GetMapping("/cleanup")
    public Result<Object> cleanup(@RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                                  @RequestAttribute(value = "role", required = false) String role) {
        if (role == null || !"admin".equalsIgnoreCase(role)) {
            return Result.error("forbidden");
        }

        // 1) 收集数据库中引用的对象 key
        List<Banner> all = bannerMapper.selectList(new LambdaQueryWrapper<Banner>());
        Set<String> referenced = new HashSet<>();
        for (Banner b : all) {
            String key = extractObjectKey(b.getImage());
            if (key != null && !key.isEmpty()) referenced.add(key);
        }

        // 2) 遍历 OSS 中的对象
        java.util.List<String> toDelete = new ArrayList<>();
        com.aliyun.oss.model.ListObjectsRequest req = new com.aliyun.oss.model.ListObjectsRequest(bucket)
                .withPrefix(PREFIX).withMaxKeys(500);
        com.aliyun.oss.model.ObjectListing listing;
        do {
            listing = oss.listObjects(req);
            for (com.aliyun.oss.model.OSSObjectSummary s : listing.getObjectSummaries()) {
                String key = s.getKey();
                if (!referenced.contains(key)) {
                    toDelete.add(key);
                }
            }
            req.setMarker(listing.getNextMarker());
        } while (listing.isTruncated());

        // 3) 删除或预览
        if (!dryRun && !toDelete.isEmpty()) {
            for (String key : toDelete) {
                try { oss.deleteObject(bucket, key); } catch (Exception ignore) {}
            }
        }

        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("dryRun", dryRun);
        out.put("referencedCount", referenced.size());
        out.put("orphanCount", toDelete.size());
        out.put("orphans", toDelete);
        return Result.success(out);
    }

    @PostMapping
    public Result<Banner> create(@RequestBody Banner b, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (b.getEnabled() == null) b.setEnabled(true);
        bannerMapper.insert(b);
        log.info("traceId={} create banner id={}", traceId, b.getId());
        return Result.success(b);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Banner b, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        // 查询旧数据，检查图片是否更换
        Banner oldBanner = bannerMapper.selectById(id);
        if (oldBanner != null && oldBanner.getImage() != null && !oldBanner.getImage().isEmpty()) {
            // 提取新旧图片的 objectKey 进行比较（忽略签名参数）
            String oldKey = extractObjectKey(oldBanner.getImage());
            String newKey = extractObjectKey(b.getImage());
            
            // 如果 objectKey 不同，说明图片被替换了，删除旧图片
            if (newKey != null && oldKey != null && !newKey.equals(oldKey)) {
                deleteOssImage(oldBanner.getImage());
                log.info("更新 Banner[{}] 时删除旧图片: {}", id, oldKey);
            }
        }
        
        b.setId(id);
        bannerMapper.updateById(b);
        log.info("traceId={} update banner id={}", traceId, id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        // 查询要删除的 banner，获取图片 URL
        Banner banner = bannerMapper.selectById(id);
        if (banner != null && banner.getImage() != null && !banner.getImage().isEmpty()) {
            String objectKey = extractObjectKey(banner.getImage());
            // 删除 OSS 中的图片
            deleteOssImage(banner.getImage());
            log.info("删除 Banner[{}] 时删除图片: {}", id, objectKey);
        }
        
        // 删除数据库记录
        bannerMapper.deleteById(id);
        log.info("traceId={} delete banner id={}", traceId, id);
        return Result.success();
    }
}


