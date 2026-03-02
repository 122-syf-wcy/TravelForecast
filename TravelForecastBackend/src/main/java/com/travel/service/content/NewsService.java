package com.travel.service.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.content.News;

/**
 * 新闻资讯服务接口
 */
public interface NewsService extends IService<News> {
    
    /**
     * 分页查询新闻列表
     */
    Page<News> getNewsPage(int page, int size, String keyword, String status, Long scenicId);
    
    /**
     * 发布新闻
     */
    boolean publishNews(News news);
    
    /**
     * 更新新闻
     */
    boolean updateNews(News news);
    
    /**
     * 删除新闻
     */
    boolean deleteNews(Long id, Long userId);
    
    /**
     * 获取新闻详情
     */
    News getNewsDetail(Long id);
    
    /**
     * 增加浏览量
     */
    void increaseViews(Long id);
    
    /**
     * 获取商家可管理的新闻（根据景区ID）
     */
    Page<News> getMerchantNewsPage(int page, int size, Long userId, String keyword, String status);
}

