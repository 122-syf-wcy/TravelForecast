package com.travel.controller.prediction;

import com.travel.service.prediction.PolicySimulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 政策沙盘模拟控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/policy")
public class PolicySimulationController {
    
    @Autowired
    private PolicySimulationService policySimulationService;
    
    /**
     * 模拟政策效果
     * @param discount 联票折扣率（%）
     * @param subsidy 交通补贴（元/人）
     * @param capacity 容量上限（人/日）
     */
    @GetMapping("/simulate")
    public Map<String, Object> simulatePolicyEffect(
            @RequestParam(defaultValue = "10") double discount,
            @RequestParam(defaultValue = "20") double subsidy,
            @RequestParam(defaultValue = "8000") int capacity) {
        
        log.info("接收政策模拟请求: discount={}%, subsidy={}元, capacity={}人", discount, subsidy, capacity);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> data = policySimulationService.simulatePolicyEffect(discount, subsidy, capacity);
            result.put("code", 200);
            result.put("message", "模拟成功");
            result.put("data", data);
        } catch (Exception e) {
            log.error("政策模拟失败", e);
            result.put("code", 500);
            result.put("message", "模拟失败: " + e.getMessage());
        }
        
        return result;
    }
}

