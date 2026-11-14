package org.sopt.daiso.global.exception;

import java.sql.SQLException;
import org.hibernate.PropertyValueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.daiso.global.response.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        return error(exception.getErrorType());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorType code = ErrorType.INVALID_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(new ErrorResponse(code.getCode(), code.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorType type = ErrorType.METHOD_NOT_ALLOWED;
        return new ResponseEntity<>(new ErrorResponse(type.getCode(), type.getMessage()), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorType type = ErrorType.UNSUPPORTED_MEDIA_TYPE;
        return new ResponseEntity<>(new ErrorResponse(type.getCode(), type.getMessage()), headers, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        if (exception.contains(PropertyValueException.class)) {
            return error(ErrorType.NULL_VALUE_NOT_ALLOWED);
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && (sqlException.getErrorCode() == 1048 || sqlException.getErrorCode() == 1364)) {
                return error(ErrorType.NULL_VALUE_NOT_ALLOWED);
            }
        }
        return handleUnexpectedException(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("요청 처리 중 서버 오류가 발생했습니다.", exception);
        return error(ErrorType.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> error(ErrorType code) {
        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getCode(), code.getMessage()));
    }
}
