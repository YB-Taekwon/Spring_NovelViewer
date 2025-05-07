package com.ian.novelviewer.common.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private final String errorCode;
    private final String message;
    private final int status;

    public static ErrorResponse from(CustomException exception) {
        return ErrorResponse.builder()
                .errorCode(exception.getCode())
                .message(exception.getMessage())
                .status(exception.getStatus().value())
                .build();
    }
}
