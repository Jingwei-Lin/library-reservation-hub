package com.comp5619.libraryreservationreviewhub.exception;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Every refusal is HTTP 200 with a {@link BaseResponse} carrying the code, as JSON.
 *
 * <p>The content type is set here rather than negotiated: a client that asked for something else (the chat's
 * EventSource asks for text/event-stream) would otherwise get no body that Spring can write, and an HTTP 500.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return json(ResultUtils.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        if (e instanceof DataIntegrityViolationException) {
            // The full exception (SQL text, table and column names) is logged above; clients get a generic message
            String cause = e.getCause() != null ? e.getCause().getMessage() : null;
            String message = cause != null && cause.contains("Data truncated for column 'status'")
                    ? "Invalid reservation status provided"
                    : "The request conflicts with existing data";
            return json(ResultUtils.error(ErrorCode.SYSTEM_ERROR, message));
        }
        if (e instanceof NullPointerException) {
            log.error("NullPointerException in service layer: ", e);
            return json(ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Unexpected null value encountered"));
        }
        return json(ResultUtils.error(ErrorCode.SYSTEM_ERROR, "system error"));
    }

    private static ResponseEntity<BaseResponse<?>> json(BaseResponse<?> body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
