package com.travel.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.content.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实时公告Mapper
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}

