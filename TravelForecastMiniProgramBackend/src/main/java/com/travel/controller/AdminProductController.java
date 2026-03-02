package com.travel.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.Product;
import com.travel.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 商家端 - 文创商品管理
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductMapper productMapper;
    private final OSS ossClient;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.publicDomain}")
    private String publicDomain;

    /**
     * 查询全部商品（含下架）
     */
    @GetMapping
    public Result<List<Product>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            qw.eq(Product::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(Product::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Product::getName, keyword);
        }
        qw.orderByAsc(Product::getSortOrder);
        return Result.success(productMapper.selectList(qw));
    }

    /**
     * 查询单个商品
     */
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) return Result.error("商品不存在");
        return Result.success(p);
    }

    /**
     * 新增商品
     */
    @PostMapping
    public Result<Product> create(@RequestBody Product product) {
        if (product.getStatus() == null) product.setStatus("ACTIVE");
        if (product.getSortOrder() == null) product.setSortOrder(0);
        if (product.getStock() == null) product.setStock(999);
        if (product.getSales() == null) product.setSales(0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return Result.success(product, "新增成功");
    }

    /**
     * 修改商品
     */
    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Product exist = productMapper.selectById(id);
        if (exist == null) return Result.error("商品不存在");
        product.setId(id);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return Result.success(product, "修改成功");
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 上架/下架商品
     */
    @PutMapping("/{id}/toggle")
    public Result<String> toggle(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) return Result.error("商品不存在");
        product.setStatus("ACTIVE".equals(product.getStatus()) ? "INACTIVE" : "ACTIVE");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return Result.success("ACTIVE".equals(product.getStatus()) ? "已上架" : "已下架");
    }

    /**
     * 上传商品图片到阿里云OSS
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件不能为空");
        try {
            String ext = getExtension(file.getOriginalFilename());
            String datePath = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
            String objectName = String.format("mp/product/%s/%d%s", datePath, System.currentTimeMillis(), ext);

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
            log.info("商品图片上传OSS成功: {}", objectName);
            return Result.success(url, "上传成功");
        } catch (Exception e) {
            log.error("商品图片上传失败: {}", e.getMessage());
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : ".jpg";
    }
}
