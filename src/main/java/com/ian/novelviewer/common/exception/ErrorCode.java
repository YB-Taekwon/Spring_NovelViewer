package com.ian.novelviewer.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("필드의 값이 유효한 형식이 아닙니다. 올바른 값을 입력해주세요.", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 잘못되었습니다.", HttpStatus.UNAUTHORIZED),
    DUPLICATE_LOGIN_ID("이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_HAS_ROLE("이미 해당 권한을 보유하고 있습니다.", HttpStatus.BAD_REQUEST),
    S3_UPLOAD_FAILED("S3 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    S3_UPDATE_FAILED("S3 수정 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    S3_DELETE_FAILED("S3 삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_PROCESSING_ERROR("파일을 처리하는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_EMPTY("파일이 비어 있습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_FORMAT("올바른 이미지 파일 형식이 아닙니다. (jpg, png, jpeg 등)", HttpStatus.INTERNAL_SERVER_ERROR),
    NOVEL_CREATION_FAILED("작품 등록에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    NOVEL_NOT_FOUND("해당 작품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_KEYWORD("검색어가 입력되지 않았습니다.", HttpStatus.BAD_REQUEST),
    NO_PERMISSION("해당 작업을 수행할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    EPISODE_NOT_FOUND("해당 회차를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("해당 댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

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