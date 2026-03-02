package com.travel.controller.user;

import com.travel.service.user.UserFavoriteService;
import com.travel.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户收藏控制器
 */
@RestController
@RequestMapping("/favorites")
@Slf4j
public class UserFavoriteController {
    
    @Autowired
    private UserFavoriteService userFavoriteService;
    
    /**
     * 添加收藏
     */
    @PostMapping("/{scenicId}")
    public Result<String> addFavorite(@RequestAttribute("userId") Long userId, 
                                      @PathVariable Long scenicId) {
        log.info("用户{}添加收藏景区: {}", userId, scenicId);
        
        boolean success = userFavoriteService.addFavorite(userId, scenicId);
        if (success) {
            return Result.success("收藏成功");
        } else {
            return Result.error("已收藏该景区");
        }
    }
    
    /**
     * 取消收藏
     */
    @DeleteMapping("/{scenicId}")
    public Result<String> removeFavorite(@RequestAttribute("userId") Long userId, 
                                         @PathVariable Long scenicId) {
        log.info("用户{}取消收藏景区: {}", userId, scenicId);
        
        boolean success = userFavoriteService.removeFavorite(userId, scenicId);
        if (success) {
            return Result.success("取消收藏成功");
        } else {
            return Result.error("取消收藏失败");
        }
    }
    
    /**
     * 检查是否已收藏
     */
    @GetMapping("/check/{scenicId}")
    public Result<Map<String, Object>> checkFavorite(@RequestAttribute("userId") Long userId, 
                                                      @PathVariable Long scenicId) {
        boolean isFavorited = userFavoriteService.isFavorited(userId, scenicId);
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorited", isFavorited);
        result.put("scenicId", scenicId);
        return Result.success(result);
    }
    
    /**
     * 获取景区收藏数和星级评分
     */
    @GetMapping("/stats/{scenicId}")
    public Result<Map<String, Object>> getFavoriteStats(@PathVariable Long scenicId) {
        int count = userFavoriteService.getFavoriteCount(scenicId);
        double starRating = userFavoriteService.calculateStarRating(count);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("scenicId", scenicId);
        stats.put("favoritesCount", count);
        stats.put("starRating", starRating);
        
        return Result.success(stats);
    }
    
    /**
     * 批量检查收藏状态
     * POST /favorites/check-batch
     * Body: [1, 2, 3] (景区ID数组)
     */
    @PostMapping("/check-batch")
    public Result<Map<Long, Boolean>> checkFavoriteBatch(
            @RequestAttribute("userId") Long userId,
            @RequestBody java.util.List<Long> scenicIds) {
        log.info("用户{}批量检查收藏状态: {}", userId, scenicIds);
        
        Map<Long, Boolean> result = new HashMap<>();
        if (scenicIds != null && !scenicIds.isEmpty()) {
            for (Long scenicId : scenicIds) {
                result.put(scenicId, userFavoriteService.isFavorited(userId, scenicId));
            }
        }
        return Result.success(result);
    }
}

