package com.travel.service.scenic;

import com.travel.entity.scenic.ScenicImage;

import java.util.List;

/**
 * 景区图片服务接口
 */
public interface ScenicImageService {

    /**
     * 根据图片ID查询单张图片
     */
    ScenicImage getById(Long id);

    /**
     * 根据景区ID查询所有图片
     */
    List<ScenicImage> getByScenicId(Long scenicId);

    /**
     * 根据景区ID和类型查询图片
     */
    List<ScenicImage> getByScenicIdAndType(Long scenicId, String imageType);

    /**
     * 添加景区图片
     */
    ScenicImage add(ScenicImage scenicImage);

    /**
     * 批量添加景区图片
     */
    boolean batchAdd(List<ScenicImage> images);

    /**
     * 删除图片
     */
    boolean delete(Long id);

    /**
     * 更新图片排序
     */
    boolean updateSortOrder(Long id, Integer sortOrder);

    /**
     * 删除景区的所有图片
     */
    boolean deleteByScenicId(Long scenicId);
}









