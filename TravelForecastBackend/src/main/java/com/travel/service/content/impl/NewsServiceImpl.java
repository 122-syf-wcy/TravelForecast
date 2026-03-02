package com.travel.service.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.entity.content.News;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.mapper.content.NewsMapper;
import com.travel.service.content.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 新闻资讯服务实现
 */
@Slf4j
@Service
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    @Autowired
    private MerchantProfileMapper merchantProfileMapper;

    @Override
    public Page<News> getNewsPage(int page, int size, String keyword, String status, Long scenicId) {
        Page<News> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(News::getTitle, keyword);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(News::getStatus, status);
        }
        
        if (scenicId != null) {
            wrapper.eq(News::getScenicId, scenicId);
        }
        
        wrapper.eq(News::getStatus, "published"); // 只显示已发布的
        wrapper.orderByDesc(News::getPublishTime);
        
        return this.page(pageParam, wrapper);
    }

    @Override
    public boolean publishNews(News news) {
        if (news.getStatus() == null) {
            news.setStatus("draft");
        }
        
        if ("published".equals(news.getStatus()) && news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        
        if (news.getViews() == null) {
            news.setViews(0);
        }
        
        return this.save(news);
    }

    @Override
    public boolean updateNews(News news) {
        News existing = this.getById(news.getId());
        if (existing == null) {
            return false;
        }
        
        // 如果从草稿变为发布，设置发布时间
        if ("published".equals(news.getStatus()) && 
            !"published".equals(existing.getStatus()) && 
            news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        
        return this.updateById(news);
    }

    @Override
    public boolean deleteNews(Long id, Long userId) {
        News news = this.getById(id);
        if (news == null) {
            return false;
        }
        
        // 检查权限：只能删除自己的新闻
        if (!news.getAuthorId().equals(userId)) {
            log.warn("用户{}尝试删除不属于自己的新闻{}", userId, id);
            return false;
        }
        
        return this.removeById(id);
    }

    @Override
    public News getNewsDetail(Long id) {
        return this.getById(id);
    }

    @Override
    public void increaseViews(Long id) {
        News news = this.getById(id);
        if (news != null) {
            news.setViews((news.getViews() == null ? 0 : news.getViews()) + 1);
            this.updateById(news);
        }
    }

    @Override
    public Page<News> getMerchantNewsPage(int page, int size, Long userId, String keyword, String status) {
        // 先查询商家管理的景区
        LambdaQueryWrapper<MerchantProfile> profileWrapper = new LambdaQueryWrapper<>();
        profileWrapper.eq(MerchantProfile::getUserId, userId);
        MerchantProfile profile = merchantProfileMapper.selectOne(profileWrapper);
        
        Page<News> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        
        if (profile != null && profile.getScenicId() != null) {
            // 只能查看和管理自己景区的新闻
            wrapper.eq(News::getScenicId, profile.getScenicId());
        } else {
            // 如果商家没有关联景区，返回空列表
            wrapper.eq(News::getAuthorId, -1L); // 不可能匹配的条件
        }
        
        wrapper.eq(News::getAuthorId, userId); // 只能看到自己发布的
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(News::getTitle, keyword);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(News::getStatus, status);
        }
        
        wrapper.orderByDesc(News::getCreatedAt);
        
        return this.page(pageParam, wrapper);
    }
}

