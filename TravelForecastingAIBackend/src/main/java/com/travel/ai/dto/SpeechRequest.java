package com.travel.ai.dto;

import lombok.Data;

/**
 * 语音服务请求DTO
 */
@Data
public class SpeechRequest {
    /** 文本内容 (TTS用) */
    private String text;
    /** 语音角色 */
    private String voice;
    /** 语速 */
    private Double speed = 1.0;
}
