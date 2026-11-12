package org.sopt.daiso.store.exception;

import org.sopt.daiso.global.exception.BusinessException;
import org.sopt.daiso.global.exception.ErrorCode;

public class StoreException extends BusinessException {
    public StoreException(ErrorCode errorCode) {
        super(errorCode);
    }
}
