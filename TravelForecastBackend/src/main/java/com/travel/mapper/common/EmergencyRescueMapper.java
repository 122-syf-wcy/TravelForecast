package com.travel.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.common.EmergencyRescue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 紧急救援 Mapper
 */
@Mapper
public interface EmergencyRescueMapper extends BaseMapper<EmergencyRescue> {

    /**
     * 根据景区ID查询救援记录
     */
    @Select("SELECT er.id, er.user_id as userId, er.scenic_id as scenicId, " +
            "er.rescue_type as rescueType, er.status, er.contact_name as contactName, " +
            "er.contact_phone as contactPhone, er.location, er.latitude, er.longitude, " +
            "er.description, er.emergency_level as emergencyLevel, " +
            "er.handler_id as handlerId, er.handler_name as handlerName, " +
            "er.handle_time as handleTime, er.complete_time as completeTime, " +
            "er.handle_notes as handleNotes, er.created_at as createdAt, " +
            "er.updated_at as updatedAt, " +
            "u.nickname as userName, u.avatar as userAvatar, s.name as scenicName " +
            "FROM emergency_rescue er " +
            "LEFT JOIN users u ON er.user_id = u.user_id " +
            "LEFT JOIN scenic_spots s ON er.scenic_id = s.id " +
            "WHERE er.scenic_id = #{scenicId} AND er.deleted_at IS NULL " +
            "ORDER BY er.created_at DESC")
    List<Map<String, Object>> selectByScenicId(@Param("scenicId") Long scenicId);

    /**
     * 根据用户ID查询救援记录
     */
    @Select("SELECT er.id, er.user_id as userId, er.scenic_id as scenicId, " +
            "er.rescue_type as rescueType, er.status, er.contact_name as contactName, " +
            "er.contact_phone as contactPhone, er.location, er.latitude, er.longitude, " +
            "er.description, er.emergency_level as emergencyLevel, " +
            "er.handler_id as handlerId, er.handler_name as handlerName, " +
            "er.handle_time as handleTime, er.complete_time as completeTime, " +
            "er.handle_notes as handleNotes, er.created_at as createdAt, " +
            "er.updated_at as updatedAt, " +
            "s.name as scenicName, s.image_url as scenicImage " +
            "FROM emergency_rescue er " +
            "LEFT JOIN scenic_spots s ON er.scenic_id = s.id " +
            "WHERE er.user_id = #{userId} AND er.deleted_at IS NULL " +
            "ORDER BY er.created_at DESC")
    List<Map<String, Object>> selectByUserId(@Param("userId") Long userId);

    /**
     * 获取景区的救援统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as totalCount, " +
            "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pendingCount, " +
            "SUM(CASE WHEN status = 'PROCESSING' THEN 1 ELSE 0 END) as processingCount, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
            "SUM(CASE WHEN emergency_level = 'URGENT' THEN 1 ELSE 0 END) as urgentCount " +
            "FROM emergency_rescue " +
            "WHERE scenic_id = #{scenicId} AND deleted_at IS NULL")
    Map<String, Object> getStatsByScenicId(@Param("scenicId") Long scenicId);
}

