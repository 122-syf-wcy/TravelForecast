package com.travel.ai.service;

import com.travel.ai.dto.EducationRequest;
import com.travel.ai.entity.StudyRoute;

import java.util.List;
import java.util.Map;

/**
 * 研学教育服务接口
 */
public interface EducationService {

    /**
     * 获取研学路线列表
     */
    List<StudyRoute> listRoutes(String ageGroup, String theme);

    /**
     * 获取研学路线详情
     */
    StudyRoute getRouteDetail(Long id);

    /**
     * AI生成研学方案
     */
    Map<String, Object> generateStudyPlan(EducationRequest request);

    /**
     * 添加研学路线
     */
    StudyRoute addRoute(StudyRoute route);

    /**
     * 更新研学路线
     */
    StudyRoute updateRoute(StudyRoute route);
}
