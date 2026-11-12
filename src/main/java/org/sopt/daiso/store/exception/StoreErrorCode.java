package org.sopt.daiso.store.exception;

import org.sopt.daiso.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StoreErrorCode implements ErrorCode {
    FAVORITE_REQUIRED(HttpStatus.BAD_REQUEST, "찜 상태는 필수입니다."),
    INVALID_FAVORITE_TYPE(HttpStatus.BAD_REQUEST, "찜 상태는 true 또는 false여야 합니다."),
    INVALID_STORE_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 매장 ID입니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    StoreErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return name(); }
    public String getMessage() { return message; }
}
