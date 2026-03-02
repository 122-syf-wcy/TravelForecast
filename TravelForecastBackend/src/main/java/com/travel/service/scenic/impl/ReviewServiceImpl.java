package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.admin.*;
import com.travel.entity.auth.*;
import com.travel.entity.common.*;
import com.travel.entity.content.*;
import com.travel.entity.dashboard.*;
import com.travel.entity.merchant.*;
import com.travel.entity.order.*;
import com.travel.entity.prediction.*;
import com.travel.entity.scenic.*;
import com.travel.entity.system.*;
import com.travel.entity.user.*;
import com.travel.exception.BusinessException;
import com.travel.mapper.admin.*;
import com.travel.mapper.auth.*;
import com.travel.mapper.common.*;
import com.travel.mapper.content.*;
import com.travel.mapper.dashboard.*;
import com.travel.mapper.merchant.*;
import com.travel.mapper.order.*;
import com.travel.mapper.prediction.*;
import com.travel.mapper.scenic.*;
import com.travel.mapper.system.*;
import com.travel.mapper.user.*;
import com.travel.service.scenic.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    private final ScenicReviewMapper scenicReviewMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final BusinessReviewReplyMapper businessReviewReplyMapper;
    private final UserMapper userMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    
    @Override
    public Page<Map<String, Object>> getReviews(Long scenicId, int page, int size, String sortBy, String filter) {
        // 构建查询条件
        LambdaQueryWrapper<ScenicReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicReview::getScenicId, scenicId);
        wrapper.eq(ScenicReview::getStatus, "published");
        
        // 过滤条件
        if ("positive".equals(filter)) {
            wrapper.in(ScenicReview::getSentiment, "positive");
        } else if ("negative".equals(filter)) {
            wrapper.in(ScenicReview::getSentiment, "negative");
        } else if ("withImages".equals(filter)) {
            wrapper.isNotNull(ScenicReview::getImages);
        }
        
        // 排序
        if ("rating".equals(sortBy)) {
            wrapper.orderByDesc(ScenicReview::getRating);
        } else if ("likes".equals(sortBy)) {
            wrapper.orderByDesc(ScenicReview::getLikes);
        } else {
            // 默认按时间排序
            wrapper.orderByDesc(ScenicReview::getCreatedAt);
        }
        
        // 分页查询
        Page<ScenicReview> reviewPage = scenicReviewMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 转换为VO，包含用户信息
        Page<Map<String, Object>> result = new Page<>(page, size, reviewPage.getTotal());
        List<Map<String, Object>> records = reviewPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        result.setRecords(records);
        
        return result;
    }
    
    private Map<String, Object> convertToVO(ScenicReview review) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", review.getId());
        vo.put("userId", review.getUserId());
        vo.put("scenicId", review.getScenicId());
        vo.put("rating", review.getRating());
        vo.put("content", review.getContent());
        vo.put("images", review.getImages());
        vo.put("tags", review.getTags());
        vo.put("visitDate", review.getVisitDate());
        vo.put("likes", review.getLikes());
        vo.put("sentiment", review.getSentiment());
        vo.put("createdAt", review.getCreatedAt());
        
        // 查询用户信息
        User user = userMapper.selectById(review.getUserId());
        if (user != null) {
            vo.put("username", user.getUsername());
            vo.put("userAvatar", user.getAvatar());
            vo.put("userNickname", user.getNickname());
        }
        
        // 查询景区名称
        ScenicSpot scenic = scenicSpotMapper.selectById(review.getScenicId());
        if (scenic != null) {
            vo.put("scenicName", scenic.getName());
        }
        
        // 查询用户回复
        List<ReviewReply> replies = reviewReplyMapper.selectList(
            new LambdaQueryWrapper<ReviewReply>()
                .eq(ReviewReply::getReviewId, review.getId())
                .eq(ReviewReply::getStatus, "published")
        );
        vo.put("replies", replies);
        
        // 查询商家回复
        List<BusinessReviewReply> businessReplies = businessReviewReplyMapper.selectList(
            new LambdaQueryWrapper<BusinessReviewReply>()
                .eq(BusinessReviewReply::getReviewId, review.getId())
                .orderByDesc(BusinessReviewReply::getCreatedAt)
        );
        
        // 如果有商家回复，添加到VO中
        if (!businessReplies.isEmpty()) {
            BusinessReviewReply merchantReply = businessReplies.get(0);
            Map<String, Object> replyVO = new HashMap<>();
            replyVO.put("content", merchantReply.getContent());
            replyVO.put("createdAt", merchantReply.getCreatedAt());
            
            // 查询回复人信息（商家信息）
            User replier = userMapper.selectById(merchantReply.getReplierId());
            if (replier != null) {
                replyVO.put("avatar", replier.getAvatar());
                replyVO.put("username", replier.getUsername());
            }
            
            vo.put("merchantReply", replyVO);
        }
        
        return vo;
    }
    
    @Override
    @Transactional
    public ScenicReview submitReview(ScenicReview review) {
        // 验证景区是否存在
        ScenicSpot scenic = scenicSpotMapper.selectById(review.getScenicId());
        if (scenic == null) {
            throw new BusinessException("景区不存在");
        }
        
        // 设置默认值
        if (review.getLikes() == null) {
            review.setLikes(0);
        }
        if (review.getStatus() == null) {
            review.setStatus("published");
        }
        if (review.getSentiment() == null) {
            // 简单的情感分析：根据评分判断
            if (review.getRating() >= 4) {
                review.setSentiment("positive");
            } else if (review.getRating() >= 3) {
                review.setSentiment("neutral");
            } else {
                review.setSentiment("negative");
            }
        }
        
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        
        scenicReviewMapper.insert(review);
        
        log.info("用户提交评价: userId={}, scenicId={}, reviewId={}", 
            review.getUserId(), review.getScenicId(), review.getId());
        
        return review;
    }
    
    @Override
    @Transactional
    public void likeReview(Long reviewId, Long userId) {
        // 检查评价是否存在
        ScenicReview review = scenicReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        
        // 检查是否已点赞
        Long count = reviewLikeMapper.selectCount(
            new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getReviewId, reviewId)
                .eq(ReviewLike::getUserId, userId)
        );
        
        if (count > 0) {
            throw new BusinessException("已点赞过该评价");
        }
        
        // 添加点赞记录
        ReviewLike like = new ReviewLike();
        like.setReviewId(reviewId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        reviewLikeMapper.insert(like);
        
        // 更新评价点赞数
        review.setLikes(review.getLikes() + 1);
        scenicReviewMapper.updateById(review);
        
        log.info("用户点赞评价: userId={}, reviewId={}", userId, reviewId);
    }
    
    @Override
    @Transactional
    public void unlikeReview(Long reviewId, Long userId) {
        // 检查评价是否存在
        ScenicReview review = scenicReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        
        // 删除点赞记录
        reviewLikeMapper.delete(
            new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getReviewId, reviewId)
                .eq(ReviewLike::getUserId, userId)
        );
        
        // 更新评价点赞数
        if (review.getLikes() > 0) {
            review.setLikes(review.getLikes() - 1);
            scenicReviewMapper.updateById(review);
        }
        
        log.info("用户取消点赞: userId={}, reviewId={}", userId, reviewId);
    }
    
    @Override
    @Transactional
    public void replyToReview(Long reviewId, Long userId, String content) {
        // 检查评价是否存在
        ScenicReview review = scenicReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        
        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setStatus("published");
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        
        reviewReplyMapper.insert(reply);
        
        log.info("商家回复评价: userId={}, reviewId={}, replyId={}", 
            userId, reviewId, reply.getId());
    }
    
    @Override
    public Map<String, Object> getReviewAnalysis(Long scenicId, String period) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime;
        switch (period) {
            case "week":
                startTime = endTime.minusWeeks(1);
                break;
            case "month":
                startTime = endTime.minusMonths(1);
                break;
            case "year":
                startTime = endTime.minusYears(1);
                break;
            default: // day
                startTime = endTime.minusDays(1);
                break;
        }
        
        // 查询景区信息
        ScenicSpot scenic = scenicSpotMapper.selectById(scenicId);
        analysis.put("scenicId", scenicId);
        analysis.put("scenicName", scenic != null ? scenic.getName() : "未知景区");
        analysis.put("period", period);
        
        // 查询时间范围内的评价
        List<ScenicReview> reviews = scenicReviewMapper.selectList(
            new LambdaQueryWrapper<ScenicReview>()
                .eq(ScenicReview::getScenicId, scenicId)
                .eq(ScenicReview::getStatus, "published")
                .ge(ScenicReview::getCreatedAt, startTime)
                .le(ScenicReview::getCreatedAt, endTime)
        );
        
        // 计算平均评分
        double averageRating = reviews.stream()
            .mapToInt(ScenicReview::getRating)
            .average()
            .orElse(0.0);
        analysis.put("averageRating", Math.round(averageRating * 10) / 10.0);
        analysis.put("totalReviews", reviews.size());
        
        // 情感分布
        Map<String, Long> sentimentDist = reviews.stream()
            .collect(Collectors.groupingBy(ScenicReview::getSentiment, Collectors.counting()));
        Map<String, Object> sentimentDistribution = new HashMap<>();
        sentimentDistribution.put("positive", sentimentDist.getOrDefault("positive", 0L));
        sentimentDistribution.put("neutral", sentimentDist.getOrDefault("neutral", 0L));
        sentimentDistribution.put("negative", sentimentDist.getOrDefault("negative", 0L));
        analysis.put("sentimentDistribution", sentimentDistribution);
        
        // 评分趋势（按天）
        Map<LocalDate, List<ScenicReview>> reviewsByDate = reviews.stream()
            .collect(Collectors.groupingBy(r -> r.getCreatedAt().toLocalDate()));
        
        List<Map<String, Object>> ratingTrend = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ScenicReview>> entry : reviewsByDate.entrySet()) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("date", entry.getKey().toString());
            trend.put("rating", entry.getValue().stream()
                .mapToInt(ScenicReview::getRating)
                .average()
                .orElse(0.0));
            trend.put("reviewCount", entry.getValue().size());
            ratingTrend.add(trend);
        }
        analysis.put("ratingTrend", ratingTrend);
        
        // 关键词云（从标签统计）
        Map<String, Long> tagCount = reviews.stream()
            .filter(r -> r.getTags() != null)
            .flatMap(r -> r.getTags().stream())
            .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        
        List<Map<String, Object>> keywordCloud = tagCount.entrySet().stream()
            .map(entry -> {
                Map<String, Object> kw = new HashMap<>();
                kw.put("keyword", entry.getKey());
                kw.put("weight", entry.getValue());
                kw.put("sentiment", "neutral"); // 简化处理
                return kw;
            })
            .sorted((a, b) -> Long.compare((Long) b.get("weight"), (Long) a.get("weight")))
            .limit(20)
            .collect(Collectors.toList());
        analysis.put("keywordCloud", keywordCloud);
        
        // 主要投诉（负面评价）
        List<ScenicReview> negativeReviews = reviews.stream()
            .filter(r -> "negative".equals(r.getSentiment()))
            .collect(Collectors.toList());
        
        List<Map<String, Object>> topComplaints = new ArrayList<>();
        if (!negativeReviews.isEmpty()) {
            Map<String, Object> complaint = new HashMap<>();
            complaint.put("issue", "服务态度");
            complaint.put("count", negativeReviews.size());
            complaint.put("exampleReviews", negativeReviews.stream()
                .limit(3)
                .map(ScenicReview::getContent)
                .collect(Collectors.toList()));
            topComplaints.add(complaint);
        }
        analysis.put("topComplaints", topComplaints);
        
        // 推荐行动
        List<String> recommendations = new ArrayList<>();
        if (averageRating < 3.5) {
            recommendations.add("整体评分偏低，需要重点改进服务质量");
        }
        if (sentimentDistribution.get("negative") != null && 
            (Long) sentimentDistribution.get("negative") > reviews.size() * 0.3) {
            recommendations.add("负面评价较多，建议关注并及时回复");
        }
        recommendations.add("保持良好的景区环境和服务");
        analysis.put("recommendedActions", recommendations);
        
        return analysis;
    }
    
    @Override
    public Map<String, Object> getReviewDetail(Long reviewId, Long currentUserId) {
        ScenicReview review = scenicReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        
        Map<String, Object> detail = convertToVO(review);
        
        // 检查当前用户是否已点赞
        if (currentUserId != null) {
            Long count = reviewLikeMapper.selectCount(
                new LambdaQueryWrapper<ReviewLike>()
                    .eq(ReviewLike::getReviewId, reviewId)
                    .eq(ReviewLike::getUserId, currentUserId)
            );
            detail.put("hasLiked", count > 0);
        } else {
            detail.put("hasLiked", false);
        }
        
        return detail;
    }
    
    @Override
    public List<Map<String, Object>> extractKeywords(Long scenicId, String type) {
        log.info("提取景区{}的关键词，类型: {}", scenicId, type);
        
        // 1. 查询评论数据
        LambdaQueryWrapper<ScenicReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicReview::getScenicId, scenicId);
        wrapper.eq(ScenicReview::getStatus, "published");
        
        // 根据类型过滤
        if ("positive".equals(type)) {
            wrapper.ge(ScenicReview::getRating, 4);
        } else if ("negative".equals(type)) {
            wrapper.le(ScenicReview::getRating, 2);
        }
        
        List<ScenicReview> reviews = scenicReviewMapper.selectList(wrapper);
        
        if (reviews.isEmpty()) {
            log.warn("景区{}没有评论数据", scenicId);
            return new ArrayList<>();
        }
        
        // 2. 定义关键词词库
        List<String> positiveWords = Arrays.asList(
            "美", "好", "不错", "推荐", "满意", "优美", "清新", "周到", "专业", "完善",
            "便利", "值得", "清幽", "如画", "开心", "深刻", "再来", "物超所值", "合理",
            "友好", "热情", "精美", "底蕴", "悠久", "特色", "鲜明", "适合", "性价比",
            "完美", "棒", "赞", "喜欢", "漂亮", "舒服", "干净", "整洁", "方便", "快",
            "满分", "超值", "实惠", "精致", "细致", "贴心", "温馨", "舒适", "愉快", "愉悦"
        );
        
        List<String> negativeWords = Arrays.asList(
            "差", "不好", "失望", "糟糕", "太多", "偏高", "不清", "陈旧", "混乱",
            "不便", "一般", "少", "态度", "噪音", "脏", "乱", "慢", "贵", "远",
            "挤", "排队", "等待", "浪费", "不值", "后悔", "坑", "骗", "假", "商业"
        );
        
        // 3. 统计关键词
        Map<String, KeywordStat> keywordMap = new HashMap<>();
        
        for (ScenicReview review : reviews) {
            String content = review.getContent();
            if (content == null || content.trim().isEmpty()) {
                continue;
            }
            
            // 判断评论类型
            String reviewType = review.getRating() >= 4 ? "positive" 
                              : review.getRating() <= 2 ? "negative" 
                              : "neutral";
            
            // 提取2-4字的词组
            for (int len = 2; len <= 4; len++) {
                for (int i = 0; i <= content.length() - len; i++) {
                    final String word = content.substring(i, i + len);
                    
                    // 跳过包含标点符号的词
                    if (word.matches(".*[，。！？、；：\"\"''（）《》【】].*")) {
                        continue;
                    }
                    
                    // 判断词的情感类型
                    boolean hasPositive = positiveWords.stream().anyMatch(word::contains);
                    boolean hasNegative = negativeWords.stream().anyMatch(word::contains);
                    
                    final String wordType;
                    if (hasPositive && !hasNegative) {
                        wordType = "positive";
                    } else if (hasNegative && !hasPositive) {
                        wordType = "negative";
                    } else {
                        wordType = reviewType;
                    }
                    
                    // 统计词频
                    String key = word + "_" + wordType;
                    keywordMap.computeIfAbsent(key, k -> new KeywordStat(word, wordType, 0))
                              .incrementCount();
                }
            }
            
            // 从标签中提取关键词（标签权重更高）
            List<String> tagsList = review.getTags();
            if (tagsList != null && !tagsList.isEmpty()) {
                for (String tagRaw : tagsList) {
                    if (tagRaw == null || tagRaw.trim().isEmpty()) continue;
                    final String tag = tagRaw.trim();
                    
                    // 判断标签情感
                    final String baseTagType = reviewType;
                    boolean hasPositive = positiveWords.stream().anyMatch(tag::contains);
                    boolean hasNegative = negativeWords.stream().anyMatch(tag::contains);
                    
                    String tagType = baseTagType;
                    if (hasPositive && !hasNegative) {
                        tagType = "positive";
                    } else if (hasNegative && !hasPositive) {
                        tagType = "negative";
                    }
                    
                    String key = tag + "_" + tagType;
                    final String finalTagType = tagType;
                    keywordMap.computeIfAbsent(key, k -> new KeywordStat(tag, finalTagType, 0))
                              .addCount(3); // 标签权重为3
                }
            }
        }
        
        // 4. 转换为结果列表，按词频排序，取前50个
        return keywordMap.values().stream()
            .sorted(Comparator.comparingInt(KeywordStat::getCount).reversed())
            .limit(50)
            .map(stat -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", stat.getWord());
                map.put("value", stat.getCount());
                map.put("type", stat.getType());
                return map;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 关键词统计辅助类
     */
    private static class KeywordStat {
        private final String word;
        private final String type;
        private int count;
        
        public KeywordStat(String word, String type, int count) {
            this.word = word;
            this.type = type;
            this.count = count;
        }
        
        public String getWord() {
            return word;
        }
        
        public String getType() {
            return type;
        }
        
        public int getCount() {
            return count;
        }
        
        public void incrementCount() {
            this.count++;
        }
        
        public void addCount(int delta) {
            this.count += delta;
        }
    }
}

