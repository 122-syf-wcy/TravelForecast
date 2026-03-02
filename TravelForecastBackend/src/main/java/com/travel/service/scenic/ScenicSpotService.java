package com.travel.service.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.ScenicSpot;

import java.util.List;

/**
 * 景区服务接口
 */
public interface ScenicSpotService {

    /**
     * 分页查询景区列表
     */
    Page<ScenicSpot> getPage(int page, int size, String status, String keyword);

    /**
     * 根据ID查询景区详情
     */
    ScenicSpot getById(Long id);

    /**
     * 根据景区编码查询景区详情
     */
    ScenicSpot getBySpotCode(String spotCode);

    /**
     * 根据ID或景区编码查询景区详情（自动判断）
     */
    ScenicSpot getByIdOrCode(String idOrCode);

    /**
     * 创建景区
     */
    ScenicSpot create(ScenicSpot scenicSpot);

    /**
     * 更新景区信息
     */
    ScenicSpot update(ScenicSpot scenicSpot);

    /**
     * 删除景区（软删除）
     */
    boolean delete(Long id);

    /**
     * 获取所有激活的景区
     */
    List<ScenicSpot> getAllActive();

    /**
     * 按城市查询景区
     */
    List<ScenicSpot> getByCity(String city);

    /**
     * 搜索景区
     */
    List<ScenicSpot> search(String keyword);

    /**
     * 更新景区状态
     */
    boolean updateStatus(Long id, String status);

    /**
     * 获取某景区下的子景点列表
     */
    List<ScenicSpot> getSubSpots(Long parentId);

    /**
     * 创建子景点
     */
    ScenicSpot createSubSpot(Long parentId, ScenicSpot subSpot);
}

