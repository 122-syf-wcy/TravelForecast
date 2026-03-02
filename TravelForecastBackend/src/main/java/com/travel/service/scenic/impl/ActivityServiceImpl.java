package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.Activity;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.entity.content.News;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.mapper.scenic.ActivityMapper;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.mapper.content.NewsMapper;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.service.scenic.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 活动服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    
    private final ActivityMapper activityMapper;
    private final MerchantProfileMapper merchantProfileMapper;
    private final NewsMapper newsMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    
    @Override
    public Page<Activity> getMerchantActivities(Long merchantId, int page, int size, String status, String category) {
        log.info("查询商家活动列表: merchantId={}, page={}, size={}, status={}, category={}", 
                merchantId, page, size, status, category);
        
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        Page<Activity> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getMerchantId, merchantId)
               .eq(Activity::getScenicId, profile.getScenicId())
               .orderByDesc(Activity::getCreatedAt);
        
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(Activity::getStatus, status);
        }
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Activity::getCategory, category);
        }
        
        return activityMapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public Activity getActivityById(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        
        // 增加浏览次数
        activity.setViews((activity.getViews() == null ? 0 : activity.getViews()) + 1);
        activityMapper.updateById(activity);
        
        return activity;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity createActivity(Activity activity, Long merchantId) {
        log.info("创建活动: merchantId={}, name={}", merchantId, activity.getName());
        
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        // 生成活动编码
        String activityCode = generateActivityCode(profile.getScenicId());
        
        activity.setActivityCode(activityCode);
        activity.setMerchantId(merchantId);
        activity.setScenicId(profile.getScenicId());
        activity.setCurrentParticipants(0);
        activity.setIsPromoted(false);
        activity.setViews(0);
        
        // 如果没有设置状态，默认为草稿
        if (activity.getStatus() == null || activity.getStatus().isEmpty()) {
            activity.setStatus("draft");
        }
        
        activityMapper.insert(activity);
        log.info("活动创建成功: id={}, code={}", activity.getId(), activity.getActivityCode());
        
        return activity;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity updateActivity(Long id, Activity activity, Long merchantId) {
        log.info("更新活动: id={}, merchantId={}", id, merchantId);
        
        Activity existing = activityMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("活动不存在");
        }
        
        if (!existing.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限修改此活动");
        }
        
        // 保留某些字段不被更新
        activity.setId(id);
        activity.setActivityCode(existing.getActivityCode());
        activity.setMerchantId(existing.getMerchantId());
        activity.setScenicId(existing.getScenicId());
        activity.setCurrentParticipants(existing.getCurrentParticipants());
        activity.setIsPromoted(existing.getIsPromoted());
        activity.setPromotedAt(existing.getPromotedAt());
        activity.setNewsId(existing.getNewsId());
        activity.setViews(existing.getViews());
        
        activityMapper.updateById(activity);
        log.info("活动更新成功: id={}", id);
        
        return activity;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long id, Long merchantId) {
        log.info("删除活动: id={}, merchantId={}", id, merchantId);
        
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        
        if (!activity.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限删除此活动");
        }
        
        // 如果已推广，需要先取消推广或删除关联的新闻
        if (activity.getIsPromoted() && activity.getNewsId() != null) {
            newsMapper.deleteById(activity.getNewsId());
        }
        
        activityMapper.deleteById(id);
        log.info("活动删除成功: id={}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void promoteActivity(Long id, Long merchantId) {
        log.info("推广活动: id={}, merchantId={}", id, merchantId);
        
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        
        if (!activity.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限推广此活动");
        }
        
        if (activity.getIsPromoted()) {
            throw new RuntimeException("活动已推广");
        }
        
        // 获取景区信息
        ScenicSpot scenic = scenicSpotMapper.selectById(activity.getScenicId());
        if (scenic == null) {
            throw new RuntimeException("景区信息不存在");
        }
        
        // 创建新闻
        News news = new News();
        news.setTitle(activity.getName());
        news.setSummary(activity.getDescription());
        // 确保 content 有值，避免数据库报错
        news.setContent(activity.getContent() != null && !activity.getContent().isEmpty() 
            ? activity.getContent() 
            : activity.getDescription());
        news.setCategory("event"); // 活动类别
        // 将封面图片转换为List<String>类型
        if (activity.getCoverImage() != null && !activity.getCoverImage().isEmpty()) {
            news.setCovers(java.util.Collections.singletonList(activity.getCoverImage()));
        }
        news.setScenicId(activity.getScenicId());
        news.setAuthorId(merchantId);
        news.setViews(0);
        news.setStatus("published");
        news.setPublishTime(LocalDateTime.now());
        news.setActivityId(activity.getId());
        news.setSource("activity");
        
        newsMapper.insert(news);
        
        // 更新活动
        activity.setIsPromoted(true);
        activity.setPromotedAt(LocalDateTime.now());
        activity.setNewsId(news.getId());
        activityMapper.updateById(activity);
        
        log.info("活动推广成功: activityId={}, newsId={}", id, news.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpromoteActivity(Long id, Long merchantId) {
        log.info("取消推广活动: id={}, merchantId={}", id, merchantId);
        
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        
        if (!activity.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("无权限操作此活动");
        }
        
        if (!activity.getIsPromoted()) {
            throw new RuntimeException("活动未推广");
        }
        
        // 删除关联的新闻
        if (activity.getNewsId() != null) {
            newsMapper.deleteById(activity.getNewsId());
        }
        
        // 更新活动
        activity.setIsPromoted(false);
        activity.setPromotedAt(null);
        activity.setNewsId(null);
        activityMapper.updateById(activity);
        
        log.info("取消推广成功: activityId={}", id);
    }
    
    @Override
    public Map<String, Object> getActivityStats(Long merchantId) {
        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, merchantId)
        );
        
        if (profile == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        // 统计各种状态的活动数量
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getMerchantId, merchantId)
               .eq(Activity::getScenicId, profile.getScenicId());
        
        Long totalCount = activityMapper.selectCount(wrapper);
        
        Long ongoingCount = activityMapper.selectCount(
            wrapper.eq(Activity::getStatus, "ongoing")
        );
        
        Long completedCount = activityMapper.selectCount(
            new LambdaQueryWrapper<Activity>()
                .eq(Activity::getMerchantId, merchantId)
                .eq(Activity::getScenicId, profile.getScenicId())
                .eq(Activity::getStatus, "completed")
        );
        
        Long promotedCount = activityMapper.selectCount(
            new LambdaQueryWrapper<Activity>()
                .eq(Activity::getMerchantId, merchantId)
                .eq(Activity::getScenicId, profile.getScenicId())
                .eq(Activity::getIsPromoted, true)
        );
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("ongoing", ongoingCount);
        stats.put("completed", completedCount);
        stats.put("promoted", promotedCount);
        
        return stats;
    }
    
    @Override
    public Page<Activity> getOngoingActivities(Long scenicId, int page, int size) {
        Page<Activity> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        
        LocalDate now = LocalDate.now();
        
        wrapper.eq(scenicId != null, Activity::getScenicId, scenicId)
               .eq(Activity::getIsPromoted, true)  // 只显示已推广的活动
               .in(Activity::getStatus, "active", "ongoing")  // 支持 active 和 ongoing 状态
               .le(Activity::getStartTime, now)  // 开始时间 <= 今天
               .ge(Activity::getEndTime, now)    // 结束时间 >= 今天
               .orderByDesc(Activity::getStartTime);
        
        return activityMapper.selectPage(pageObj, wrapper);
    }
    
    /**
     * 生成活动编码
     */
    private String generateActivityCode(Long scenicId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ACT" + scenicId + timestamp;
    }
}

