package com.travel.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.common.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}


