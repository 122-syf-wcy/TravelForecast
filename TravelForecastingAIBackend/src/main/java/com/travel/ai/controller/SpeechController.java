package com.travel.ai.controller;

import com.travel.ai.common.Result;
import com.travel.ai.dto.SpeechRequest;
import com.travel.ai.service.SpeechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 语音服务控制器
 * 代理转发到数字人后端的语音服务
 */
@RestController
@RequestMapping("/speech")
@Tag(name = "语音服务", description = "TTS语音合成代理")
public class SpeechController {

    @Autowired
    private SpeechService speechService;

    /**
     * 文本转语音
     */
    @PostMapping("/tts")
    @Operation(summary = "文本转语音")
    public ResponseEntity<?> textToSpeech(@RequestBody SpeechRequest request) {
        if (!speechService.isAvailable()) {
            return ResponseEntity.status(503)
                    .body(Result.error("语音服务暂不可用"));
        }

        byte[] audioData = speechService.textToSpeech(
                request.getText(), request.getVoice(), request.getSpeed());

        if (audioData == null) {
            return ResponseEntity.status(500)
                    .body(Result.error("语音合成失败"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tts.wav")
                .body(audioData);
    }

    /**
     * 检查语音服务状态
     */
    @GetMapping("/status")
    @Operation(summary = "检查语音服务状态")
    public Result<?> status() {
        boolean available = speechService.isAvailable();
        return Result.success(java.util.Map.of(
                "available", available,
                "service", "digital-human-tts"
        ));
    }
}
