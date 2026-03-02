package com.travel.ai.exception;

import lombok.Getter;

/**
 * AI服务自定义异常
 */
@Getter
public class AiServiceException extends RuntimeException {

    private final Integer code;

    public AiServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public AiServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
