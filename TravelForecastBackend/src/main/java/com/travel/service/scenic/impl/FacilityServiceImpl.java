package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Facility;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.mapper.scenic.FacilityMapper;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.service.scenic.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 设施服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityServiceImpl implements FacilityService {
    
    private final FacilityMapper facilityMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    
    @Override
    public Page<Facility> getMerchantFacilities(Long merchantId, int page, int size, String status, String category) {
        log.info("查询商家设施列表: merchantId={}, page={}, size={}, status={}, category={}", 
                merchantId, page, size, status, category);
        
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        Page<Facility> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Facility::getMerchantId, merchantId)
               .eq(Facility::getScenicId, profile.getScenicId())
               .orderByDesc(Facility::getCreatedAt);
        
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(Facility::getStatus, status);
        }
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Facility::getCategory, category);
        }
        
        return facilityMapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public Facility getFacilityById(Long id) {
        Facility facility = facilityMapper.selectById(id);
        if (facility == null) {
            throw new RuntimeException("设施不存在");
        }
        return facility;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Facility createFacility(Facility facility, Long merchantId) {
        log.info("创建设施: merchantId={}, name={}", merchantId, facility.getName());
        
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        // 生成设施编码
        String facilityCode = generateFacilityCode(profile.getScenicId());
        
        facility.setFacilityCode(facilityCode);
        facility.setMerchantId(merchantId);
        facility.setScenicId(profile.getScenicId());
        facility.setRating(BigDecimal.ZERO);
        facility.setReviewCount(0);
        
        // 如果没有设置状态，默认为正常
        if (facility.getStatus() == null || facility.getStatus().isEmpty()) {
            facility.setStatus("normal");
        }
        
        // 如果没有设置是否免费，默认为免费
        if (facility.getIsFree() == null) {
            facility.setIsFree(true);
        }
        
        facilityMapper.insert(facility);
        log.info("设施创建成功: id={}, code={}", facility.getId(), facility.getFacilityCode());
        
        return facility;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Facility updateFacility(Long id, Facility facility, Long merchantId) {
        log.info("更新设施: id={}, merchantId={}", id, merchantId);
        
        Facility existing = facilityMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("设施不存在");
        }
        
        if (!existing.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限修改此设施");
        }
        
        // 保留某些字段不被更新
        facility.setId(id);
        facility.setFacilityCode(existing.getFacilityCode());
        facility.setMerchantId(existing.getMerchantId());
        facility.setScenicId(existing.getScenicId());
        facility.setRating(existing.getRating());
        facility.setReviewCount(existing.getReviewCount());
        
        facilityMapper.updateById(facility);
        log.info("设施更新成功: id={}", id);
        
        return facility;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFacility(Long id, Long merchantId) {
        log.info("删除设施: id={}, merchantId={}", id, merchantId);
        
        Facility facility = facilityMapper.selectById(id);
        if (facility == null) {
            throw new RuntimeException("设施不存在");
        }
        
        if (!facility.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限删除此设施");
        }
        
        facilityMapper.deleteById(id);
        log.info("设施删除成功: id={}", id);
    }
    
    @Override
    public Map<String, Object> getFacilityStats(Long merchantId) {
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        // 统计各种状态的设施数量
        LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Facility::getMerchantId, merchantId)
               .eq(Facility::getScenicId, profile.getScenicId());
        
        Long totalCount = facilityMapper.selectCount(wrapper);
        
        Long normalCount = facilityMapper.selectCount(
            new LambdaQueryWrapper<Facility>()
                .eq(Facility::getMerchantId, merchantId)
                .eq(Facility::getScenicId, profile.getScenicId())
                .eq(Facility::getStatus, "normal")
        );
        
        Long maintenanceCount = facilityMapper.selectCount(
            new LambdaQueryWrapper<Facility>()
                .eq(Facility::getMerchantId, merchantId)
                .eq(Facility::getScenicId, profile.getScenicId())
                .eq(Facility::getStatus, "maintenance")
        );
        
        Long closedCount = facilityMapper.selectCount(
            new LambdaQueryWrapper<Facility>()
                .eq(Facility::getMerchantId, merchantId)
                .eq(Facility::getScenicId, profile.getScenicId())
                .eq(Facility::getStatus, "closed")
        );
        
        // 按类别统计
        Map<String, Long> byCategory = new HashMap<>();
        String[] categories = {"停车场", "餐厅", "卫生间", "服务中心", "医疗点", "其他"};
        for (String category : categories) {
            Long count = facilityMapper.selectCount(
                new LambdaQueryWrapper<Facility>()
                    .eq(Facility::getMerchantId, merchantId)
                    .eq(Facility::getScenicId, profile.getScenicId())
                    .eq(Facility::getCategory, category)
            );
            byCategory.put(category, count);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("normal", normalCount);
        stats.put("maintenance", maintenanceCount);
        stats.put("closed", closedCount);
        stats.put("byCategory", byCategory);
        
        return stats;
    }
    
    @Override
    public Page<Facility> getScenicFacilities(Long scenicId, String category, int page, int size) {
        Page<Facility> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(scenicId != null, Facility::getScenicId, scenicId)
               .in(Facility::getStatus, "normal", "active")  // 支持 normal 和 active 两种状态
               .eq(category != null && !category.isEmpty(), Facility::getCategory, category)
               .orderByDesc(Facility::getRating);
        
        Page<Facility> result = facilityMapper.selectPage(pageObj, wrapper);
        
        // 转换图片URL为相对路径
        result.getRecords().forEach(facility -> {
            if (facility.getThumbnail() != null) {
                facility.setThumbnail(convertOssUrlToRelative(facility.getThumbnail()));
            }
        });
        
        return result;
    }
    
    /**
     * 生成设施编码
     */
    private String generateFacilityCode(Long scenicId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "FAC" + scenicId + timestamp;
    }
    
    /**
     * 是否直接使用OSS URL
     * true: 本地开发环境，前端直接访问OSS
     * false: 生产环境，转换为相对路径通过Nginx代理
     */
    @org.springframework.beans.factory.annotation.Value("${app.oss.use-direct-url:true}")
    private boolean useDirectOssUrl;
    
    /**
     * 将OSS完整URL转换为相对路径（用于通过Nginx代理）
     * 
     * 配置说明：
     * - 本地开发：application.yml 中设置 app.oss.use-direct-url=true
     * - 生产环境：application.yml 中设置 app.oss.use-direct-url=false
     */
    private String convertOssUrlToRelative(String ossUrl) {
        if (ossUrl == null || ossUrl.isEmpty()) {
            return ossUrl;
        }
        
        // 如果配置为直接使用OSS URL（本地开发环境），直接返回完整URL
        if (useDirectOssUrl && ossUrl.contains("aliyuncs.com")) {
            return ossUrl;
        }
        
        // 如果已经是相对路径，直接返回
        if (ossUrl.startsWith("/")) {
            return ossUrl;
        }
        
        // 如果是OSS URL，提取相对路径（生产环境通过Nginx代理）
        try {
            // 处理阿里云OSS URL格式
            if (ossUrl.contains("aliyuncs.com")) {
                // 查找 /scenic/images/ 或类似的路径
                if (ossUrl.contains("/scenic/images/")) {
                    String path = ossUrl.substring(ossUrl.indexOf("/scenic/images/") + "/scenic".length());
                    return path;  // 返回 /images/xxx.jpg
                }
                // 如果没有找到标准路径，尝试提取最后一个路径部分
                int lastSlashIdx = ossUrl.lastIndexOf("/");
                if (lastSlashIdx > 0) {
                    String filename = ossUrl.substring(lastSlashIdx);
                    return "/images" + filename;
                }
            }
            
            // 处理其他云存储或完整URL
            if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                // 提取文件名
                int lastSlashIdx = ossUrl.lastIndexOf("/");
                if (lastSlashIdx > 0) {
                    String filename = ossUrl.substring(lastSlashIdx);
                    return "/images" + filename;
                }
            }
        } catch (Exception e) {
            log.warn("转换OSS URL失败: {}", ossUrl, e);
        }
        
        return ossUrl;
    }
}

