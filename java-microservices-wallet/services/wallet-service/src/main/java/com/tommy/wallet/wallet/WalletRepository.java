package com.tommy.wallet.wallet;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class WalletRepository {

    private final JdbcClient jdbcClient;

    public WalletRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Wallet> findByUserId(UUID userId) {
        return jdbcClient.sql("""
                SELECT user_id, balance, currency
                FROM wallets
                WHERE user_id = :userId
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new Wallet(
                        rs.getObject("user_id", UUID.class),
                        rs.getLong("balance"),
                        rs.getString("currency")
                ))
                .optional();
    }

    public Optional<Wallet> findByUserIdForUpdate(UUID userId) {
        return jdbcClient.sql("""
                SELECT user_id, balance, currency
                FROM wallets
                WHERE user_id = :userId
                FOR UPDATE
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new Wallet(
                        rs.getObject("user_id", UUID.class),
                        rs.getLong("balance"),
                        rs.getString("currency")
                ))
                .optional();
    }

    public void updateBalance(UUID userId, long balance) {
        int updated = jdbcClient.sql("""
                UPDATE wallets
                SET balance = :balance
                WHERE user_id = :userId
                """)
                .param("balance", balance)
                .param("userId", userId)
                .update();

        if (updated != 1) {
            throw new IllegalStateException("Wallet update affected no row.");
        }
    }
}