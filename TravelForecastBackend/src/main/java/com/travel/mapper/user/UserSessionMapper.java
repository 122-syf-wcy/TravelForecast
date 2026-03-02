package com.travel.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.user.UserSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户会话Mapper接口
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {
    // MyBatis-Plus已提供基础CRUD方法
    // 可在此添加自定义SQL方法
}
