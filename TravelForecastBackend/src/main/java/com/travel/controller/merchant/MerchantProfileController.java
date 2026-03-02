package com.travel.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.service.merchant.MerchantProfileService;
import com.travel.service.scenic.ScenicSpotService;
import com.travel.utils.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/merchant")
public class MerchantProfileController {

    @Autowired
    private MerchantProfileService merchantProfileService;
    
    @Autowired
    private ScenicSpotService scenicSpotService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前商家的景区信息
     */
    @GetMapping("/profile/my-scenic")
    public Result<Map<String, Object>> getMyScenicInfo(HttpServletRequest request) {
        try {
            // 从token获取用户ID
            String token = request.getHeader("Authorization");
            if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 获取商家资料
            MerchantProfile profile = merchantProfileService.getByUserId(userId);
            if (profile == null || profile.getScenicId() == null) {
                return Result.error("未绑定景区");
            }
            
            // 获取景区信息
            ScenicSpot scenic = scenicSpotService.getById(profile.getScenicId());
            if (scenic == null) {
                return Result.error("景区不存在");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("scenicId", scenic.getId());
            result.put("scenicName", scenic.getName());
            result.put("businessName", profile.getBusinessName());
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取景区信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/profiles/by-user/{userId}")
    public Result<MerchantProfile> getByUser(@PathVariable Long userId) {
        MerchantProfile mp = merchantProfileService.getByUserId(userId);
        return Result.success(mp);
    }

    @PostMapping("/profiles")
    public Result<MerchantProfile> save(@RequestBody MerchantProfile profile) {
        return Result.success(merchantProfileService.saveOrUpdate(profile));
    }

    @GetMapping("/profiles")
    public Result<Page<MerchantProfile>> page(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(merchantProfileService.page(pageNum, pageSize, keyword));
    }
}


