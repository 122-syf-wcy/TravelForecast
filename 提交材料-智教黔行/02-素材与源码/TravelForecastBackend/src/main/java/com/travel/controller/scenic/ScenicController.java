package com.travel.controller.scenic;

import com.travel.common.Result;
import com.travel.dto.scenic.ScenicDetailVO;
import com.travel.entity.scenic.Facility;
import com.travel.entity.scenic.ScenicImage;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.entity.scenic.ScenicVideo;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.mapper.scenic.ScenicVideoMapper;
import com.travel.service.scenic.FacilityService;
import com.travel.service.scenic.ScenicImageService;
import com.travel.service.scenic.ScenicSpotService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 用户端景区查询Controller
 */
@Slf4j
@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
public class ScenicController {

    private final ScenicSpotService scenicSpotService;
    private final ScenicImageService scenicImageService;
    private final ScenicVideoMapper scenicVideoMapper;
    private final ScenicStatisticsMapper scenicStatisticsMapper;
    private final FacilityService facilityService;

    /**
     * 获取所有激活的景区列表
     */
    @GetMapping({"", "/list"})
    public Result<List<ScenicSpot>> list() {
        log.info("查询所有激活的景区");
        List<ScenicSpot> spots = scenicSpotService.getAllActive();

        spots.forEach(spot -> {
            if (spot.getImageUrl() == null || spot.getImageUrl().trim().isEmpty()) {
                List<ScenicImage> images = scenicImageService.getByScenicId(spot.getId());
                if (images != null && !images.isEmpty()) {
                    spot.setImageUrl(images.get(0).getImageUrl());
                }
            }
            spot.setImageUrl(convertOssUrlToRelative(spot.getImageUrl()));
        });

        return Result.success(spots);
    }

    /**
     * 获取景区详情（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}")
    public Result<ScenicDetailVO> detail(@PathVariable String idOrCode) {
        log.info("查询景区详情: idOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        List<ScenicImage> allImages = scenicImageService.getByScenicId(spot.getId());
        List<ScenicImage> galleryImages = scenicImageService.getByScenicIdAndType(spot.getId(), "GALLERY");

        ScenicDetailVO vo = new ScenicDetailVO();
        vo.setId(spot.getId());
        vo.setName(spot.getName());
        vo.setDescription(spot.getDescription());
        vo.setFullDescription(spot.getFullDescription());

        String imageUrl = spot.getImageUrl();
        if ((imageUrl == null || imageUrl.trim().isEmpty()) && allImages != null && !allImages.isEmpty()) {
            imageUrl = allImages.get(0).getImageUrl();
        }
        vo.setImageUrl(convertOssUrlToRelative(imageUrl));

        vo.setRating(spot.getRating() != null ? spot.getRating().doubleValue() : 4.5);
        vo.setCategory(spot.getCategory());
        vo.setLevel(spot.getLevel());
        vo.setAddress(spot.getAddress());
        vo.setOpeningHours(spot.getOpeningHours());
        vo.setPrice(spot.getPrice());
        vo.setSuggestedTime(spot.getSuggestedTime());
        vo.setMaxCapacity(spot.getMaxCapacity());
        vo.setReviewCount(spot.getReviewCount());
        vo.setTags(spot.getTags());
        vo.setLatitude(spot.getLatitude() != null ? spot.getLatitude().doubleValue() : null);
        vo.setLongitude(spot.getLongitude() != null ? spot.getLongitude().doubleValue() : null);

        List<ScenicImage> carouselImages = (galleryImages != null && !galleryImages.isEmpty()) ? galleryImages : allImages;
        if (carouselImages != null && !carouselImages.isEmpty()) {
            vo.setImages(carouselImages.stream()
                    .map(img -> convertOssUrlToRelative(img.getImageUrl()))
                    .collect(Collectors.toList()));
        }

        // 从数据库查询最新的统计数据
        QueryWrapper<ScenicStatistics> wrapper = new QueryWrapper<>();
        wrapper.eq("scenic_id", spot.getId())
               .orderByDesc("stat_date")
               .last("LIMIT 1");

        ScenicStatistics latestStats = scenicStatisticsMapper.selectOne(wrapper);

        if (latestStats != null) {
            int baseFlow = latestStats.getVisitorCount();
            int fluctuation = (int)(baseFlow * 0.1 * (ThreadLocalRandom.current().nextDouble() - 0.5) * 2);
            vo.setCurrentFlow(Math.max(0, baseFlow + fluctuation));

            Map<String, Object> weather = new HashMap<>();
            weather.put("temperature", 18 + ThreadLocalRandom.current().nextInt(10));
            weather.put("condition", latestStats.getWeather() != null ? latestStats.getWeather() : "晴朗");
            weather.put("windSpeed", 2 + ThreadLocalRandom.current().nextInt(4));
            weather.put("humidity", 60 + ThreadLocalRandom.current().nextInt(20));
            vo.setWeather(weather);
        } else {
            vo.setCurrentFlow(300 + ThreadLocalRandom.current().nextInt(500));

            Map<String, Object> weather = new HashMap<>();
            weather.put("temperature", 20 + ThreadLocalRandom.current().nextInt(6));
            weather.put("condition", "晴朗");
            weather.put("windSpeed", 3);
            weather.put("humidity", 65);
            vo.setWeather(weather);
        }

        return Result.success(vo);
    }

    /**
     * 获取景区实时数据（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}/realtime")
    public Result<Map<String, Object>> realtime(@PathVariable String idOrCode) {
        log.info("查询景区实时数据: idOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        Map<String, Object> data = new HashMap<>();

        QueryWrapper<ScenicStatistics> wrapper = new QueryWrapper<>();
        wrapper.eq("scenic_id", spot.getId())
               .orderByDesc("stat_date")
               .last("LIMIT 1");

        ScenicStatistics latestStats = scenicStatisticsMapper.selectOne(wrapper);

        int currentFlow;
        int maxCapacity = spot.getMaxCapacity() != null ? spot.getMaxCapacity() : 2000;

        if (latestStats != null) {
            int baseFlow = latestStats.getVisitorCount();
            int fluctuation = (int)(baseFlow * 0.1 * (ThreadLocalRandom.current().nextDouble() - 0.5) * 2);
            currentFlow = Math.max(0, baseFlow + fluctuation);
            log.info("景区 {} 使用真实数据: 基础流量={}, 实时流量={}", spot.getName(), baseFlow, currentFlow);
        } else {
            currentFlow = 300 + ThreadLocalRandom.current().nextInt(500);
            log.warn("景区 {} 没有历史数据，使用默认值: {}", spot.getName(), currentFlow);
        }

        double flowRate = Math.min(1.0, currentFlow / (double) maxCapacity);

        data.put("currentFlow", currentFlow);
        data.put("maxCapacity", maxCapacity);
        data.put("flowRate", flowRate);
        data.put("updateTime", java.time.LocalDateTime.now().toString());

        Map<String, Integer> areaDistribution = new HashMap<>();
        areaDistribution.put("A区", (int)(currentFlow * 0.35));
        areaDistribution.put("B区", (int)(currentFlow * 0.25));
        areaDistribution.put("C区", (int)(currentFlow * 0.20));
        areaDistribution.put("D区", (int)(currentFlow * 0.20));
        data.put("areaDistribution", areaDistribution);

        return Result.success(data);
    }

    /**
     * 搜索景区
     */
    @GetMapping("/search")
    public Result<List<ScenicSpot>> search(@RequestParam String keyword) {
        log.info("搜索景区: keyword={}", keyword);
        List<ScenicSpot> spots = scenicSpotService.search(keyword);
        return Result.success(spots);
    }

    /**
     * 获取景区视频列表（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}/videos")
    public Result<List<ScenicVideo>> getVideos(@PathVariable String idOrCode) {
        log.info("查询景区视频: idOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        QueryWrapper<ScenicVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scenic_id", spot.getId());
        queryWrapper.orderByDesc("created_at");

        List<ScenicVideo> videos = scenicVideoMapper.selectList(queryWrapper);

        log.info("景区 {} 共有 {} 个视频", spot.getName(), videos.size());
        return Result.success(videos);
    }

    /**
     * 获取景区内子景点列表（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}/sub-spots")
    public Result<List<Map<String, Object>>> getSubSpots(@PathVariable String idOrCode) {
        log.info("查询景区子景点: idOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        List<ScenicSpot> subSpots = scenicSpotService.getSubSpots(spot.getId());

        List<Map<String, Object>> result = subSpots.stream().map(sub -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sub.getId());
            item.put("name", sub.getName());
            item.put("description", sub.getDescription());
            item.put("imageUrl", convertOssUrlToRelative(sub.getImageUrl()));
            item.put("latitude", sub.getLatitude());
            item.put("longitude", sub.getLongitude());
            item.put("tags", sub.getTags());
            item.put("openingHours", sub.getOpeningHours());
            item.put("sortOrder", sub.getSortOrder());
            return item;
        }).collect(Collectors.toList());

        log.info("景区 {} 共有 {} 个子景点", spot.getName(), result.size());
        return Result.success(result);
    }

    /**
     * 获取景区设施列表（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}/facilities")
    public Result<Map<String, Object>> getFacilities(
            @PathVariable String idOrCode,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查询景区设施: idOrCode={}, category={}, page={}, size={}", idOrCode, category, page, size);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        Page<Facility> result = facilityService.getScenicFacilities(spot.getId(), category, page, size);

        result.getRecords().forEach(facility -> {
            if (facility.getThumbnail() != null) {
                facility.setThumbnail(convertOssUrlToRelative(facility.getThumbnail()));
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());

        log.info("景区 {} 共有 {} 个设施", spot.getName(), result.getTotal());
        return Result.success(response);
    }

    /**
     * 是否直接使用OSS URL
     */
    @org.springframework.beans.factory.annotation.Value("${app.oss.use-direct-url:true}")
    private boolean useDirectOssUrl;

    /**
     * 将OSS完整URL转换为相对路径
     */
    private String convertOssUrlToRelative(String ossUrl) {
        if (ossUrl == null || ossUrl.isEmpty()) {
            return ossUrl;
        }

        if (useDirectOssUrl && ossUrl.contains("aliyuncs.com")) {
            return ossUrl;
        }

        if (ossUrl.startsWith("/")) {
            return ossUrl;
        }

        try {
            if (ossUrl.contains("aliyuncs.com")) {
                if (ossUrl.contains("/scenic/images/")) {
                    return ossUrl.substring(ossUrl.indexOf("/scenic/images/") + "/scenic".length());
                }
                int lastSlashIdx = ossUrl.lastIndexOf("/");
                if (lastSlashIdx > 0) {
                    return "/images" + ossUrl.substring(lastSlashIdx);
                }
            }

            if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                int lastSlashIdx = ossUrl.lastIndexOf("/");
                if (lastSlashIdx > 0) {
                    return "/images" + ossUrl.substring(lastSlashIdx);
                }
            }
        } catch (Exception e) {
            log.warn("转换OSS URL失败: {}", ossUrl, e);
        }

        return ossUrl;
    }
}
