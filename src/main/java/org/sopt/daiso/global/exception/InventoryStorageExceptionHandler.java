package org.sopt.daiso.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.daiso.global.response.ErrorResponse;
import org.sopt.daiso.product.controller.ProductController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ProductController.class)
public class InventoryStorageExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(InventoryStorageExceptionHandler.class);

    @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessResourceException.class,
            QueryTimeoutException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErrorResponse> handleStorageUnavailable(Exception exception) {
        log.error("상품의 매장별 재고 저장소에 접근할 수 없습니다.", exception);
        ErrorType type = ErrorType.STORAGE_UNAVAILABLE;
        return ResponseEntity.status(type.getStatus()).body(new ErrorResponse(type.getCode(), type.getMessage()));
    }
}
