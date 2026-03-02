package com.travel.controller.system;

import com.travel.service.system.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    /**
     * 导出数据
     */
    @PostMapping("/data")
    public Map<String, Object> exportData(@RequestBody Map<String, Object> params) {
        log.info("接收数据导出请求: {}", params);
        
        Map<String, Object> result = new HashMap<>();
        try {
            String dataType = (String) params.get("dataType");
            List<Long> scenicIds = (List<Long>) params.get("scenicIds");
            String startDateStr = (String) params.get("startDate");
            String endDateStr = (String) params.get("endDate");
            String granularity = (String) params.get("granularity");
            String format = (String) params.get("format");
            Boolean includeRawData = (Boolean) params.getOrDefault("includeRawData", true);
            Boolean includeAnalytics = (Boolean) params.getOrDefault("includeAnalytics", false);
            Boolean includePredictions = (Boolean) params.getOrDefault("includePredictions", false);

            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            String exportId = exportService.exportData(
                    dataType, scenicIds, startDate, endDate, granularity, format,
                    includeRawData, includeAnalytics, includePredictions
            );

            result.put("code", 200);
            result.put("message", "数据导出任务已创建");
            result.put("data", exportId);
        } catch (Exception e) {
            log.error("数据导出失败", e);
            result.put("code", 500);
            result.put("message", "数据导出失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取导出统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getExportStats() {
        log.info("获取导出统计");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = exportService.getExportStats();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取导出统计失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取导出历史
     */
    @GetMapping("/history")
    public Map<String, Object> getExportHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        log.info("获取导出历史: page={}, pageSize={}, keyword={}", pageNum, pageSize, keyword);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = exportService.getExportHistory(pageNum, pageSize, keyword);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取导出历史失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 下载导出文件
     */
    @GetMapping("/download/{exportId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String exportId) {
        log.info("下载导出文件: exportId={}", exportId);
        try {
            // 获取导出历史以获取文件名
            Map<String, Object> historyData = exportService.getExportHistory(1, 1000, null);
            List<Map<String, Object>> records = (List<Map<String, Object>>) historyData.get("data");
            
            String fileName = "export.csv";
            String format = "csv";
            
            for (Map<String, Object> record : records) {
                if (exportId.equals(record.get("id"))) {
                    fileName = (String) record.get("name");
                    format = (String) record.get("format");
                    break;
                }
            }

            byte[] fileContent = exportService.downloadExportFile(exportId);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            
            // 根据格式设置Content-Type
            switch (format.toLowerCase()) {
                case "excel":
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    break;
                case "csv":
                    headers.setContentType(MediaType.parseMediaType("text/csv"));
                    break;
                case "pdf":
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    break;
                case "json":
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    break;
                default:
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            // URL编码文件名以支持中文
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.badRequest().body(("下载失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 删除导出记录
     */
    @DeleteMapping("/{exportId}")
    public Map<String, Object> deleteExportRecord(@PathVariable String exportId) {
        log.info("删除导出记录: exportId={}", exportId);
        Map<String, Object> result = new HashMap<>();
        try {
            exportService.deleteExportRecord(exportId);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除导出记录失败", e);
            result.put("code", 500);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 清理过期记录
     */
    @DeleteMapping("/clear-expired")
    public Map<String, Object> clearExpiredRecords() {
        log.info("清理过期或失败的导出记录");
        Map<String, Object> result = new HashMap<>();
        try {
            int count = exportService.clearExpiredRecords();
            result.put("code", 200);
            result.put("message", "已清理 " + count + " 条记录");
            result.put("data", count);
        } catch (Exception e) {
            log.error("清理过期记录失败", e);
            result.put("code", 500);
            result.put("message", "清理失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取今日下载统计
     */
    @GetMapping("/download-stats")
    public Map<String, Object> getDownloadStats() {
        log.info("获取今日下载统计");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = exportService.getTodayDownloadStats();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取下载统计失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取存储用量统计
     */
    @GetMapping("/storage-stats")
    public Map<String, Object> getStorageStats() {
        log.info("获取存储用量统计");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = exportService.getStorageStats();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取存储统计失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}

