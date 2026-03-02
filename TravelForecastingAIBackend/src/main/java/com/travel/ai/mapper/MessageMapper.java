package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
