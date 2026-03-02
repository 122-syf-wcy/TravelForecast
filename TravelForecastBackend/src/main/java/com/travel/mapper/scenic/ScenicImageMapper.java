package com.travel.mapper.scenic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.scenic.ScenicImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 景区图片Mapper
 */
@Mapper
public interface ScenicImageMapper extends BaseMapper<ScenicImage> {

    /**
     * 根据景区ID查询所有图片
     */
    @Select("SELECT * FROM scenic_images WHERE scenic_id = #{scenicId} ORDER BY sort_order ASC, created_at DESC")
    List<ScenicImage> selectByScenicId(@Param("scenicId") Long scenicId);

    /**
     * 根据景区ID和图片类型查询
     */
    @Select("SELECT * FROM scenic_images WHERE scenic_id = #{scenicId} AND image_type = #{imageType} ORDER BY sort_order ASC")
    List<ScenicImage> selectByScenicIdAndType(@Param("scenicId") Long scenicId, @Param("imageType") String imageType);

    /**
     * 更新图片排序
     */
    @Update("UPDATE scenic_images SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}

