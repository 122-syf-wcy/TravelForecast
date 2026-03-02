package com.travel.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.user.UserBehaviorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户行为日志Mapper接口
 */
@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {
    
    /**
     * 获取平均停留时长
     */
    @Select("SELECT AVG(duration) FROM user_behavior_logs WHERE created_at >= #{since}")
    Double getAverageDuration(LocalDateTime since);

    /**
     * 获取独立访客数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_behavior_logs WHERE created_at >= #{since}")
    Long countUniqueUsers(LocalDateTime since);

    /**
     * 获取每小时访问分布
     */
    @Select("SELECT HOUR(created_at) as hour, COUNT(*) as count " +
            "FROM user_behavior_logs " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY HOUR(created_at) " +
            "ORDER BY hour")
    List<Map<String, Object>> getHourlyDistribution();

    /**
     * 获取模块使用排行
     */
    @Select("SELECT module as name, COUNT(*) as count " +
            "FROM user_behavior_logs " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "AND module IS NOT NULL AND module != '' " +
            "GROUP BY module " +
            "ORDER BY count DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getModuleRanking();

    /**
     * 获取停留时长分布
     */
    @Select("SELECT " +
            "CASE " +
            "  WHEN duration < 60 THEN '<1分钟' " +
            "  WHEN duration >= 60 AND duration < 180 THEN '1-3分钟' " +
            "  WHEN duration >= 180 AND duration < 300 THEN '3-5分钟' " +
            "  WHEN duration >= 300 AND duration < 600 THEN '5-10分钟' " +
            "  ELSE '>10分钟' " +
            "END as range_name, " +
            "COUNT(*) as count " +
            "FROM user_behavior_logs " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "AND duration IS NOT NULL " +
            "GROUP BY range_name " +
            "ORDER BY " +
            "  CASE range_name " +
            "    WHEN '<1分钟' THEN 1 " +
            "    WHEN '1-3分钟' THEN 2 " +
            "    WHEN '3-5分钟' THEN 3 " +
            "    WHEN '5-10分钟' THEN 4 " +
            "    ELSE 5 " +
            "  END")
    List<Map<String, Object>> getDurationDistribution();

    /**
     * 获取热门页面排行
     */
    @Select("SELECT module as page, COUNT(*) as visits " +
            "FROM user_behavior_logs " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "AND behavior_type = 'page_view' " +
            "AND module IS NOT NULL AND module != '' " +
            "GROUP BY module " +
            "ORDER BY visits DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getHotPages();
}

