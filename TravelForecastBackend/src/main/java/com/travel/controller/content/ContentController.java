package com.travel.controller.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.system.SiteConfig;
import com.travel.mapper.system.SiteConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {
    private final SiteConfigMapper siteConfigMapper;

    @GetMapping("/landing")
    public Result<Map<String, Object>> getLanding(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        SiteConfig sc = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfig>()
                .eq(SiteConfig::getConfigKey, SiteConfig.KEY_LANDING)
                .last("limit 1"));
        Map<String, Object> data = sc == null ? Map.of() : com.alibaba.fastjson2.JSON.parseObject(sc.getConfigJson());
        org.slf4j.LoggerFactory.getLogger(ContentController.class).info("traceId={} GET /content/landing hit, exist={}", traceId, sc != null);
        return Result.success(data);
    }

    @PostMapping("/landing")
    public Result<Void> saveLandingPost(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return doSaveLanding(body, request);
    }

    @PutMapping("/landing")
    public Result<Void> saveLandingPut(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return doSaveLanding(body, request);
    }

    private Result<Void> doSaveLanding(Map<String, Object> body, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        String json = com.alibaba.fastjson2.JSON.toJSONString(body);
        SiteConfig exist = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfig>()
                .eq(SiteConfig::getConfigKey, SiteConfig.KEY_LANDING)
                .last("limit 1"));
        if (exist == null) {
            SiteConfig sc = new SiteConfig();
            sc.setConfigKey(SiteConfig.KEY_LANDING);
            sc.setConfigJson(json);
            sc.setCreatedAt(LocalDateTime.now());
            sc.setUpdatedAt(LocalDateTime.now());
            siteConfigMapper.insert(sc);
        } else {
            exist.setConfigJson(json);
            exist.setUpdatedAt(LocalDateTime.now());
            siteConfigMapper.updateById(exist);
        }
        org.slf4j.LoggerFactory.getLogger(ContentController.class).info("traceId={} SAVE /content/landing ok", traceId);
        return Result.success();
    }
}


