package com.travel.controller.merchant;

import com.travel.common.Result;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.service.merchant.MerchantProfileService;
import com.travel.service.scenic.ScenicSpotService;
import com.travel.utils.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 商家端-子景点管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/sub-spots")
@RequiredArgsConstructor
public class MerchantSubSpotController {

    private final ScenicSpotService scenicSpotService;
    private final MerchantProfileService merchantProfileService;
    private final JwtUtil jwtUtil;

    /**
     * 获取商家主景区下的子景点列表
     */
    @GetMapping
    public Result<List<ScenicSpot>> list(HttpServletRequest request) {
        Long scenicId = getMerchantScenicId(request);
        if (scenicId == null) {
            return Result.error("未绑定景区");
        }
        List<ScenicSpot> subSpots = scenicSpotService.getSubSpots(scenicId);
        log.info("商家查询子景点列表, scenicId={}, count={}", scenicId, subSpots.size());
        return Result.success(subSpots);
    }

    /**
     * 新增子景点
     */
    @PostMapping
    public Result<ScenicSpot> create(@RequestBody ScenicSpot subSpot, HttpServletRequest request) {
        Long scenicId = getMerchantScenicId(request);
        if (scenicId == null) {
            return Result.error("未绑定景区");
        }
        ScenicSpot created = scenicSpotService.createSubSpot(scenicId, subSpot);
        log.info("商家新增子景点: {}", created.getName());
        return Result.success(created);
    }

    /**
     * 更新子景点
     */
    @PutMapping("/{id}")
    public Result<ScenicSpot> update(@PathVariable Long id, @RequestBody ScenicSpot subSpot, HttpServletRequest request) {
        Long scenicId = getMerchantScenicId(request);
        if (scenicId == null) {
            return Result.error("未绑定景区");
        }
        ScenicSpot existing = scenicSpotService.getById(id);
        if (existing == null || !scenicId.equals(existing.getParentId())) {
            return Result.error("无权操作该子景点");
        }
        subSpot.setId(id);
        subSpot.setParentId(scenicId);
        ScenicSpot updated = scenicSpotService.update(subSpot);
        return Result.success(updated);
    }

    /**
     * 删除子景点
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long scenicId = getMerchantScenicId(request);
        if (scenicId == null) {
            return Result.error("未绑定景区");
        }
        ScenicSpot existing = scenicSpotService.getById(id);
        if (existing == null || !scenicId.equals(existing.getParentId())) {
            return Result.error("无权操作该子景点");
        }
        scenicSpotService.delete(id);
        return Result.success(null);
    }

    /**
     * 从JWT Token中获取商家绑定的景区ID
     */
    private Long getMerchantScenicId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return null;
        }
        token = token.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        MerchantProfile profile = merchantProfileService.getByUserId(userId);
        return profile != null ? profile.getScenicId() : null;
    }
}
