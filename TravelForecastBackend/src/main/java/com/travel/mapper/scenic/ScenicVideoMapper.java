package com.travel.mapper.scenic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.scenic.ScenicVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 景区视频Mapper
 */
@Mapper
public interface ScenicVideoMapper extends BaseMapper<ScenicVideo> {

    /**
     * 根据景区ID查询所有视频
     */
    @Select("SELECT * FROM scenic_videos WHERE scenic_id = #{scenicId} ORDER BY created_at DESC")
    List<ScenicVideo> selectByScenicId(@Param("scenicId") Long scenicId);
}

