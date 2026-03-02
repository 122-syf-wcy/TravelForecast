package com.travel.ai.service.impl;

import com.travel.ai.client.DigitalHumanClient;
import com.travel.ai.service.SpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 语音服务实现
 * 代理调用旧数字人后端的TTS服务
 */
@Service
public class SpeechServiceImpl implements SpeechService {

    private static final Logger log = LoggerFactory.getLogger(SpeechServiceImpl.class);

    @Autowired
    private DigitalHumanClient digitalHumanClient;

    @Override
    public byte[] textToSpeech(String text, String voice, Double speed) {
        if (!isAvailable()) {
            log.warn("语音服务不可用");
            return null;
        }

        log.info("TTS请求: text={} chars, voice={}, speed={}", text.length(), voice, speed);

        byte[] audioData = digitalHumanClient.textToSpeech(text, voice);

        if (audioData != null) {
            log.info("TTS响应: {} bytes", audioData.length);
        }

        return audioData;
    }

    @Override
    public boolean isAvailable() {
        return digitalHumanClient.isAvailable();
    }
}
