package com.tommy.login.config;

import java.util.UUID;

import com.tommy.login.user.User;
import com.tommy.login.user.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class DemoUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Tài khoản mẫu chỉ dùng trong môi trường local.
        createDemoUser(
                "11111111-1111-4111-8111-111111111111",
                "demo1",
                "Demo123!"
        );

        createDemoUser(
                "22222222-2222-4222-8222-222222222222",
                "demo2",
                "Demo456!"
        );
    }

    private void createDemoUser(
            String id,
            String username,
            String rawPassword
    ) {
        UUID expectedId = UUID.fromString(id);

        // Chỉ tạo khi chưa có, không ghi đè mật khẩu đã lưu.
        if (userRepository.findByUsername(username).isEmpty()) {
            User newUser = new User(
                    expectedId,
                    username,
                    passwordEncoder.encode(rawPassword)
            );

            userRepository.createIfAbsent(newUser);
        }

        // Kiểm tra UUID khớp với thiết kế để liên kết ví sau này.
        User savedUser = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Demo user was not created: " + username
                        )
                );

        if (!savedUser.id().equals(expectedId)) {
            throw new IllegalStateException(
                    "Demo user UUID does not match: " + username
            );
        }
    }
}