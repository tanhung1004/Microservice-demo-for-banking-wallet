package com.tommy.login.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(
            @Value("${app.jwt.private-key}") String privateKeyPath,
            @Value("${app.jwt.public-key}") String publicKeyPath
    ) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(
                        readPem(privateKeyPath, "PRIVATE KEY")
                )
        );

        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                new X509EncodedKeySpec(
                        readPem(publicKeyPath, "PUBLIC KEY")
                )
        );

        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    }

    private byte[] readPem(String filePath, String label) throws Exception {
        String content = Files.readString(Path.of(filePath));

        String base64 = content
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(base64);
    }
}