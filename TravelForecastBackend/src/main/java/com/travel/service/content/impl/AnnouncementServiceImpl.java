package com.travel.service.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.content.Announcement;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.mapper.content.AnnouncementMapper;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.service.content.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时公告服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {
    
    private final AnnouncementMapper announcementMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    
    @Override
    public Page<Announcement> getMerchantAnnouncements(Long merchantId, int page, int size) {
        log.info("获取商家公告列表: merchantId={}, page={}, size={}", merchantId, page, size);
        
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        Page<Announcement> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getScenicId, profile.getScenicId())
               .eq(Announcement::getMerchantId, merchantId)
               .orderByDesc(Announcement::getPriority)
               .orderByDesc(Announcement::getCreatedAt);
        
        return announcementMapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public List<Announcement> getScenicAnnouncements(Long scenicId) {
        log.info("获取景区公告列表: scenicId={}", scenicId);
        
        LocalDateTime now = LocalDateTime.now();
        
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getScenicId, scenicId)
               .eq(Announcement::getStatus, "active")
               // 开始时间为空或小于等于现在
               .and(w -> w.isNull(Announcement::getStartTime)
                         .or()
                         .le(Announcement::getStartTime, now))
               // 结束时间为空或大于等于现在
               .and(w -> w.isNull(Announcement::getEndTime)
                         .or()
                         .ge(Announcement::getEndTime, now))
               .orderByDesc(Announcement::getPriority)
               .orderByDesc(Announcement::getCreatedAt)
               .last("LIMIT 10"); // 最多返回10条
        
        return announcementMapper.selectList(wrapper);
    }
    
    @Override
    public void createAnnouncement(Announcement announcement) {
        log.info("创建公告: title={}, merchantId={}", announcement.getTitle(), announcement.getMerchantId());
        
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, announcement.getMerchantId())
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        announcement.setScenicId(profile.getScenicId());
        
        // 设置默认值
        if (announcement.getType() == null) {
            announcement.setType("normal");
        }
        if (announcement.getStatus() == null) {
            announcement.setStatus("active");
        }
        if (announcement.getPriority() == null) {
            announcement.setPriority(0);
        }
        
        announcementMapper.insert(announcement);
        log.info("公告创建成功: id={}", announcement.getId());
    }
    
    @Override
    public void updateAnnouncement(Announcement announcement) {
        log.info("更新公告: id={}", announcement.getId());
        
        Announcement existing = announcementMapper.selectById(announcement.getId());
        if (existing == null) {
            throw new RuntimeException("公告不存在");
        }
        
        if (!existing.getMerchantId().equals(announcement.getMerchantId())) {
            throw new RuntimeException("无权限修改此公告");
        }
        
        announcementMapper.updateById(announcement);
        log.info("公告更新成功: id={}", announcement.getId());
    }
    
    @Override
    public void deleteAnnouncement(Long id, Long merchantId) {
        log.info("删除公告: id={}, merchantId={}", id, merchantId);
        
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("公告不存在");
        }
        
        if (!existing.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限删除此公告");
        }
        
        announcementMapper.deleteById(id);
        log.info("公告删除成功: id={}", id);
    }
    
    @Override
    public Announcement getAnnouncementById(Long id) {
        return announcementMapper.selectById(id);
    }
}

