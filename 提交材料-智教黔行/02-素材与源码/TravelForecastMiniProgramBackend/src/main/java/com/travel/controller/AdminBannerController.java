package com.travel.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.Banner;
import com.travel.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理员端 - 轮播图管理
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerMapper bannerMapper;
    private final OSS ossClient;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.publicDomain}")
    private String publicDomain;

    /**
     * 查询全部轮播图（含未启用）
     */
    @GetMapping
    public Result<List<Banner>> list() {
        LambdaQueryWrapper<Banner> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(Banner::getSort);
        return Result.success(bannerMapper.selectList(qw));
    }

    /**
     * 新增轮播图
     */
    @PostMapping
    public Result<Banner> create(@RequestBody Banner banner) {
        if (banner.getSort() == null) banner.setSort(0);
        if (banner.getEnabled() == null) banner.setEnabled(true);
        bannerMapper.insert(banner);
        return Result.success(banner, "新增成功");
    }

    /**
     * 修改轮播图
     */
    @PutMapping("/{id}")
    public Result<Banner> update(@PathVariable Long id, @RequestBody Banner banner) {
        Banner exist = bannerMapper.selectById(id);
        if (exist == null) return Result.error("轮播图不存在");
        banner.setId(id);
        bannerMapper.updateById(banner);
        return Result.success(banner, "修改成功");
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        bannerMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 启用/禁用轮播图
     */
    @PutMapping("/{id}/toggle")
    public Result<String> toggle(@PathVariable Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) return Result.error("轮播图不存在");
        banner.setEnabled(!banner.getEnabled());
        bannerMapper.updateById(banner);
        return Result.success(banner.getEnabled() ? "已启用" : "已禁用");
    }

    /**
     * 上传轮播图图片到阿里云OSS
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件不能为空");
        try {
            String ext = getExtension(file.getOriginalFilename());
            String datePath = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
            String objectName = String.format("mp/banner/%s/%d%s", datePath, System.currentTimeMillis(), ext);

            try (InputStream is = file.getInputStream()) {
                ossClient.putObject(bucket, objectName, is);
            }
            try {
                ossClient.setObjectAcl(bucket, objectName, CannedAccessControlList.PublicRead);
            } catch (Exception ex) {
                log.warn("设置对象ACL为公共读失败: {}", ex.getMessage());
            }

            String url = publicDomain.endsWith("/")
                    ? publicDomain + objectName
                    : publicDomain + "/" + objectName;
            log.info("轮播图上传OSS成功: {}", objectName);
            return Result.success(url, "上传成功");
        } catch (Exception e) {
            log.error("轮播图上传失败: {}", e.getMessage());
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量修复已有轮播图的OSS公共读权限
     */
    @PostMapping("/fix-acl")
    public Result<String> fixAcl() {
        List<Banner> all = bannerMapper.selectList(null);
        String domain = publicDomain.endsWith("/") ? publicDomain : publicDomain + "/";
        int fixed = 0;
        for (Banner b : all) {
            String img = b.getImage();
            if (img == null || !img.startsWith(domain)) continue;
            String objectName = img.substring(domain.length());
            try {
                ossClient.setObjectAcl(bucket, objectName, CannedAccessControlList.PublicRead);
                fixed++;
                log.info("修复Banner ACL: {}", objectName);
            } catch (Exception e) {
                log.warn("修复Banner ACL失败 {}: {}", objectName, e.getMessage());
            }
        }
        return Result.success("已修复 " + fixed + " 张轮播图的访问权限");
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : ".jpg";
    }
}
