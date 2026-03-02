package com.travel.mapper.scenic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.scenic.ScenicSpot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 景区Mapper
 */
@Mapper
public interface ScenicSpotMapper extends BaseMapper<ScenicSpot> {

    /**
     * 根据状态查询景区列表
     */
    @Select("SELECT * FROM scenic_spots WHERE status = #{status} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<ScenicSpot> selectByStatus(@Param("status") String status);

    /**
     * 搜索景区（按名称或描述）
     */
    @Select("SELECT * FROM scenic_spots WHERE deleted_at IS NULL AND " +
            "(name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_at DESC")
    List<ScenicSpot> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 按城市查询景区
     */
    @Select("SELECT * FROM scenic_spots WHERE city = #{city} AND deleted_at IS NULL ORDER BY rating DESC")
    List<ScenicSpot> selectByCity(@Param("city") String city);

    /**
     * 查询某景区下的子景点列表
     */
    @Select("SELECT * FROM scenic_spots WHERE parent_id = #{parentId} AND deleted_at IS NULL ORDER BY sort_order ASC, created_at ASC")
    List<ScenicSpot> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有主景区（parent_id 为 NULL 的）
     */
    @Select("SELECT * FROM scenic_spots WHERE parent_id IS NULL AND status = #{status} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<ScenicSpot> selectMainSpotsByStatus(@Param("status") String status);
}

