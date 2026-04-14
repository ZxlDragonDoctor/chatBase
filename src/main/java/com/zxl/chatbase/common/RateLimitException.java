package com.zxl.chatbase.common;

public class RateLimitException extends RuntimeException {

    private String message;

    private long retryAfterSeconds;

    public RateLimitException(String message) {
        super(message);
        this.message = message;
    }

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}