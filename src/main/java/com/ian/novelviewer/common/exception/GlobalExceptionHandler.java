package com.ian.novelviewer.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.stream.Collectors;

import static com.ian.novelviewer.common.exception.ErrorCode.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("커스텀 예외 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.from(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("유효성 검증 중 오류 발생: {}", e.getMessage(), e);

        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(VALIDATION_ERROR.getCode())
                .message(errorMessage)
                .status(BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException e) {
        log.error("파일 처리 중 예외 발생: {}", e.getMessage(), e);
        ErrorCode error = FILE_PROCESSING_ERROR;

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(error.getCode())
                .message(e.getMessage())
                .status(error.getStatus().value())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("접근 거부 예외 발생: {}", e.getMessage(), e);
        ErrorCode error = NO_PERMISSION;

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(error.getCode())
                .message(error.getMessage())
                .status(error.getStatus().value())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("서버 내부 오류 발생: {}", e.getMessage(), e);
        ErrorCode error = INTERNAL_SERVER_ERROR;

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(error.getCode())
                .message(error.getMessage())
                .status(error.getStatus().value())
                .build();

        return ResponseEntity.status(error.getStatus()).body(response);
    }
}
