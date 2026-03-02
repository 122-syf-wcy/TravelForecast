package com.travel.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.admin.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}

