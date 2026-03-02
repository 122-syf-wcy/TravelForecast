package com.travel.service.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.content.Announcement;

import java.util.List;

/**
 * 实时公告服务接口
 */
public interface AnnouncementService {
    
    /**
     * 获取商家的公告列表（分页）
     */
    Page<Announcement> getMerchantAnnouncements(Long merchantId, int page, int size);
    
    /**
     * 获取景区的公告列表（用户端）
     */
    List<Announcement> getScenicAnnouncements(Long scenicId);
    
    /**
     * 创建公告
     */
    void createAnnouncement(Announcement announcement);
    
    /**
     * 更新公告
     */
    void updateAnnouncement(Announcement announcement);
    
    /**
     * 删除公告
     */
    void deleteAnnouncement(Long id, Long merchantId);
    
    /**
     * 获取公告详情
     */
    Announcement getAnnouncementById(Long id);
}

