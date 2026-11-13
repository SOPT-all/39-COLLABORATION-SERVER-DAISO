package org.sopt.daiso.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorType {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 본문이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    FAVORITE_REQUIRED(HttpStatus.BAD_REQUEST, "찜 상태는 필수입니다."),
    INVALID_FAVORITE_TYPE(HttpStatus.BAD_REQUEST, "찜 상태는 true 또는 false여야 합니다."),
    INVALID_STORE_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 매장 ID입니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    INVALID_PRODUCT_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 상품 ID입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorType(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return name();
    }

    public String getMessage() {
        return message;
    }
}
