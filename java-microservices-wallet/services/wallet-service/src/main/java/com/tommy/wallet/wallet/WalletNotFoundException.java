package com.tommy.wallet.wallet;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException() {
        super("Không tìm thấy ví của tài khoản.");
    }
}