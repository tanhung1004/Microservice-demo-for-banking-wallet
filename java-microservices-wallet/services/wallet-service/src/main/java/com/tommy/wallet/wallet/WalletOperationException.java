package com.tommy.wallet.wallet;

public class WalletOperationException extends RuntimeException {

    private final String code;

    public WalletOperationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}