package com.ian.novelviewer.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 잘못되었습니다.", HttpStatus.UNAUTHORIZED),
    DUPLICATE_LOGIN_ID("이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_HAS_ROLE("이미 해당 권한을 보유하고 있습니다.", HttpStatus.BAD_REQUEST),
    S3_UPLOAD_FAILED("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    S3_ROLLBACK_FAILED("이미지 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    NOVEL_CREATION_FAILED("작품 등록에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    NOVEL_NOT_FOUND("해당 작품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_KEYWORD("검색어가 입력되지 않았습니다.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    public String getCode() {
        return name();
    }
}