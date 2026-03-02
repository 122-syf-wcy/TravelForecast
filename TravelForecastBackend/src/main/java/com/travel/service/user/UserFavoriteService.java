package com.travel.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.user.UserFavorite;

/**
 * 用户收藏服务接口
 */
public interface UserFavoriteService extends IService<UserFavorite> {
    
    /**
     * 添加收藏
     */
    boolean addFavorite(Long userId, Long scenicId);
    
    /**
     * 取消收藏
     */
    boolean removeFavorite(Long userId, Long scenicId);
    
    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long scenicId);
    
    /**
     * 获取景区收藏数
     */
    int getFavoriteCount(Long scenicId);
    
    /**
     * 根据收藏数计算星级评分
     */
    double calculateStarRating(int favoritesCount);
}

