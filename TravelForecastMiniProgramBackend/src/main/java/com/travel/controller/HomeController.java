package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.Banner;
import com.travel.entity.News;
import com.travel.mapper.BannerMapper;
import com.travel.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {

    private final BannerMapper bannerMapper;
    private final NewsMapper newsMapper;
    private final OssProxyController ossProxy;

    @GetMapping("/content/banners/public")
    public Result<List<Banner>> banners() {
        LambdaQueryWrapper<Banner> qw = new LambdaQueryWrapper<>();
        qw.eq(Banner::getEnabled, true).orderByAsc(Banner::getSort);
        List<Banner> list = bannerMapper.selectList(qw);
        // 将 OSS 图片 URL 转为后端代理 URL，绕过防盗链
        for (Banner b : list) {
            if (b.getImage() != null) {
                b.setImage(ossProxy.toProxyUrl(b.getImage()));
            }
        }
        return Result.success(list);
    }

    @GetMapping("/news/hot")
    public Result<Page<News>> hotNews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        LambdaQueryWrapper<News> qw = new LambdaQueryWrapper<>();
        qw.eq(News::getStatus, "published").orderByDesc(News::getPublishTime);
        Page<News> p = newsMapper.selectPage(new Page<>(page, size), qw);
        return Result.success(p);
    }
}
