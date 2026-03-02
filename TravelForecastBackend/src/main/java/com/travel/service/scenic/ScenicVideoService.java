package com.travel.service.scenic;

import com.travel.entity.scenic.ScenicVideo;

import java.util.List;

/**
 * 景区视频服务接口
 */
public interface ScenicVideoService {

    /**
     * 根据景区ID查询所有视频
     */
    List<ScenicVideo> getByScenicId(Long scenicId);

    /**
     * 添加景区视频
     */
    ScenicVideo add(ScenicVideo scenicVideo);

    /**
     * 批量添加景区视频
     */
    boolean batchAdd(List<ScenicVideo> videos);

    /**
     * 删除视频
     */
    boolean delete(Long id);

    /**
     * 删除景区的所有视频
     */
    boolean deleteByScenicId(Long scenicId);

    /**
     * 更新视频信息
     */
    boolean update(ScenicVideo scenicVideo);
}

