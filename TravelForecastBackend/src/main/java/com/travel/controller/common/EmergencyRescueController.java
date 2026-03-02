package com.travel.controller.common;

import com.travel.entity.common.EmergencyRescue;
import com.travel.service.common.EmergencyRescueService;
import com.travel.utils.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 紧急救援控制器
 */
@Slf4j
@RestController
@RequestMapping("/emergency-rescue")
@RequiredArgsConstructor
public class EmergencyRescueController {

    private final EmergencyRescueService emergencyRescueService;
    private final JwtUtil jwtUtil;

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("未登录或token无效");
    }

    /**
     * 创建救援请求（用户端）
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRescue(@RequestBody EmergencyRescue rescue,
                                                             HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            rescue.setUserId(userId);
            
            boolean success = emergencyRescueService.createRescue(rescue);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? 200 : 500);
            response.put("message", success ? "救援请求已提交，工作人员将尽快处理" : "提交失败");
            response.put("data", rescue);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "提交失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取用户的救援记录（用户端）
     */
    @GetMapping("/my-rescues")
    public ResponseEntity<Map<String, Object>> getMyRescues(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> rescues = emergencyRescueService.getRescueListByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", rescues);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取景区的救援列表（商家端）
     */
    @GetMapping("/scenic/{scenicId}")
    public ResponseEntity<Map<String, Object>> getRescuesByScenicId(@PathVariable Long scenicId) {
        try {
            List<Map<String, Object>> rescues = emergencyRescueService.getRescueListByScenicId(scenicId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", rescues);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 商家获取其管理的景区的所有救援列表（商家端）
     */
    @GetMapping("/merchant/list")
    public ResponseEntity<Map<String, Object>> getMerchantRescues(HttpServletRequest request) {
        log.info("========== 接收商家救援列表请求 ==========");
        try {
            Long userId = getUserIdFromRequest(request);
            log.info("商家用户ID: {}", userId);
            
            List<Map<String, Object>> rescues = emergencyRescueService.getRescueListByMerchantUserId(userId);
            log.info("成功获取救援列表，共 {} 条记录", rescues.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", rescues);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取商家救援列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
            response.put("error", e.getClass().getName());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 处理救援请求（商家端）
     */
    @PostMapping("/handle/{rescueId}")
    public ResponseEntity<Map<String, Object>> handleRescue(@PathVariable Long rescueId,
                                                             @RequestBody Map<String, String> params,
                                                             HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            String handlerName = params.get("handlerName");
            
            boolean success = emergencyRescueService.handleRescue(rescueId, userId, handlerName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? 200 : 500);
            response.put("message", success ? "已接受处理" : "处理失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 完成救援请求（商家端）
     */
    @PostMapping("/complete/{rescueId}")
    public ResponseEntity<Map<String, Object>> completeRescue(@PathVariable Long rescueId,
                                                               @RequestBody Map<String, String> params) {
        try {
            String handleNotes = params.get("handleNotes");
            
            boolean success = emergencyRescueService.completeRescue(rescueId, handleNotes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? 200 : 500);
            response.put("message", success ? "救援已完成" : "操作失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 取消救援请求（用户端或商家端）
     */
    @PostMapping("/cancel/{rescueId}")
    public ResponseEntity<Map<String, Object>> cancelRescue(@PathVariable Long rescueId) {
        try {
            boolean success = emergencyRescueService.cancelRescue(rescueId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? 200 : 500);
            response.put("message", success ? "已取消" : "操作失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取景区救援统计信息（商家端）
     */
    @GetMapping("/stats/{scenicId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long scenicId) {
        try {
            Map<String, Object> stats = emergencyRescueService.getStatsByScenicId(scenicId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取救援详情
     */
    @GetMapping("/detail/{rescueId}")
    public ResponseEntity<Map<String, Object>> getRescueDetail(@PathVariable Long rescueId) {
        try {
            EmergencyRescue rescue = emergencyRescueService.getById(rescueId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", rescue);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除救援记录（商家端）
     */
    @DeleteMapping("/delete/{rescueId}")
    public ResponseEntity<Map<String, Object>> deleteRescue(@PathVariable Long rescueId) {
        try {
            boolean success = emergencyRescueService.removeById(rescueId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? 200 : 500);
            response.put("message", success ? "删除成功" : "删除失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

