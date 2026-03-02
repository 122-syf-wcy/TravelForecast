package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.*;
import com.travel.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final CheckInMapper checkInMapper;
    private final UserPointsMapper pointsMapper;
    private final FavoriteMapper favoriteMapper;
    private final ScenicSpotMapper scenicSpotMapper;

    // ---- 签到 ----
    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkIn(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        LocalDate today = LocalDate.now();

        // 检查今天是否已签到
        CheckIn existing = checkInMapper.selectOne(
            new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getCheckinDate, today)
        );

        if (existing != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("checkedIn", true);
            result.put("points", 0);
            result.put("message", "今日已签到");
            result.put("totalPoints", getTotalPoints(userId));
            return Result.success(result);
        }

        // 计算连续签到天数 → 奖励积分
        int consecutiveDays = getConsecutiveDays(userId, today);
        int points = 5 + Math.min(consecutiveDays, 6); // 5~11分，连续签到递增

        // 插入签到记录
        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckinDate(today);
        checkIn.setPoints(points);
        checkIn.setCreatedAt(LocalDateTime.now());
        checkInMapper.insert(checkIn);

        // 增加积分
        addPoints(userId, points);

        Map<String, Object> result = new HashMap<>();
        result.put("checkedIn", true);
        result.put("points", points);
        result.put("consecutiveDays", consecutiveDays + 1);
        result.put("totalPoints", getTotalPoints(userId));
        result.put("message", String.format("签到成功 +%d 黔豆", points));
        return Result.success(result);
    }

    @GetMapping("/checkin/status")
    public Result<Map<String, Object>> checkInStatus(@RequestParam Long userId) {
        LocalDate today = LocalDate.now();
        CheckIn existing = checkInMapper.selectOne(
            new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getCheckinDate, today)
        );

        int consecutiveDays = getConsecutiveDays(userId, today);

        Map<String, Object> result = new HashMap<>();
        result.put("checkedInToday", existing != null);
        result.put("consecutiveDays", consecutiveDays);
        result.put("totalPoints", getTotalPoints(userId));
        return Result.success(result);
    }

    // ---- 收藏 ----
    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> getFavorites(@RequestParam Long userId) {
        List<Favorite> favs = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreatedAt)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite f : favs) {
            Map<String, Object> item = new HashMap<>();
            item.put("favoriteId", f.getId());
            item.put("scenicId", f.getScenicId());
            item.put("createdAt", f.getCreatedAt());
            ScenicSpot spot = scenicSpotMapper.selectById(f.getScenicId());
            if (spot != null) {
                item.put("scenicName", spot.getName());
                item.put("scenicImage", spot.getImageUrl());
                item.put("scenicCategory", spot.getCategory());
            }
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping("/favorites/add")
    public Result<String> addFavorite(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long scenicId = Long.valueOf(body.get("scenicId").toString());

        Favorite existing = favoriteMapper.selectOne(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getScenicId, scenicId)
        );

        if (existing != null) {
            return Result.success("已收藏");
        }

        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setScenicId(scenicId);
        fav.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(fav);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/favorites")
    public Result<String> removeFavorite(
            @RequestParam Long userId,
            @RequestParam Long scenicId) {
        favoriteMapper.delete(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getScenicId, scenicId)
        );
        return Result.success("已取消收藏");
    }

    @GetMapping("/favorites/check")
    public Result<Map<String, Object>> checkFavorite(
            @RequestParam Long userId,
            @RequestParam Long scenicId) {
        Favorite existing = favoriteMapper.selectOne(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getScenicId, scenicId)
        );
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorited", existing != null);
        return Result.success(result);
    }

    // ---- 辅助方法 ----
    private int getConsecutiveDays(Long userId, LocalDate today) {
        int days = 0;
        LocalDate checkDate = today.minusDays(1);
        while (true) {
            CheckIn ci = checkInMapper.selectOne(
                new LambdaQueryWrapper<CheckIn>()
                    .eq(CheckIn::getUserId, userId)
                    .eq(CheckIn::getCheckinDate, checkDate)
            );
            if (ci == null) break;
            days++;
            checkDate = checkDate.minusDays(1);
            if (days > 30) break; // 防止无限循环
        }
        return days;
    }

    private void addPoints(Long userId, int pts) {
        UserPoints up = getOrCreatePoints(userId);
        up.setTotalPoints(up.getTotalPoints() + pts);
        pointsMapper.updateById(up);
    }

    private int getTotalPoints(Long userId) {
        UserPoints up = getOrCreatePoints(userId);
        return up.getTotalPoints();
    }

    private UserPoints getOrCreatePoints(Long userId) {
        UserPoints up = pointsMapper.selectOne(
            new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, userId)
        );
        if (up == null) {
            up = new UserPoints();
            up.setUserId(userId);
            up.setTotalPoints(0);
            up.setUsedPoints(0);
            pointsMapper.insert(up);
        }
        return up;
    }
}
