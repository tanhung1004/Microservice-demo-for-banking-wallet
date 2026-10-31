package com.tommy.login.auth;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        // Tạo khóa riêng cho test, không dùng hoặc sửa khóa trong secrets/.
        KeyPair keyPair = generateKeyPair();

        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();

        var encoder = NimbusJwtEncoder
                .withKeyPair(publicKey, privateKey)
                .build();

        jwtService = new JwtService(encoder);

        decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();

        // Kiểm tra issuer và thời gian hiệu lực khi đọc token.
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer("wallet-login")
        );
    }

    @Test
    void tokenHasValidSignatureAndExpectedClaims() {
        UUID userId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        String token = jwtService.createAccessToken(userId);

        // decode() sẽ báo lỗi nếu chữ ký không hợp lệ.
        Jwt jwt = decoder.decode(token);

        assertEquals("RS256", jwt.getHeaders().get("alg"));
        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(
                "wallet-login",
                jwt.getClaimAsString("iss")
        );
        assertEquals(
                List.of("wallet-api"),
                jwt.getAudience()
        );

        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());

        assertEquals(
                900L,
                Duration.between(
                        jwt.getIssuedAt(),
                        jwt.getExpiresAt()
                ).getSeconds()
        );

        assertFalse(jwt.getClaims().containsKey("password"));
        assertFalse(jwt.getClaims().containsKey("passwordHash"));
        assertFalse(jwt.getClaims().containsKey("password_hash"));
    }

    @Test
    void tokenIsRejectedWithWrongPublicKey() throws Exception {
        String token = jwtService.createAccessToken(UUID.randomUUID());

        KeyPair otherKeyPair = generateKeyPair();

        var wrongDecoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) otherKeyPair.getPublic())
                .build();

        assertThrows(
                JwtException.class,
                () -> wrongDecoder.decode(token)
        );
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}