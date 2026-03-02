package com.travel.controller.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.content.News;
import com.travel.service.content.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端新闻资讯Controller
 */
@Slf4j
@RestController
@RequestMapping("/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    /**
     * 分页查询新闻列表（用户端）
     * GET /news
     */
    @GetMapping
    public Result<Page<News>> getNewsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long scenicId
    ) {
        log.info("用户查询新闻列表: page={}, size={}, keyword={}, scenicId={}", 
                page, size, keyword, scenicId);

        Page<News> newsPage = newsService.getNewsPage(page, size, keyword, "published", scenicId);
        return Result.success(newsPage);
    }

    /**
     * 获取新闻详情（用户端）
     * GET /news/{id}
     */
    @GetMapping("/{id}")
    public Result<News> getNewsDetail(@PathVariable Long id) {
        log.info("用户查看新闻详情: id={}", id);

        News news = newsService.getNewsDetail(id);
        if (news == null) {
            return Result.error("新闻不存在");
        }

        // 只能查看已发布的新闻
        if (!"published".equals(news.getStatus())) {
            return Result.error("新闻未发布");
        }

        // 增加浏览量
        newsService.increaseViews(id);

        return Result.success(news);
    }

    /**
     * 获取热门新闻（按浏览量降序排序，默认3条）
     * GET /news/hot
     */
    @GetMapping("/hot")
    public Result<Page<News>> getHotNews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        log.info("用户查询热门新闻: page={}, size={}", page, size);

        // 按浏览量降序排序
        Page<News> pageParam = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<News> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        wrapper.eq(News::getStatus, "published"); // 只显示已发布的
        wrapper.orderByDesc(News::getViews);      // 按浏览量降序
        wrapper.orderByDesc(News::getPublishTime); // 浏览量相同时按发布时间降序
        
        Page<News> newsPage = newsService.page(pageParam, wrapper);
        log.info("返回{}条热门新闻", newsPage.getRecords().size());
        return Result.success(newsPage);
    }
}

