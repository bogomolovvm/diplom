package org.example.diplom.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.diplom.model.User;
import org.example.diplom.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                    new User(null, "user", passwordEncoder.encode("user")),
                    new User(null, "admin", passwordEncoder.encode("password"))
            ));
            log.info("DataInitializer: тестовые пользователи созданы");
        } else {
            log.info("DataInitializer: пользователи уже существуют, пропускаем");
        }
    }
}
