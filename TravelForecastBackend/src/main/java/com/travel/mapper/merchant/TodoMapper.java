package com.travel.mapper.merchant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.merchant.Todo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办事项Mapper
 */
@Mapper
public interface TodoMapper extends BaseMapper<Todo> {
}

