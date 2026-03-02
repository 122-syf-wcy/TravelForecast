package com.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.WechatUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WechatUserMapper extends BaseMapper<WechatUser> {
    @Select("SELECT * FROM wechat_users WHERE openid = #{openid} AND (deleted_at IS NULL OR deleted_at = 0) LIMIT 1")
    WechatUser selectByOpenid(String openid);

    @Select("SELECT * FROM wechat_users WHERE user_id = #{userId} AND (deleted_at IS NULL OR deleted_at = 0) LIMIT 1")
    WechatUser selectByUserId(Long userId);
}
