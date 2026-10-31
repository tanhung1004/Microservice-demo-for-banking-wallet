package com.tommy.login.auth;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
    public record UserInfo(
            UUID id,
            String username
    ) {
    }

    @Override
    public String toString() {
        return "LoginResponse[tokenType=" + tokenType
                + ", expiresIn=" + expiresIn
                + ", user=" + user + "]";
    }
}