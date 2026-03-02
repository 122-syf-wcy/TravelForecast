package com.travel.controller.scenic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.dto.scenic.ReplyRequest;
import com.travel.entity.scenic.ScenicReview;
import com.travel.service.scenic.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取景区评价列表（分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getReviews(
            @RequestParam Long scenicId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "all") String filter) {

        log.info("获取景区评价列表: scenicId={}, page={}, size={}, sortBy={}, filter={}",
            scenicId, page, size, sortBy, filter);

        Page<Map<String, Object>> reviewPage = reviewService.getReviews(scenicId, page, size, sortBy, filter);

        Map<String, Object> result = new HashMap<>();
        result.put("total", reviewPage.getTotal());
        result.put("reviews", reviewPage.getRecords());

        if (!reviewPage.getRecords().isEmpty()) {
            double avgRating = reviewPage.getRecords().stream()
                .mapToInt(r -> (Integer) r.get("rating"))
                .average()
                .orElse(0.0);
            result.put("averageRating", Math.round(avgRating * 10) / 10.0);

            Map<String, Long> ratingDist = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                ratingDist.put(String.valueOf(i), 0L);
            }
            result.put("ratingDistribution", ratingDist);
        } else {
            result.put("averageRating", 0.0);
            result.put("ratingDistribution", new HashMap<>());
        }

        result.put("popularTags", Arrays.asList(
            Map.of("name", "风景优美", "count", 10),
            Map.of("name", "值得一游", "count", 8)
        ));

        return Result.success(result);
    }

    /**
     * 提交评价
     */
    @PostMapping
    public Result<ScenicReview> submitReview(
            @RequestBody ScenicReview review,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        review.setUserId(userId);

        log.info("用户提交评价: userId={}, scenicId={}, rating={}",
            userId, review.getScenicId(), review.getRating());

        ScenicReview created = reviewService.submitReview(review);
        return Result.success(created, "评价提交成功");
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/{reviewId}")
    public Result<Map<String, Object>> getReviewDetail(
            @PathVariable Long reviewId,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        log.info("获取评价详情: reviewId={}", reviewId);
        Map<String, Object> detail = reviewService.getReviewDetail(reviewId, userId);
        return Result.success(detail);
    }

    /**
     * 点赞评价
     */
    @PostMapping("/{reviewId}/like")
    public Result<Void> likeReview(
            @PathVariable Long reviewId,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("用户点赞评价: userId={}, reviewId={}", userId, reviewId);
        reviewService.likeReview(reviewId, userId);
        return Result.success(null, "点赞成功");
    }

    /**
     * 取消点赞
     */
    @PostMapping("/{reviewId}/unlike")
    public Result<Void> unlikeReview(
            @PathVariable Long reviewId,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("用户取消点赞: userId={}, reviewId={}", userId, reviewId);
        reviewService.unlikeReview(reviewId, userId);
        return Result.success(null, "已取消点赞");
    }

    /**
     * 回复评价（商家端）
     */
    @PostMapping("/{reviewId}/reply")
    public Result<Void> replyToReview(
            @PathVariable Long reviewId,
            @RequestBody ReplyRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestAttribute(value = "role", required = false) String role) {

        if (userId == null) {
            return Result.error("未登录");
        }

        if (!"merchant".equals(role) && !"admin".equals(role)) {
            return Result.error("无权回复评价");
        }

        log.info("用户回复评价: userId={}, reviewId={}", userId, reviewId);
        reviewService.replyToReview(reviewId, userId, request.getContent());
        return Result.success(null, "回复成功");
    }

    /**
     * 获取评价统计分析（商户端/管理员端）
     */
    @GetMapping("/admin/reviews/analysis/{scenicId}")
    public Result<Map<String, Object>> getReviewAnalysis(
            @PathVariable Long scenicId,
            @RequestParam(defaultValue = "week") String period,
            @RequestAttribute(value = "role", required = false) String role) {

        if (!"merchant".equals(role) && !"admin".equals(role)) {
            return Result.error("无权查看统计分析");
        }

        log.info("获取评价统计分析: scenicId={}, period={}", scenicId, period);
        Map<String, Object> analysis = reviewService.getReviewAnalysis(scenicId, period);
        return Result.success(analysis);
    }

    /**
     * 获取评价关键词词云数据
     */
    @GetMapping("/keywords")
    public Result<List<Map<String, Object>>> getReviewKeywords(
            @RequestParam Long scenicId,
            @RequestParam(defaultValue = "all") String type) {

        log.info("获取评价关键词: scenicId={}, type={}", scenicId, type);
        List<Map<String, Object>> keywords = reviewService.extractKeywords(scenicId, type);
        return Result.success(keywords);
    }
}
