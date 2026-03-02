package com.travel.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.order.TicketOrder;
import com.travel.entity.scenic.ScenicReview;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.mapper.order.TicketOrderMapper;
import com.travel.mapper.scenic.ScenicReviewMapper;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.mapper.user.UserMapper;
import com.travel.service.system.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 数据导出服务实现类
 */
@Slf4j
@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private ScenicStatisticsMapper statisticsMapper;

    @Autowired
    private ScenicSpotMapper scenicSpotMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TicketOrderMapper ticketOrderMapper;

    @Autowired
    private ScenicReviewMapper scenicReviewMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String EXPORT_KEY_PREFIX = "export:task:";
    private static final String EXPORT_INDEX_KEY = "export:task:index";
    private static final long EXPORT_TTL_HOURS = 72; // 导出记录保留72小时

    @Override
    public String exportData(String dataType, List<Long> scenicIds, LocalDate startDate, LocalDate endDate,
                            String granularity, String format, boolean includeRawData,
                            boolean includeAnalytics, boolean includePredictions) {
        log.info("开始导出数据: type={}, scenicIds={}, dateRange={} to {}, format={}",
                dataType, scenicIds, startDate, endDate, format);

        // 生成导出任务ID
        String exportId = "EX-" + System.currentTimeMillis();

        // 获取数据
        List<Map<String, Object>> exportData = fetchExportData(dataType, scenicIds, startDate, endDate, granularity);

        // 添加分析数据
        if (includeAnalytics) {
            addAnalyticsData(exportData, dataType);
        }

        // 添加预测数据
        if (includePredictions) {
            addPredictionData(exportData, scenicIds);
        }

        // 生成文件内容
        byte[] fileContent = generateFileContent(exportData, format, dataType);

        // 获取景区名称
        String scenicName = "所有景区";
        if (scenicIds != null && !scenicIds.isEmpty() && scenicIds.get(0) != null) {
            ScenicSpot scenic = scenicSpotMapper.selectById(scenicIds.get(0));
            if (scenic != null) {
                scenicName = scenic.getName();
            }
        }

        // 创建导出任务记录
        Map<String, Object> task = new HashMap<>();
        task.put("id", exportId);
        task.put("name", generateFileName(dataType, scenicName, startDate, endDate, format));
        task.put("type", getDataTypeLabel(dataType));
        task.put("format", format);
        task.put("size", formatFileSize(fileContent.length));
        task.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        task.put("status", "已完成");
        task.put("fileContent", fileContent);

        // 存储到Redis
        redisTemplate.opsForValue().set(EXPORT_KEY_PREFIX + exportId, task, EXPORT_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForSet().add(EXPORT_INDEX_KEY, exportId);

        log.info("数据导出完成: exportId={}, size={}", exportId, fileContent.length);
        return exportId;
    }

    @Override
    public Map<String, Object> getExportStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Map<String, Object>> allTasks = getAllExportTasks();
        long total = allTasks.size();
        long completed = allTasks.stream().filter(t -> "已完成".equals(t.get("status"))).count();
        long processing = allTasks.stream().filter(t -> "处理中".equals(t.get("status"))).count();
        long failed = allTasks.stream().filter(t -> "失败".equals(t.get("status"))).count();

        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("processing", processing);
        stats.put("failed", failed);

        return stats;
    }

    @Override
    public Map<String, Object> getExportHistory(int pageNum, int pageSize, String keyword) {
        log.info("获取导出历史: page={}, pageSize={}, keyword={}", pageNum, pageSize, keyword);

        List<Map<String, Object>> allRecords = new ArrayList<>(getAllExportTasks());
        
        // 按创建时间降序排序
        allRecords.sort((a, b) -> {
            String timeA = (String) a.get("createTime");
            String timeB = (String) b.get("createTime");
            return timeB.compareTo(timeA);
        });

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.toLowerCase();
            allRecords = allRecords.stream()
                    .filter(r -> {
                        String name = ((String) r.get("name")).toLowerCase();
                        String type = ((String) r.get("type")).toLowerCase();
                        return name.contains(kw) || type.contains(kw);
                    })
                    .collect(Collectors.toList());
        }

        int total = allRecords.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pageData = new ArrayList<>();
        if (fromIndex < total) {
            pageData = allRecords.subList(fromIndex, toIndex);
            // 移除文件内容（不返回给前端）
            pageData = pageData.stream()
                    .map(record -> {
                        Map<String, Object> copy = new HashMap<>(record);
                        copy.remove("fileContent");
                        return copy;
                    })
                    .collect(Collectors.toList());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", pageData);
        result.put("total", total);

        return result;
    }

    @Override
    public byte[] downloadExportFile(String exportId) {
        log.info("下载导出文件: exportId={}", exportId);
        Map<String, Object> task = getExportTask(exportId);
        if (task == null) {
            throw new RuntimeException("导出记录不存在");
        }
        return (byte[]) task.get("fileContent");
    }

    @Override
    public void deleteExportRecord(String exportId) {
        log.info("删除导出记录: exportId={}", exportId);
        redisTemplate.delete(EXPORT_KEY_PREFIX + exportId);
        redisTemplate.opsForSet().remove(EXPORT_INDEX_KEY, exportId);
    }

    @Override
    public int clearExpiredRecords() {
        log.info("清理过期或失败的导出记录");
        int count = 0;
        List<Map<String, Object>> tasks = getAllExportTasks();
        for (Map<String, Object> task : tasks) {
            if (!"已完成".equals(task.get("status"))) {
                String id = (String) task.get("id");
                deleteExportRecord(id);
                count++;
            }
        }
        return count;
    }

    @Override
    public Map<String, Object> getTodayDownloadStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Map<String, Object>> allTasks = getAllExportTasks();
        // 只统计今日的导出记录
        String todayPrefix = LocalDate.now().toString();
        List<Map<String, Object>> todayTasks = allTasks.stream()
                .filter(t -> {
                    String time = (String) t.get("createTime");
                    return time != null && time.startsWith(todayPrefix);
                })
                .collect(java.util.stream.Collectors.toList());

        long excelCount = todayTasks.stream().filter(t -> "excel".equals(t.get("format"))).count();
        long csvCount = todayTasks.stream().filter(t -> "csv".equals(t.get("format"))).count();
        long pdfCount = todayTasks.stream().filter(t -> "pdf".equals(t.get("format"))).count();
        long jsonCount = todayTasks.stream().filter(t -> "json".equals(t.get("format"))).count();

        stats.put("total", excelCount + csvCount + pdfCount + jsonCount);
        stats.put("excel", excelCount);
        stats.put("csv", csvCount);
        stats.put("pdf", pdfCount);
        stats.put("json", jsonCount);

        return stats;
    }

    @Override
    public Map<String, Object> getStorageStats() {
        List<Map<String, Object>> allTasks = getAllExportTasks();
        long totalBytes = allTasks.stream()
                .mapToLong(t -> {
                    Object content = t.get("fileContent");
                    if (content instanceof byte[]) return ((byte[]) content).length;
                    return 0;
                })
                .sum();

        long totalCapacity = 5L * 1024 * 1024 * 1024; // 5GB
        double percentage = (double) totalBytes / totalCapacity * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("used", formatFileSize(totalBytes));
        stats.put("total", "5GB");
        stats.put("percentage", Math.round(percentage * 10) / 10.0);

        return stats;
    }

    // ==================== Redis辅助方法 ====================

    /**
     * 从Redis获取所有导出任务
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAllExportTasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        try {
            Set<Object> ids = redisTemplate.opsForSet().members(EXPORT_INDEX_KEY);
            if (ids != null) {
                for (Object id : ids) {
                    Map<String, Object> task = getExportTask((String) id);
                    if (task != null) {
                        tasks.add(task);
                    } else {
                        // 索引中存在但数据已过期，清理索引
                        redisTemplate.opsForSet().remove(EXPORT_INDEX_KEY, id);
                    }
                }
            }
        } catch (Exception e) {
            log.error("从Redis获取导出任务列表失败", e);
        }
        return tasks;
    }

    /**
     * 从Redis获取单个导出任务
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getExportTask(String exportId) {
        try {
            Object obj = redisTemplate.opsForValue().get(EXPORT_KEY_PREFIX + exportId);
            if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }
        } catch (Exception e) {
            log.error("从Redis获取导出任务失败: exportId={}", exportId, e);
        }
        return null;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取导出数据
     */
    private List<Map<String, Object>> fetchExportData(String dataType, List<Long> scenicIds,
                                                      LocalDate startDate, LocalDate endDate, String granularity) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 构建查询条件
        LambdaQueryWrapper<ScenicStatistics> wrapper = new LambdaQueryWrapper<ScenicStatistics>()
                .between(ScenicStatistics::getStatDate, startDate, endDate)
                .orderByAsc(ScenicStatistics::getStatDate);

        // 如果指定了景区ID且不是"所有景区"
        if (scenicIds != null && !scenicIds.isEmpty() && scenicIds.get(0) != null) {
            wrapper.in(ScenicStatistics::getScenicId, scenicIds);
        }

        List<ScenicStatistics> statistics = statisticsMapper.selectList(wrapper);

        // 获取景区映射
        Map<Long, String> scenicMap = scenicSpotMapper.selectList(null).stream()
                .collect(Collectors.toMap(ScenicSpot::getId, ScenicSpot::getName));

        // 根据数据类型处理数据
        for (ScenicStatistics stat : statistics) {
            Map<String, Object> record = new HashMap<>();
            record.put("日期", stat.getStatDate().toString());
            record.put("景区", scenicMap.getOrDefault(stat.getScenicId(), "未知景区"));

            switch (dataType) {
                case "visitor":
                    record.put("游客数量", stat.getVisitorCount());
                    record.put("订单数量", stat.getOrderCount());
                    record.put("平均停留时间(小时)", stat.getAvgStayTime());
                    record.put("天气", stat.getWeather());
                    record.put("是否周末", stat.getIsWeekend() ? "是" : "否");
                    record.put("是否节假日", stat.getIsHoliday() ? "是" : "否");
                    break;
                case "income":
                    record.put("收入(元)", stat.getRevenue());
                    record.put("订单数量", stat.getOrderCount());
                    record.put("游客数量", stat.getVisitorCount());
                    record.put("客单价(元)", stat.getOrderCount() > 0 ? 
                            stat.getRevenue().divide(new BigDecimal(stat.getOrderCount()), 2, RoundingMode.HALF_UP) : 
                            BigDecimal.ZERO);
                    break;
                case "satisfaction":
                    // 从评价表查询当天的真实评分
                    record.put("游客数量", stat.getVisitorCount());
                    record.put("收藏增长", stat.getFavoritesCount());
                    {
                        LambdaQueryWrapper<ScenicReview> rw = new LambdaQueryWrapper<ScenicReview>()
                                .between(ScenicReview::getCreatedAt,
                                        stat.getStatDate().atStartOfDay(),
                                        stat.getStatDate().plusDays(1).atStartOfDay())
                                .eq(ScenicReview::getStatus, "published");
                        if (stat.getScenicId() != null) rw.eq(ScenicReview::getScenicId, stat.getScenicId());
                        List<ScenicReview> dayReviews = scenicReviewMapper.selectList(rw);
                        double avgRating = dayReviews.stream()
                                .filter(r -> r.getRating() != null)
                                .mapToInt(ScenicReview::getRating)
                                .average().orElse(0.0);
                        record.put("评价数量", dayReviews.size());
                        record.put("平均评分", BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));
                    }
                    break;
                case "source":
                    // 从订单表统计当天的票种分布
                    record.put("游客数量", stat.getVisitorCount());
                    {
                        LambdaQueryWrapper<TicketOrder> ow = new LambdaQueryWrapper<TicketOrder>()
                                .between(TicketOrder::getCreatedAt,
                                        stat.getStatDate().atStartOfDay(),
                                        stat.getStatDate().plusDays(1).atStartOfDay());
                        if (stat.getScenicId() != null) ow.eq(TicketOrder::getScenicId, stat.getScenicId());
                        List<TicketOrder> dayOrders = ticketOrderMapper.selectList(ow);
                        int adult = 0, student = 0, child = 0, elder = 0;
                        for (TicketOrder o : dayOrders) {
                            int cnt = o.getTicketCount() != null ? o.getTicketCount() : 1;
                            switch (o.getTicketType() != null ? o.getTicketType() : "adult") {
                                case "child": child += cnt; break;
                                case "student": student += cnt; break;
                                case "elder": elder += cnt; break;
                                default: adult += cnt; break;
                            }
                        }
                        record.put("成人票", adult);
                        record.put("学生票", student);
                        record.put("儿童票", child);
                        record.put("老人票", elder);
                    }
                    break;
                case "prediction":
                    record.put("实际游客数", stat.getVisitorCount());
                    record.put("订单数", stat.getOrderCount());
                    record.put("收入(元)", stat.getRevenue());
                    record.put("天气", stat.getWeather());
                    record.put("是否周末", stat.getIsWeekend() != null && stat.getIsWeekend() ? "是" : "否");
                    break;
                default:
                    record.put("游客数量", stat.getVisitorCount());
                    record.put("收入", stat.getRevenue());
            }

            result.add(record);
        }

        // 根据粒度聚合数据
        if (!"day".equals(granularity)) {
            result = aggregateByGranularity(result, granularity);
        }

        return result;
    }

    /**
     * 添加分析数据
     */
    private void addAnalyticsData(List<Map<String, Object>> exportData, String dataType) {
        if (exportData.isEmpty()) {
            return;
        }

        // 计算汇总统计
        Map<String, Object> summary = new HashMap<>();
        summary.put("日期", "汇总统计");
        summary.put("景区", "全部");

        if ("visitor".equals(dataType)) {
            int totalVisitors = exportData.stream()
                    .mapToInt(d -> (Integer) d.getOrDefault("游客数量", 0))
                    .sum();
            int totalOrders = exportData.stream()
                    .mapToInt(d -> (Integer) d.getOrDefault("订单数量", 0))
                    .sum();
            summary.put("游客数量", totalVisitors);
            summary.put("订单数量", totalOrders);
            summary.put("平均每日游客", totalVisitors / exportData.size());
        } else if ("income".equals(dataType)) {
            BigDecimal totalIncome = exportData.stream()
                    .map(d -> (BigDecimal) d.getOrDefault("收入(元)", BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.put("收入(元)", totalIncome);
            summary.put("平均每日收入", totalIncome.divide(new BigDecimal(exportData.size()), 2, RoundingMode.HALF_UP));
        }

        exportData.add(summary);
    }

    /**
     * 添加预测数据（基于历史同期均值推算）
     */
    private void addPredictionData(List<Map<String, Object>> exportData, List<Long> scenicIds) {
        LocalDate today = LocalDate.now();

        // 查询最近30天历史数据计算基准
        LambdaQueryWrapper<ScenicStatistics> histWrapper = new LambdaQueryWrapper<ScenicStatistics>()
                .between(ScenicStatistics::getStatDate, today.minusDays(30), today)
                .orderByAsc(ScenicStatistics::getStatDate);
        if (scenicIds != null && !scenicIds.isEmpty() && scenicIds.get(0) != null) {
            histWrapper.in(ScenicStatistics::getScenicId, scenicIds);
        }
        List<ScenicStatistics> histStats = statisticsMapper.selectList(histWrapper);

        // 计算基准日均游客数
        int avgDailyVisitors = histStats.isEmpty() ? 0 :
                (int) histStats.stream().mapToInt(ScenicStatistics::getVisitorCount).average().orElse(0);

        // 周末/工作日分别计算均值
        int avgWeekday = histStats.isEmpty() ? 0 :
                (int) histStats.stream()
                    .filter(s -> s.getIsWeekend() == null || !s.getIsWeekend())
                    .mapToInt(ScenicStatistics::getVisitorCount).average().orElse(avgDailyVisitors);
        int avgWeekend = histStats.isEmpty() ? 0 :
                (int) histStats.stream()
                    .filter(s -> s.getIsWeekend() != null && s.getIsWeekend())
                    .mapToInt(ScenicStatistics::getVisitorCount).average().orElse(avgDailyVisitors);

        for (int i = 1; i <= 7; i++) {
            LocalDate predictDate = today.plusDays(i);
            boolean isWeekend = predictDate.getDayOfWeek().getValue() >= 6;
            int predictedVisitors = isWeekend ? avgWeekend : avgWeekday;

            Map<String, Object> prediction = new HashMap<>();
            prediction.put("日期", predictDate.toString() + " (预测)");
            prediction.put("景区", "全部");
            prediction.put("预测游客数", predictedVisitors);
            prediction.put("数据来源", histStats.isEmpty() ? "无历史数据" : "基于近30天历史均值");
            exportData.add(prediction);
        }
    }

    /**
     * 根据粒度聚合数据
     */
    private List<Map<String, Object>> aggregateByGranularity(List<Map<String, Object>> data, String granularity) {
        // 简化实现：仅处理周和月粒度
        if ("week".equals(granularity)) {
            // 按周聚合
            Map<String, List<Map<String, Object>>> weeklyGroups = new LinkedHashMap<>();
            for (Map<String, Object> record : data) {
                String dateStr = (String) record.get("日期");
                LocalDate date = LocalDate.parse(dateStr);
                String weekKey = date.getYear() + "-W" + date.getDayOfWeek().getValue() / 7;
                weeklyGroups.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(record);
            }
            return aggregateGroups(weeklyGroups);
        } else if ("month".equals(granularity)) {
            // 按月聚合
            Map<String, List<Map<String, Object>>> monthlyGroups = new LinkedHashMap<>();
            for (Map<String, Object> record : data) {
                String dateStr = (String) record.get("日期");
                LocalDate date = LocalDate.parse(dateStr);
                String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                monthlyGroups.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(record);
            }
            return aggregateGroups(monthlyGroups);
        }
        return data;
    }

    /**
     * 聚合分组数据
     */
    private List<Map<String, Object>> aggregateGroups(Map<String, List<Map<String, Object>>> groups) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("日期", entry.getKey());
            
            List<Map<String, Object>> groupData = entry.getValue();
            if (!groupData.isEmpty()) {
                aggregated.put("景区", groupData.get(0).get("景区"));
                
                // 聚合数值字段
                if (groupData.get(0).containsKey("游客数量")) {
                    int totalVisitors = groupData.stream()
                            .mapToInt(d -> (Integer) d.getOrDefault("游客数量", 0))
                            .sum();
                    aggregated.put("游客数量", totalVisitors);
                }
                
                if (groupData.get(0).containsKey("收入(元)")) {
                    BigDecimal totalIncome = groupData.stream()
                            .map(d -> (BigDecimal) d.getOrDefault("收入(元)", BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    aggregated.put("收入(元)", totalIncome);
                }
            }
            
            result.add(aggregated);
        }
        return result;
    }

    /**
     * 生成文件内容
     */
    private byte[] generateFileContent(List<Map<String, Object>> data, String format, String dataType) {
        switch (format.toLowerCase()) {
            case "csv":
                return generateCSV(data);
            case "json":
                return generateJSON(data);
            case "excel":
            case "pdf":
                // Excel和PDF生成需要额外的库支持，这里简化为CSV格式
                return generateCSV(data);
            default:
                return generateCSV(data);
        }
    }

    /**
     * 生成CSV内容
     */
    private byte[] generateCSV(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return new byte[0];
        }

        StringBuilder csv = new StringBuilder();
        
        // 添加UTF-8 BOM以支持Excel正确显示中文
        csv.append('\ufeff');
        
        // 表头
        Set<String> headers = data.get(0).keySet();
        csv.append(String.join(",", headers)).append("\n");

        // 数据行
        for (Map<String, Object> record : data) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object value = record.get(header);
                String valueStr = value != null ? value.toString() : "";
                // 处理包含逗号的值
                if (valueStr.contains(",")) {
                    valueStr = "\"" + valueStr + "\"";
                }
                values.add(valueStr);
            }
            csv.append(String.join(",", values)).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成JSON内容
     */
    private byte[] generateJSON(List<Map<String, Object>> data) {
        // 简单的JSON序列化（实际项目中应使用Jackson等库）
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"data\": [\n");
        
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> record = data.get(i);
            json.append("    {\n");
            
            int j = 0;
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                json.append("      \"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value).append("\"");
                } else {
                    json.append(value);
                }
                if (j < record.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
                j++;
            }
            
            json.append("    }");
            if (i < data.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ],\n");
        json.append("  \"total\": ").append(data.size()).append("\n");
        json.append("}\n");

        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String dataType, String scenicName, LocalDate startDate, LocalDate endDate, String format) {
        String typeLabel = getDataTypeLabel(dataType);
        String dateRange = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                          "-" + 
                          endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String extension = getFileExtension(format);
        
        return String.format("%s-%s-%s.%s", scenicName, typeLabel, dateRange, extension);
    }

    /**
     * 获取数据类型标签
     */
    private String getDataTypeLabel(String dataType) {
        switch (dataType) {
            case "visitor": return "游客流量数据";
            case "income": return "收入数据";
            case "satisfaction": return "满意度数据";
            case "source": return "游客来源数据";
            case "prediction": return "预测数据";
            default: return "数据";
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "excel": return "xlsx";
            case "csv": return "csv";
            case "pdf": return "pdf";
            case "json": return "json";
            default: return "txt";
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return String.format("%.1fMB", bytes / (1024.0 * 1024));
        }
    }
}

