package com.tommy.login.auth;

import java.nio.charset.StandardCharsets;

import com.tommy.login.user.User;
import com.tommy.login.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;

        // Vẫn kiểm tra hash khi username không tồn tại.
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    public LoginResponse login(LoginRequest request) {
        validate(request);

        var foundUser = userRepository.findByUsername(request.username());

        String storedHash = foundUser
                .map(User::passwordHash)
                .orElse(dummyHash);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                storedHash
        );

        if (foundUser.isEmpty() || !passwordMatches) {
            throw new AuthException(
                    401,
                    "INVALID_CREDENTIALS",
                    "Tên đăng nhập hoặc mật khẩu không đúng."
            );
        }

        User user = foundUser.orElseThrow();

        return new LoginResponse(
                jwtService.createAccessToken(user.id()),
                "Bearer",
                JwtService.EXPIRES_IN_SECONDS,
                new LoginResponse.UserInfo(user.id(), user.username())
        );
    }

    private void validate(LoginRequest request) {
        if (request == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()) {

            throw new AuthException(
                    400,
                    "VALIDATION_ERROR",
                    "Vui lòng nhập tên đăng nhập và mật khẩu."
            );
        }

        if (request.username().length() > 50
                || request.password()
                        .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new AuthException(
                    400,
                    "VALIDATION_ERROR",
                    "Tên đăng nhập hoặc mật khẩu vượt giới hạn độ dài."
            );
        }
    }
}