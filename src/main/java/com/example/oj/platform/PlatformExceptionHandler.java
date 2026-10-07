package com.example.oj.platform;

import com.example.oj.common.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {"com.example.oj.platform", "com.example.oj.learning",
        "com.example.oj.knowledge", "com.example.oj.assistant"})
public class PlatformExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<PlatformResponse<Void>> business(BusinessException e) {
        int status = e.getCode() / 100;
        if (status < 400 || status > 599) status = 500;
        return ResponseEntity.status(status).body(PlatformResponse.error(e.getCode(),
                status >= 500 ? "Service unavailable" : e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<PlatformResponse<Void>> invalid(Exception e) {
        return ResponseEntity.badRequest().body(PlatformResponse.error(40000, "Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PlatformResponse<Void>> failure(Exception e) {
        return ResponseEntity.internalServerError().body(PlatformResponse.error(50000, "Internal error"));
    }
}
