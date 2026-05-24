package com.kraft.lotto.support;

public record ApiError(String code, String message, boolean retryable) {

    public static ApiError of(ErrorCode errorCode) {
        return new ApiError(errorCode.name(), errorCode.getDefaultMessage(), errorCode.isRetryable());
    }

    public static ApiError of(ErrorCode errorCode, String message) {
        String resolved = (message == null || message.isBlank()) ? errorCode.getDefaultMessage() : message;
        return new ApiError(errorCode.name(), resolved, errorCode.isRetryable());
    }
}
