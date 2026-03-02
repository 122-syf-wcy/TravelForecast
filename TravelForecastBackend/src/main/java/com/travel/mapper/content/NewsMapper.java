package com.travel.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.content.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻资讯Mapper接口
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
}

