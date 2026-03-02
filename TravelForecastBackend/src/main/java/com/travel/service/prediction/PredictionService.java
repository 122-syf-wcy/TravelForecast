package com.travel.service.prediction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 客流预测服务接口
 */
public interface PredictionService {

    /**
     * 预测未来7天总客流
     * 
     * @param model 预测模型类型（dual_stream/lstm/arima），默认dual_stream
     * @return 包含总客流量、增长率、准确度的预测数据
     */
    Map<String, Object> predictNext7DaysTotal(String model);

    /**
     * 预测景区客流趋势
     * 
     * @param scenicId 景区ID，null表示全部景区
     * @param model    预测模型类型（hybrid/lstm/arima），默认hybrid
     * @param days     预测天数（默认7天）
     * @param factors  预测因子（如weather,holiday）
     * @return 包含日期、预测值、置信度的趋势数据
     */
    Map<String, Object> predictScenicTrend(Long scenicId, String model, Integer days, List<String> factors);

    /**
     * 预测小时客流分布
     * 
     * @param date     预测日期
     * @param scenicId 景区ID，null表示全部景区
     * @param model    预测模型类型（dual_stream/lstm/arima），默认dual_stream
     * @return 包含小时时段和对应客流量的分布数据
     */
    Map<String, Object> predictHourlyDistribution(LocalDate date, Long scenicId, String model);

    /**
     * 获取高峰预警信息
     * 
     * @return 包含预警数量、高风险景区、预警等级的预警数据
     */
    Map<String, Object> getPeakWarning();

    /**
     * 获取预测详情列表（分页）
     * 
     * @param page     页码
     * @param pageSize 每页大小
     * @return 包含分页预测详情的数据
     */
    Map<String, Object> getPredictionDetails(int page, int pageSize);

    /**
     * 获取最佳游览时段建议
     * 
     * @return 包含最佳时段、拥挤度、等待时间的建议数据
     */
    Map<String, Object> getBestVisitTime();
}
