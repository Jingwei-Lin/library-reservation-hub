package com.comp5619.libraryreservationreviewhub.exception;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        if (e instanceof DataIntegrityViolationException) {
            // The full exception (SQL text, table and column names) is logged above; clients get a generic message
            String cause = e.getCause() != null ? e.getCause().getMessage() : null;
            String message = cause != null && cause.contains("Data truncated for column 'status'")
                    ? "Invalid reservation status provided"
                    : "The request conflicts with existing data";
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
        }
        if (e instanceof NullPointerException) {
            log.error("NullPointerException in service layer: ", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Unexpected null value encountered");
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "system error");
    }
}

