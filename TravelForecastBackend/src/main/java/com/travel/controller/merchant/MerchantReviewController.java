package com.travel.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.dto.scenic.ReplyRequest;
import com.travel.entity.merchant.BusinessReviewReply;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.entity.scenic.ScenicReview;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.user.User;
import com.travel.mapper.scenic.BusinessReviewReplyMapper;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.mapper.scenic.ScenicReviewMapper;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.mapper.user.UserMapper;
import com.travel.service.content.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家端评论管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/reviews")
@RequiredArgsConstructor
public class MerchantReviewController {

    private final MerchantProfileMapper merchantProfileMapper;
    private final ScenicReviewMapper scenicReviewMapper;
    private final BusinessReviewReplyMapper businessReviewReplyMapper;
    private final UserMapper userMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    private final SensitiveWordService sensitiveWordService;

    /**
     * 获取商家关联景区的评论列表（分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getMerchantReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestAttribute("userId") Long userId) {

        log.info("商家获取评论列表: userId={}, page={}, size={}, status={}", userId, page, size, status);

        MerchantProfile merchantProfile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, userId)
        );

        if (merchantProfile == null || merchantProfile.getScenicId() == null) {
            return Result.error("未找到商家关联的景区");
        }

        Long scenicId = merchantProfile.getScenicId();

        LambdaQueryWrapper<ScenicReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicReview::getScenicId, scenicId);
        wrapper.ne(ScenicReview::getStatus, "deleted");

        if (!"all".equals(status)) {
            wrapper.eq(ScenicReview::getStatus, status);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(ScenicReview::getContent, keyword.trim());
        }

        if ("newest".equals(sortBy)) {
            wrapper.orderByDesc(ScenicReview::getCreatedAt);
        } else if ("rating_high".equals(sortBy)) {
            wrapper.orderByDesc(ScenicReview::getRating);
        } else if ("rating_low".equals(sortBy)) {
            wrapper.orderByAsc(ScenicReview::getRating);
        } else if ("likes".equals(sortBy)) {
            wrapper.orderByDesc(ScenicReview::getLikes);
        }

        Page<ScenicReview> pageParam = new Page<>(page, size);
        Page<ScenicReview> reviewPage = scenicReviewMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> reviews = reviewPage.getRecords().stream().map(review -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", review.getId());
            map.put("userId", review.getUserId());
            map.put("scenicId", review.getScenicId());
            map.put("rating", review.getRating());
            map.put("content", review.getContent());
            map.put("images", review.getImages());
            map.put("tags", review.getTags());
            map.put("visitDate", review.getVisitDate());
            map.put("likes", review.getLikes());
            map.put("sentiment", review.getSentiment());
            map.put("status", review.getStatus());
            map.put("createdAt", review.getCreatedAt());

            User user = userMapper.selectById(review.getUserId());
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("userAvatar", user.getAvatar());
            } else {
                map.put("username", "未知用户");
                map.put("userAvatar", null);
            }

            ScenicSpot scenic = scenicSpotMapper.selectById(review.getScenicId());
            map.put("scenicName", scenic != null ? scenic.getName() : "未知景区");

            List<BusinessReviewReply> replies = businessReviewReplyMapper.selectList(
                new LambdaQueryWrapper<BusinessReviewReply>()
                    .eq(BusinessReviewReply::getReviewId, review.getId())
                    .orderByDesc(BusinessReviewReply::getCreatedAt)
            );

            map.put("reply", replies.isEmpty() ? null : replies.get(0));
            map.put("hasReply", !replies.isEmpty());

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", reviewPage.getTotal());
        result.put("reviews", reviews);
        result.put("currentPage", page);
        result.put("pageSize", size);

        return Result.success(result);
    }

    /**
     * 回复评论
     */
    @PostMapping("/{reviewId}/reply")
    public Result<BusinessReviewReply> replyReview(
            @PathVariable Long reviewId,
            @RequestBody ReplyRequest request,
            @RequestAttribute("userId") Long userId) {

        log.info("商家回复评论: userId={}, reviewId={}", userId, reviewId);

        ScenicReview review = scenicReviewMapper.selectById(reviewId);
        if (review == null) {
            return Result.error("评论不存在");
        }

        MerchantProfile merchantProfile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, userId)
        );

        if (merchantProfile == null || !merchantProfile.getScenicId().equals(review.getScenicId())) {
            return Result.error("无权回复此评论");
        }

        if (sensitiveWordService.containsBlockedWords(request.getContent())) {
            return Result.error("回复内容包含敏感词，禁止发布");
        }

        String filteredContent = sensitiveWordService.filterSensitiveWords(request.getContent());

        List<BusinessReviewReply> existingReplies = businessReviewReplyMapper.selectList(
            new LambdaQueryWrapper<BusinessReviewReply>()
                .eq(BusinessReviewReply::getReviewId, reviewId)
                .eq(BusinessReviewReply::getReplierId, userId)
        );

        BusinessReviewReply reply;

        if (!existingReplies.isEmpty()) {
            reply = existingReplies.get(0);
            reply.setContent(filteredContent);
            businessReviewReplyMapper.updateById(reply);
            log.info("更新商家回复: replyId={}", reply.getId());
        } else {
            reply = new BusinessReviewReply();
            reply.setReviewId(reviewId);
            reply.setReplierId(userId);
            reply.setContent(filteredContent);
            reply.setCreatedAt(LocalDateTime.now());
            businessReviewReplyMapper.insert(reply);
            log.info("创建商家回复: replyId={}", reply.getId());
        }

        return Result.success(reply, "回复成功");
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{reviewId}")
    public Result<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestAttribute("userId") Long userId) {

        log.info("商家删除评论: userId={}, reviewId={}", userId, reviewId);

        ScenicReview review = scenicReviewMapper.selectById(reviewId);
        if (review == null) {
            return Result.error("评论不存在");
        }

        MerchantProfile merchantProfile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, userId)
        );

        if (merchantProfile == null || !merchantProfile.getScenicId().equals(review.getScenicId())) {
            return Result.error("无权删除此评论");
        }

        review.setStatus("deleted");
        scenicReviewMapper.updateById(review);

        log.info("评论已删除: reviewId={}", reviewId);
        return Result.success(null, "删除成功");
    }

    /**
     * 获取评论统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getReviewStats(@RequestAttribute("userId") Long userId) {

        log.info("商家获取评论统计: userId={}", userId);

        MerchantProfile merchantProfile = merchantProfileMapper.selectOne(
            new LambdaQueryWrapper<MerchantProfile>()
                .eq(MerchantProfile::getUserId, userId)
        );

        if (merchantProfile == null || merchantProfile.getScenicId() == null) {
            return Result.error("未找到商家关联的景区");
        }

        Long scenicId = merchantProfile.getScenicId();

        Map<String, Object> stats = new HashMap<>();

        Long totalCount = scenicReviewMapper.selectCount(
            new LambdaQueryWrapper<ScenicReview>()
                .eq(ScenicReview::getScenicId, scenicId)
                .ne(ScenicReview::getStatus, "deleted")
        );
        stats.put("totalCount", totalCount);

        List<ScenicReview> allReviews = scenicReviewMapper.selectList(
            new LambdaQueryWrapper<ScenicReview>()
                .eq(ScenicReview::getScenicId, scenicId)
                .ne(ScenicReview::getStatus, "deleted")
        );

        long unrepliedCount = allReviews.stream()
            .filter(r -> {
                Long replyCount = businessReviewReplyMapper.selectCount(
                    new LambdaQueryWrapper<BusinessReviewReply>()
                        .eq(BusinessReviewReply::getReviewId, r.getId())
                );
                return replyCount == 0;
            })
            .count();
        stats.put("unrepliedCount", unrepliedCount);

        double avgRating = allReviews.stream()
            .mapToInt(ScenicReview::getRating)
            .average()
            .orElse(0.0);
        stats.put("averageRating", Math.round(avgRating * 10) / 10.0);

        Map<Integer, Long> ratingDistribution = allReviews.stream()
            .collect(Collectors.groupingBy(ScenicReview::getRating, Collectors.counting()));
        stats.put("ratingDistribution", ratingDistribution);

        return Result.success(stats);
    }
}
