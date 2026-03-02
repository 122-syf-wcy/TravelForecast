package com.travel.ai.service;

/**
 * 语音服务接口
 * 代理调用数字人后端的语音服务
 */
public interface SpeechService {

    /**
     * 文本转语音 (TTS)
     */
    byte[] textToSpeech(String text, String voice, Double speed);

    /**
     * 检查语音服务是否可用
     */
    boolean isAvailable();
}
