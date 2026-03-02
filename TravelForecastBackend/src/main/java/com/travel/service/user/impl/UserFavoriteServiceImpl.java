package com.travel.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.user.UserFavorite;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.user.UserFavoriteMapper;
import com.travel.service.user.UserFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户收藏服务实现
 */
@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements UserFavoriteService {
    
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    
    @Autowired
    private ScenicSpotMapper scenicSpotMapper;
    
    @Override
    @Transactional
    public boolean addFavorite(Long userId, Long scenicId) {
        // 检查是否已收藏
        if (isFavorited(userId, scenicId)) {
            return false;
        }
        
        // 添加收藏
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setScenicId(scenicId);
        favorite.setCreatedAt(LocalDateTime.now());
        
        boolean result = this.save(favorite);
        
        // 更新景区收藏数
        if (result) {
            updateScenicFavoriteCount(scenicId);
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public boolean removeFavorite(Long userId, Long scenicId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getScenicId, scenicId);
        
        boolean result = this.remove(wrapper);
        
        // 更新景区收藏数
        if (result) {
            updateScenicFavoriteCount(scenicId);
        }
        
        return result;
    }
    
    @Override
    public boolean isFavorited(Long userId, Long scenicId) {
        return userFavoriteMapper.checkFavorite(userId, scenicId) > 0;
    }
    
    @Override
    public int getFavoriteCount(Long scenicId) {
        return userFavoriteMapper.getFavoriteCount(scenicId);
    }
    
    @Override
    public double calculateStarRating(int favoritesCount) {
        if (favoritesCount >= 1000) return 5.0;
        if (favoritesCount >= 500) return 4.5;
        if (favoritesCount >= 200) return 4.0;
        if (favoritesCount >= 100) return 3.5;
        if (favoritesCount >= 50) return 3.0;
        if (favoritesCount >= 20) return 2.5;
        if (favoritesCount >= 10) return 2.0;
        if (favoritesCount >= 5) return 1.5;
        if (favoritesCount >= 1) return 1.0;
        return 0.5; // 默认半星
    }
    
    /**
     * 更新景区的收藏数统计
     */
    private void updateScenicFavoriteCount(Long scenicId) {
        int count = getFavoriteCount(scenicId);
        ScenicSpot scenic = scenicSpotMapper.selectById(scenicId);
        if (scenic != null) {
            scenic.setFavoritesCount(count);
            scenicSpotMapper.updateById(scenic);
        }
    }
}

