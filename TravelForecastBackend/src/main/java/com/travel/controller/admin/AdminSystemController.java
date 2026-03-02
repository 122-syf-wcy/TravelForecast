package com.travel.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.OperationLog;
import com.travel.common.Result;
import com.travel.entity.system.SystemLog;
import com.travel.mapper.system.SystemLogMapper;
import com.travel.service.system.SystemConfigService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final SystemConfigService systemConfigService;
    private final SystemLogMapper systemLogMapper;

    /**
     * 获取所有系统配置
     * GET /admin/system/config
     */
    @GetMapping("/config")
    public Result<Map<String, String>> getSystemConfig() {
        Map<String, String> config = systemConfigService.getAllConfigs();
        return Result.success(config);
    }

    /**
     * 保存系统配置
     * POST /admin/system/config
     */
    @OperationLog(module = "系统设置", description = "修改系统配置")
    @PostMapping("/config")
    public Result<Void> saveSystemConfig(@RequestBody Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            systemConfigService.setConfig(entry.getKey(), entry.getValue());
        }
        log.info("系统配置已更新");
        return Result.success(null, "系统配置保存成功");
    }

    /**
     * 分页查询系统日志
     * GET /admin/system/logs?page=1&size=10&level=info&startTime=xxx&endTime=xxx
     */
    @GetMapping("/logs")
    public Result<Page<SystemLog>> getSystemLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        Page<SystemLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();

        if (level != null && !level.isEmpty()) {
            wrapper.eq(SystemLog::getLogLevel, level);
        }

        if (module != null && !module.isEmpty()) {
            wrapper.eq(SystemLog::getModule, module);
        }

        if (startTime != null) {
            wrapper.ge(SystemLog::getLogTime, startTime);
        }

        if (endTime != null) {
            wrapper.le(SystemLog::getLogTime, endTime);
        }

        wrapper.orderByDesc(SystemLog::getLogTime);

        Page<SystemLog> result = systemLogMapper.selectPage(pageObj, wrapper);
        return Result.success(result);
    }

    /**
     * 添加系统日志（内部使用）
     * POST /admin/system/logs
     */
    @PostMapping("/logs")
    public Result<Void> addSystemLog(@RequestBody SystemLog systemLog) {
        if (systemLog.getLogTime() == null) {
            systemLog.setLogTime(LocalDateTime.now());
        }
        systemLogMapper.insert(systemLog);
        return Result.success(null, "日志已记录");
    }

    /**
     * 导出系统日志
     * GET /admin/system/logs/export
     */
    @GetMapping("/logs/export")
    public Result<Map<String, Object>> exportLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();

        if (level != null && !level.isEmpty()) {
            wrapper.eq(SystemLog::getLogLevel, level);
        }

        if (startTime != null) {
            wrapper.ge(SystemLog::getLogTime, startTime);
        }

        if (endTime != null) {
            wrapper.le(SystemLog::getLogTime, endTime);
        }

        wrapper.orderByDesc(SystemLog::getLogTime);
        wrapper.last("LIMIT 10000"); // 限制最多导出10000条

        java.util.List<SystemLog> logs = systemLogMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("count", logs.size());
        result.put("exportTime", LocalDateTime.now());

        return Result.success(result);
    }

    /**
     * 获取系统统计信息
     * GET /admin/system/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        // 统计各级别日志数量
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemLog::getLogLevel, "info");
        long infoCount = systemLogMapper.selectCount(wrapper);

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemLog::getLogLevel, "warning");
        long warningCount = systemLogMapper.selectCount(wrapper);

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemLog::getLogLevel, "error");
        long errorCount = systemLogMapper.selectCount(wrapper);

        stats.put("infoCount", infoCount);
        stats.put("warningCount", warningCount);
        stats.put("errorCount", errorCount);
        stats.put("totalCount", infoCount + warningCount + errorCount);

        return Result.success(stats);
    }
}

