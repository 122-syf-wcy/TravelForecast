package com.travel.controller.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.content.Announcement;
import com.travel.service.content.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时公告控制器（用户端）
 */
@Slf4j
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    
    private final AnnouncementService announcementService;
    
    /**
     * 获取景区公告列表（用户端）
     */
    @GetMapping
    public Map<String, Object> getScenicAnnouncements(@RequestParam Long scenicId) {
        log.info("用户查询景区公告: scenicId={}", scenicId);
        
        List<Announcement> announcements = announcementService.getScenicAnnouncements(scenicId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("announcements", announcements);
        response.put("total", announcements.size());
        
        return response;
    }
}

