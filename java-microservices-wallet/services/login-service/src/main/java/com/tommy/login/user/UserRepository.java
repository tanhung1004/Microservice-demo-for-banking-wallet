package com.tommy.login.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // Tìm tài khoản theo username.
    public Optional<User> findByUsername(String username) {
        return jdbcClient.sql("""
                SELECT id, username, password_hash
                FROM users
                WHERE username = :username
                """)
                .param("username", username)
                .query((rs, rowNum) -> new User(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("password_hash")
                ))
                .optional();
    }

    // Chỉ thêm khi username chưa tồn tại.
    public void createIfAbsent(User user) {
        jdbcClient.sql("""
                INSERT INTO users (id, username, password_hash)
                VALUES (:id, :username, :passwordHash)
                ON CONFLICT (username) DO NOTHING
                """)
                .param("id", user.id())
                .param("username", user.username())
                .param("passwordHash", user.passwordHash())
                .update();
    }
}