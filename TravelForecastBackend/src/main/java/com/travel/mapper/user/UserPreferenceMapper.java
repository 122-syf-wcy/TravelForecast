package com.travel.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.user.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好Mapper接口
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}

