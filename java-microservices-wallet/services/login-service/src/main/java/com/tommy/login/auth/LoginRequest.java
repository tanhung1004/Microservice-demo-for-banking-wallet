package com.tommy.login.auth;

public record LoginRequest(
        String username,
        String password
) {
    @Override
    public String toString() {
        return "LoginRequest[password=REDACTED]";
    }
}