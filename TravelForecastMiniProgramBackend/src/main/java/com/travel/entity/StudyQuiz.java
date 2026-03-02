package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mp_study_quiz")
public class StudyQuiz {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicId;
    private String scenicName;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer answer;
    private String explanation;
    private String difficulty;
    private String category;
    private String status;
    private LocalDateTime createdAt;
}
