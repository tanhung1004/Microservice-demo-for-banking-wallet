package com.tommy.login.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(AuthExceptionHandler.class);

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(
                        exception.code(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson() {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        "Nội dung JSON không hợp lệ."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        // Không ghi request, password hoặc token vào log.
        log.error("Login failed with exception type: {}",
                exception.getClass().getName());

        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Hệ thống gặp lỗi, vui lòng thử lại sau."
                ));
    }
}