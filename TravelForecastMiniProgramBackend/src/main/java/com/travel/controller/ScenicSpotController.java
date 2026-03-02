package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.ScenicSpot;
import com.travel.mapper.ScenicSpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class ScenicSpotController {

    private final ScenicSpotMapper scenicSpotMapper;
    private final OssProxyController ossProxy;

    @GetMapping("/list")
    public Result<List<ScenicSpot>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<>();
        qw.isNull(ScenicSpot::getParentId);
        qw.eq(ScenicSpot::getStatus, "ACTIVE");
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(ScenicSpot::getName, keyword)
                    .or().like(ScenicSpot::getDescription, keyword)
                    .or().like(ScenicSpot::getAddress, keyword));
        }
        if (category != null && !category.isBlank()) {
            qw.like(ScenicSpot::getCategory, category);
        }
        qw.orderByDesc(ScenicSpot::getRating);
        List<ScenicSpot> list = scenicSpotMapper.selectList(qw);
        list.forEach(this::proxySpotImage);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<ScenicSpot> detail(@PathVariable Long id) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot == null) {
            return Result.error("景区不存在");
        }
        proxySpotImage(spot);
        return Result.success(spot);
    }

    @GetMapping("/{id}/sub-spots")
    public Result<List<ScenicSpot>> subSpots(@PathVariable Long id) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<>();
        qw.eq(ScenicSpot::getParentId, id);
        qw.eq(ScenicSpot::getStatus, "ACTIVE");
        qw.orderByAsc(ScenicSpot::getSortOrder);
        List<ScenicSpot> list = scenicSpotMapper.selectList(qw);
        list.forEach(this::proxySpotImage);
        return Result.success(list);
    }

    private void proxySpotImage(ScenicSpot s) {
        if (s != null && s.getImageUrl() != null) {
            s.setImageUrl(ossProxy.toProxyUrl(s.getImageUrl()));
        }
    }
}
