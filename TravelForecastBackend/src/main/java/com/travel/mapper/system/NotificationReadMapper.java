package com.travel.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.system.NotificationRead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知阅读记录Mapper
 */
@Mapper
public interface NotificationReadMapper extends BaseMapper<NotificationRead> {
}

