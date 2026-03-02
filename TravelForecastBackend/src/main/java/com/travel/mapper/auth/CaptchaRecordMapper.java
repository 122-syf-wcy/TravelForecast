package com.travel.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.auth.CaptchaRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验证码记录Mapper接口
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@Mapper
public interface CaptchaRecordMapper extends BaseMapper<CaptchaRecord> {
    // MyBatis-Plus已提供基础CRUD方法
    // 可在此添加自定义SQL方法
}
