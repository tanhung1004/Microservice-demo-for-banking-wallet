package com.tommy.wallet.wallet;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ResponseEntity<Wallet> getBalance(
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Chỉ lấy danh tính từ JWT đã được xác thực.
        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(walletService.getBalance(userId));
    }
}