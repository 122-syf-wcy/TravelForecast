package com.travel.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.user.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户收藏Mapper
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
    
    /**
     * 检查用户是否收藏了某个景区
     */
    @Select("SELECT COUNT(*) FROM user_favorites WHERE user_id = #{userId} AND scenic_id = #{scenicId}")
    int checkFavorite(@Param("userId") Long userId, @Param("scenicId") Long scenicId);
    
    /**
     * 获取景区的收藏数
     */
    @Select("SELECT COUNT(*) FROM user_favorites WHERE scenic_id = #{scenicId}")
    int getFavoriteCount(@Param("scenicId") Long scenicId);
    
    /**
     * 获取景区截止到昨天的收藏数
     */
    @Select("SELECT COUNT(*) FROM user_favorites WHERE scenic_id = #{scenicId} AND DATE(created_at) < CURDATE()")
    int getYesterdayFavoriteCount(@Param("scenicId") Long scenicId);
}

