package org.sopt.daiso.global.exception;

public class StoreException extends BusinessException {
    public StoreException(ErrorCode errorCode) {
        super(errorCode);
    }
}
