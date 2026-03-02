package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.*;
import com.travel.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyQuizMapper quizMapper;
    private final StudyBadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final UserPointsMapper pointsMapper;
    private final StudyAnswerLogMapper answerLogMapper;

    @GetMapping("/quiz")
    public Result<List<StudyQuiz>> quizList(@RequestParam(required = false) String scenicName) {
        LambdaQueryWrapper<StudyQuiz> qw = new LambdaQueryWrapper<>();
        qw.eq(StudyQuiz::getStatus, "ACTIVE");
        if (scenicName != null && !scenicName.isBlank()) {
            qw.like(StudyQuiz::getScenicName, scenicName);
        }
        return Result.success(quizMapper.selectList(qw));
    }

    @PostMapping("/quiz/answer")
    public Result<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long quizId = Long.valueOf(body.get("quizId").toString());
        int userAnswer = Integer.parseInt(body.get("userAnswer").toString());

        StudyQuiz quiz = quizMapper.selectById(quizId);
        if (quiz == null) return Result.error("题目不存在");

        boolean correct = userAnswer == quiz.getAnswer();
        int points = correct ? 10 : 0;

        StudyAnswerLog log = new StudyAnswerLog();
        log.setUserId(userId);
        log.setQuizId(quizId);
        log.setUserAnswer(userAnswer);
        log.setIsCorrect(correct);
        log.setPoints(points);
        answerLogMapper.insert(log);

        if (correct) {
            addPoints(userId, points);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("points", points);
        result.put("explanation", quiz.getExplanation());
        return Result.success(result);
    }

    @GetMapping("/passport")
    public Result<Map<String, Object>> passport(@RequestParam Long userId) {
        List<StudyBadge> allBadges = badgeMapper.selectList(
                new LambdaQueryWrapper<StudyBadge>().orderByAsc(StudyBadge::getSortOrder));

        LambdaQueryWrapper<UserBadge> ubqw = new LambdaQueryWrapper<>();
        ubqw.eq(UserBadge::getUserId, userId);
        Set<Long> unlockedIds = userBadgeMapper.selectList(ubqw).stream()
                .map(UserBadge::getBadgeId).collect(Collectors.toSet());

        List<Map<String, Object>> badges = new ArrayList<>();
        for (StudyBadge b : allBadges) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("char", b.getIconChar());
            m.put("bg", b.getColor());
            m.put("desc", b.getDescription());
            m.put("unlocked", unlockedIds.contains(b.getId()));
            badges.add(m);
        }

        UserPoints up = getOrCreatePoints(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("badges", badges);
        result.put("totalPoints", up.getTotalPoints());
        return Result.success(result);
    }

    @PostMapping("/points/add")
    public Result<UserPoints> addPointsApi(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        int pts = Integer.parseInt(body.get("points").toString());
        addPoints(userId, pts);
        return Result.success(getOrCreatePoints(userId));
    }

    private void addPoints(Long userId, int pts) {
        UserPoints up = getOrCreatePoints(userId);
        up.setTotalPoints(up.getTotalPoints() + pts);
        pointsMapper.updateById(up);
    }

    private UserPoints getOrCreatePoints(Long userId) {
        LambdaQueryWrapper<UserPoints> qw = new LambdaQueryWrapper<>();
        qw.eq(UserPoints::getUserId, userId);
        UserPoints up = pointsMapper.selectOne(qw);
        if (up == null) {
            up = new UserPoints();
            up.setUserId(userId);
            up.setTotalPoints(0);
            up.setUsedPoints(0);
            pointsMapper.insert(up);
        }
        return up;
    }
}