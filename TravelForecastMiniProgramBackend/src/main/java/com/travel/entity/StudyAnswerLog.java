package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mp_study_answer_log")
public class StudyAnswerLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long quizId;
    private Integer userAnswer;
    private Boolean isCorrect;
    private Integer points;
    private LocalDateTime createdAt;
}
