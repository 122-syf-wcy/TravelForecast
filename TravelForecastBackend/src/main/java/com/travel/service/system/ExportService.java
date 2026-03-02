package com.travel.service.system;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据导出服务接口
 */
public interface ExportService {

    /**
     * 导出数据
     * @param dataType 数据类型 (visitor, income, satisfaction, source, prediction)
     * @param scenicIds 景区ID列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param granularity 数据粒度 (hour, day, week, month)
     * @param format 导出格式 (excel, csv, pdf, json)
     * @param includeRawData 是否包含原始数据
     * @param includeAnalytics 是否包含分析结果
     * @param includePredictions 是否包含预测数据
     * @return 导出任务ID
     */
    String exportData(
        String dataType,
        List<Long> scenicIds,
        LocalDate startDate,
        LocalDate endDate,
        String granularity,
        String format,
        boolean includeRawData,
        boolean includeAnalytics,
        boolean includePredictions
    );

    /**
     * 获取导出任务统计
     * @return 包含总数、已完成、处理中、失败的统计数据
     */
    Map<String, Object> getExportStats();

    /**
     * 获取导出历史记录
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 包含分页数据的Map
     */
    Map<String, Object> getExportHistory(int pageNum, int pageSize, String keyword);

    /**
     * 下载导出文件
     * @param exportId 导出ID
     * @return 文件字节数组
     */
    byte[] downloadExportFile(String exportId);

    /**
     * 删除导出记录
     * @param exportId 导出ID
     */
    void deleteExportRecord(String exportId);

    /**
     * 清理过期或失败的导出记录
     * @return 清理的记录数
     */
    int clearExpiredRecords();

    /**
     * 获取今日下载统计
     * @return 包含总下载量及各格式下载量的Map
     */
    Map<String, Object> getTodayDownloadStats();

    /**
     * 获取存储用量统计
     * @return 包含已用、总计、百分比的Map
     */
    Map<String, Object> getStorageStats();
}

