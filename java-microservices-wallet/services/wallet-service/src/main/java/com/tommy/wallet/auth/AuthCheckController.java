package com.tommy.wallet.auth;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
public class AuthCheckController {

    @GetMapping("/api/wallet/auth-check")
    public Map<String, String> check(
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Jwt ở đây đã qua bộ lọc xác thực của Spring Security.
        return Map.of("userId", jwt.getSubject());
    }
}