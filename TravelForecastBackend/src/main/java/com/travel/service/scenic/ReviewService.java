package com.travel.service.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.ScenicReview;
import java.util.Map;

/**
 * 评价Service接口
 */
public interface ReviewService {
    
    /**
     * 获取景区评价列表（分页）
     */
    Page<Map<String, Object>> getReviews(Long scenicId, int page, int size, String sortBy, String filter);
    
    /**
     * 提交评价
     */
    ScenicReview submitReview(ScenicReview review);
    
    /**
     * 点赞评价
     */
    void likeReview(Long reviewId, Long userId);
    
    /**
     * 取消点赞
     */
    void unlikeReview(Long reviewId, Long userId);
    
    /**
     * 回复评价
     */
    void replyToReview(Long reviewId, Long userId, String content);
    
    /**
     * 获取评价统计分析（商户端）
     */
    Map<String, Object> getReviewAnalysis(Long scenicId, String period);
    
    /**
     * 获取评价详情
     */
    Map<String, Object> getReviewDetail(Long reviewId, Long currentUserId);
    
    /**
     * 提取评价关键词（词云）
     * @param scenicId 景区ID
     * @param type 类型: all(全部), positive(好评), negative(差评)
     * @return 关键词列表
     */
    java.util.List<Map<String, Object>> extractKeywords(Long scenicId, String type);
}

