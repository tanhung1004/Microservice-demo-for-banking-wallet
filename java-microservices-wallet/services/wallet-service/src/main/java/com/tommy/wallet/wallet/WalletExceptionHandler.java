package com.tommy.wallet.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WalletController.class)
public class WalletExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(WalletExceptionHandler.class);

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            WalletNotFoundException exception
    ) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse(
                        "WALLET_NOT_FOUND",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(WalletOperationException.class)
    public ResponseEntity<ErrorResponse> handleOperation(
            WalletOperationException exception
    ) {
        int status = switch (exception.getCode()) {
            case "VALIDATION_ERROR" -> 400;
            case "BALANCE_LIMIT_EXCEEDED" -> 409;
            default -> 500;
        };

        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        exception.getCode(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        "Body phải là JSON hợp lệ với trường amount là số nguyên."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception
    ) {
        // Không ghi token hoặc nội dung request vào log.
        log.error("Wallet request failed with exception type: {}",
                exception.getClass().getName());

        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Hệ thống gặp lỗi, vui lòng thử lại sau."
                ));
    }
}