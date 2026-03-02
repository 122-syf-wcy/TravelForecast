package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI知识库 Mapper
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
}
