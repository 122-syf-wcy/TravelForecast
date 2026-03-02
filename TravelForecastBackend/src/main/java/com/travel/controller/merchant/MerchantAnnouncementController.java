package com.travel.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.content.Announcement;
import com.travel.service.content.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家端实时公告控制器
 */
@Slf4j
@RestController
@RequestMapping("/merchant/announcements")
@RequiredArgsConstructor
public class MerchantAnnouncementController {
    
    private final AnnouncementService announcementService;
    
    /**
     * 获取商家的公告列表
     */
    @GetMapping
    public Map<String, Object> getMerchantAnnouncements(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("商家查询公告列表: merchantId={}, page={}, size={}", userId, page, size);
        
        Page<Announcement> result = announcementService.getMerchantAnnouncements(userId, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("announcements", result.getRecords());
        response.put("total", result.getTotal());
        response.put("currentPage", result.getCurrent());
        response.put("pageSize", result.getSize());
        
        return response;
    }
    
    /**
     * 创建公告
     */
    @PostMapping
    public Map<String, Object> createAnnouncement(
            @RequestAttribute("userId") Long userId,
            @RequestBody Announcement announcement) {
        
        log.info("商家创建公告: merchantId={}, title={}", userId, announcement.getTitle());
        
        announcement.setMerchantId(userId);
        announcementService.createAnnouncement(announcement);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "公告创建成功");
        response.put("id", announcement.getId());
        
        return response;
    }
    
    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateAnnouncement(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody Announcement announcement) {
        
        log.info("商家更新公告: merchantId={}, id={}", userId, id);
        
        announcement.setId(id);
        announcement.setMerchantId(userId);
        announcementService.updateAnnouncement(announcement);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "公告更新成功");
        
        return response;
    }
    
    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteAnnouncement(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        
        log.info("商家删除公告: merchantId={}, id={}", userId, id);
        
        announcementService.deleteAnnouncement(id, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "公告删除成功");
        
        return response;
    }
    
    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public Announcement getAnnouncement(@PathVariable Long id) {
        log.info("查询公告详情: id={}", id);
        return announcementService.getAnnouncementById(id);
    }
}

