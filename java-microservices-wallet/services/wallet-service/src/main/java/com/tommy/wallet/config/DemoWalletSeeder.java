package com.tommy.wallet.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class DemoWalletSeeder implements CommandLineRunner {

    private final JdbcClient jdbcClient;

    public DemoWalletSeeder(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void run(String... args) {
        // UUID phải trùng với hai tài khoản demo bên Login.
        createWalletIfMissing(
                UUID.fromString("11111111-1111-4111-8111-111111111111")
        );

        createWalletIfMissing(
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );
    }

    private void createWalletIfMissing(UUID userId) {
        jdbcClient.sql("""
                INSERT INTO wallets (user_id, balance, currency)
                VALUES (:userId, 0, 'VND')
                ON CONFLICT (user_id) DO NOTHING
                """)
                .param("userId", userId)
                .update();
    }
}