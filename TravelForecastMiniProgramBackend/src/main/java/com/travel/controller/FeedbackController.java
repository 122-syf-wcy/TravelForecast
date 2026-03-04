package com.travel.controller;

import com.travel.common.Result;
import com.travel.entity.Feedback;
import com.travel.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;

    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Map<String, Object> body) {
        Feedback fb = new Feedback();
        fb.setUserId(body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null);
        fb.setType((String) body.getOrDefault("type", "其他"));
        fb.setContent((String) body.getOrDefault("content", ""));
        fb.setContact((String) body.getOrDefault("contact", ""));
        fb.setStatus("pending");
        fb.setCreatedAt(LocalDateTime.now());

        if (fb.getContent() == null || fb.getContent().trim().length() < 10) {
            return Result.error("反馈内容至少需要10个字");
        }

        feedbackMapper.insert(fb);
        return Result.success("提交成功，感谢您的反馈");
    }
}
