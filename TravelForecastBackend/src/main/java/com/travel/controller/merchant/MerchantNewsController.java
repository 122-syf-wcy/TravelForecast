package com.travel.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.entity.content.News;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.service.content.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商家端新闻资讯Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/news")
public class MerchantNewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private MerchantProfileMapper merchantProfileMapper;

    /**
     * 分页查询商家的新闻列表
     * GET /merchant/news
     */
    @GetMapping
    public Result<Page<News>> getNewsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("商家查询新闻列表: userId={}, page={}, size={}, keyword={}, status={}", 
                userId, page, size, keyword, status);

        Page<News> newsPage = newsService.getMerchantNewsPage(page, size, userId, keyword, status);
        return Result.success(newsPage);
    }

    /**
     * 获取新闻详情
     * GET /merchant/news/{id}
     */
    @GetMapping("/{id}")
    public Result<News> getNewsDetail(
            @PathVariable Long id,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.error("未登录");
        }

        News news = newsService.getNewsDetail(id);
        if (news == null) {
            return Result.error("新闻不存在");
        }

        // 验证权限
        if (!news.getAuthorId().equals(userId)) {
            return Result.error("无权访问");
        }

        return Result.success(news);
    }

    /**
     * 发布新闻
     * POST /merchant/news
     */
    @PostMapping
    public Result<String> publishNews(
            @RequestBody News news,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("商家发布新闻: userId={}, title={}", userId, news.getTitle());

        // 获取商家的景区ID
        MerchantProfile profile = merchantProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantProfile>()
                        .eq(MerchantProfile::getUserId, userId)
        );

        if (profile == null || profile.getScenicId() == null) {
            return Result.error("请先在商家资料中设置管理的景区");
        }

        // 设置景区ID和作者ID
        news.setScenicId(profile.getScenicId());
        news.setAuthorId(userId);

        boolean success = newsService.publishNews(news);
        if (success) {
            return Result.success("发布成功");
        } else {
            return Result.error("发布失败");
        }
    }

    /**
     * 更新新闻
     * PUT /merchant/news/{id}
     */
    @PutMapping("/{id}")
    public Result<String> updateNews(
            @PathVariable Long id,
            @RequestBody News news,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("商家更新新闻: userId={}, newsId={}", userId, id);

        News existing = newsService.getNewsDetail(id);
        if (existing == null) {
            return Result.error("新闻不存在");
        }

        // 验证权限
        if (!existing.getAuthorId().equals(userId)) {
            return Result.error("无权修改");
        }

        news.setId(id);
        news.setAuthorId(userId);
        news.setScenicId(existing.getScenicId()); // 保持景区ID不变

        boolean success = newsService.updateNews(news);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 删除新闻
     * DELETE /merchant/news/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteNews(
            @PathVariable Long id,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("商家删除新闻: userId={}, newsId={}", userId, id);

        boolean success = newsService.deleteNews(id, userId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
}

