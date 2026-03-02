package com.travel.controller.admin;

import com.travel.service.admin.AdminDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员端总览控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {
    
    @Autowired
    private AdminDashboardService dashboardService;
    
    /**
     * 获取总览统计数据
     */
    @GetMapping("/overview/statistics")
    public Map<String, Object> getOverviewStatistics() {
        log.info("获取总览统计数据");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getOverviewStatistics();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取总览统计数据失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取平台运营趋势数据
     */
    @GetMapping("/trend")
    public Map<String, Object> getTrendData(@RequestParam(defaultValue = "week") String timeRange) {
        log.info("获取平台运营趋势: timeRange={}", timeRange);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getTrendData(timeRange);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取运营趋势失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取用户分布数据
     */
    @GetMapping("/distribution")
    public Map<String, Object> getDistributionData(
            @RequestParam(defaultValue = "region") String type) {
        log.info("获取用户分布数据: type={}", type);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getDistributionData(type);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取用户分布失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取热门景区排行
     */
    @GetMapping("/scenic/rankings")
    public Map<String, Object> getHotScenicRankings() {
        log.info("获取热门景区排行");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getHotScenicRankings();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取景区排行失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取管理员通知
     */
    @GetMapping("/notices")
    public Map<String, Object> getAdminNotices() {
        log.info("获取管理员通知");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getAdminNotices();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取通知失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取待办事项
     */
    @GetMapping("/tasks")
    public Map<String, Object> getPendingTasks() {
        log.info("获取待办事项");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getPendingTasks();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取待办事项失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取平台活动日志
     */
    @GetMapping("/activity-logs")
    public Map<String, Object> getActivityLogs() {
        log.info("获取平台活动日志");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getActivityLogs();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取活动日志失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 标记待办事项为已处理
     */
    @PostMapping("/tasks/{taskId}/process")
    public Map<String, Object> markTaskAsProcessed(
            @PathVariable Long taskId,
            @RequestAttribute(required = false) String username) {
        log.info("标记待办事项为已处理: taskId={}, operator={}", taskId, username);
        Map<String, Object> result = new HashMap<>();
        try {
            String processedBy = username != null ? username : "admin";
            dashboardService.markTaskAsProcessed(taskId, processedBy);
            result.put("code", 200);
            result.put("message", "处理成功");
        } catch (Exception e) {
            log.error("处理待办事项失败", e);
            result.put("code", 500);
            result.put("message", "处理失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 创建待办事项
     */
    @PostMapping("/tasks")
    public Map<String, Object> createTask(@RequestBody Map<String, String> request,
            @RequestAttribute(required = false) String username) {
        String title = request.get("title");
        String description = request.get("description");
        String submitter = username != null ? username : "admin";
        
        log.info("创建待办事项: title={}, submitter={}", title, submitter);
        Map<String, Object> result = new HashMap<>();
        try {
            if (title == null || title.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "标题不能为空");
                return result;
            }
            
            dashboardService.createTask(title, description, submitter);
            result.put("code", 200);
            result.put("message", "创建成功");
        } catch (Exception e) {
            log.error("创建待办事项失败", e);
            result.put("code", 500);
            result.put("message", "创建失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 删除待办事项
     */
    @DeleteMapping("/tasks/{taskId}")
    public Map<String, Object> deleteTask(@PathVariable Long taskId) {
        log.info("删除待办事项: taskId={}", taskId);
        Map<String, Object> result = new HashMap<>();
        try {
            dashboardService.deleteTask(taskId);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除待办事项失败", e);
            result.put("code", 500);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取景区热度排行TOP10（数据分析页面）
     */
    @GetMapping("/analytics/scenic-hot-rankings")
    public Map<String, Object> getScenicHotRankings() {
        log.info("获取景区热度排行");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getScenicHotRankings();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取景区热度排行失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取用户活跃度分布
     */
    @GetMapping("/analytics/user-activity")
    public Map<String, Object> getUserActivityDistribution() {
        log.info("获取用户活跃度分布");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getUserActivityDistribution();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取用户活跃度分布失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取核心指标数据
     */
    @GetMapping("/analytics/metrics")
    public Map<String, Object> getAnalyticsMetrics(@RequestParam(defaultValue = "month") String timeRange) {
        log.info("获取核心指标数据，时间范围: {}", timeRange);
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = dashboardService.getAnalyticsMetrics(timeRange);
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取核心指标数据失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }
}

