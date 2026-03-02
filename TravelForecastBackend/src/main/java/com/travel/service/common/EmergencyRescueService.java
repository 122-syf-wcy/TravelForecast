package com.travel.service.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.common.EmergencyRescue;
import java.util.List;
import java.util.Map;

/**
 * 紧急救援服务接口
 */
public interface EmergencyRescueService extends IService<EmergencyRescue> {

    /**
     * 创建救援请求
     * @param rescue 救援信息
     * @return 创建结果
     */
    boolean createRescue(EmergencyRescue rescue);

    /**
     * 根据景区ID查询救援记录
     * @param scenicId 景区ID
     * @return 救援记录列表
     */
    List<Map<String, Object>> getRescueListByScenicId(Long scenicId);

    /**
     * 根据用户ID查询救援记录
     * @param userId 用户ID
     * @return 救援记录列表
     */
    List<Map<String, Object>> getRescueListByUserId(Long userId);

    /**
     * 处理救援请求
     * @param rescueId 救援ID
     * @param handlerId 处理人ID
     * @param handlerName 处理人姓名
     * @return 处理结果
     */
    boolean handleRescue(Long rescueId, Long handlerId, String handlerName);

    /**
     * 完成救援请求
     * @param rescueId 救援ID
     * @param handleNotes 处理备注
     * @return 完成结果
     */
    boolean completeRescue(Long rescueId, String handleNotes);

    /**
     * 取消救援请求
     * @param rescueId 救援ID
     * @return 取消结果
     */
    boolean cancelRescue(Long rescueId);

    /**
     * 获取景区的救援统计信息
     * @param scenicId 景区ID
     * @return 统计信息
     */
    Map<String, Object> getStatsByScenicId(Long scenicId);

    /**
     * 根据商家用户ID获取其管理的景区的救援列表
     * @param merchantUserId 商家用户ID
     * @return 救援记录列表
     */
    List<Map<String, Object>> getRescueListByMerchantUserId(Long merchantUserId);
}

