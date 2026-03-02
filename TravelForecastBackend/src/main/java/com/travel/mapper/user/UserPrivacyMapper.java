package com.travel.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.user.UserPrivacy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户隐私设置Mapper接口
 */
@Mapper
public interface UserPrivacyMapper extends BaseMapper<UserPrivacy> {
}

