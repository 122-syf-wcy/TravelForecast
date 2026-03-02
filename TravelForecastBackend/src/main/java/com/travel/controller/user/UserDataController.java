package com.travel.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.user.UserFavorite;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.user.UserFavoriteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户数据控制器 - 提供小程序端用户相关数据接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserDataController {

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    
    @Autowired
    private ScenicSpotMapper scenicSpotMapper;

    /**
     * 获取用户收藏的景点列表
     */
    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> getUserFavorites(@RequestParam Long userId) {
        log.info("获取用户收藏列表, userId: {}", userId);
        
        try {
            // 查询用户收藏记录
            LambdaQueryWrapper<UserFavorite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserFavorite::getUserId, userId)
                       .orderByDesc(UserFavorite::getCreatedAt);
            List<UserFavorite> favorites = userFavoriteMapper.selectList(queryWrapper);
            
            if (favorites.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            
            // 获取景点ID列表
            List<Long> scenicIds = favorites.stream()
                    .map(UserFavorite::getScenicId)
                    .collect(Collectors.toList());
            
            // 查询景点详情
            List<ScenicSpot> scenicSpots = scenicSpotMapper.selectBatchIds(scenicIds);
            
            // 转换为返回格式
            List<Map<String, Object>> result = scenicSpots.stream().map(scenic -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", scenic.getId());
                item.put("scenicId", scenic.getId());
                item.put("name", scenic.getName());
                item.put("description", scenic.getDescription());
                item.put("imageUrl", scenic.getImageUrl());
                item.put("coverImage", scenic.getImageUrl());
                item.put("address", scenic.getAddress());
                item.put("ticketPrice", scenic.getPrice());
                item.put("rating", scenic.getRating());
                item.put("openTime", scenic.getOpeningHours());
                return item;
            }).collect(Collectors.toList());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户收藏失败", e);
            return Result.success(new ArrayList<>()); // 返回空列表而不是错误
        }
    }
}
