package com.tommy.wallet.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception
    ) {
        // Không ghi token hoặc thông tin request vào log.
        log.error("Wallet request failed with exception type: {}",
                exception.getClass().getName());

        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Hệ thống gặp lỗi, vui lòng thử lại sau."
                ));
    }
}