package com.travel.mapper.scenic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.scenic.ReviewLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价点赞Mapper接口
 */
@Mapper
public interface ReviewLikeMapper extends BaseMapper<ReviewLike> {
}

