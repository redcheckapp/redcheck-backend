package com.redcheck.backend.config;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Checks if the user already exists so that we dont duplicate it
        if (!userRepository.existsByEmail("demo@redcheck.com")) {
            User demoUser = new User();
            demoUser.setUsername("Usuario Demo");
            demoUser.setEmail("demo@redcheck.com");
            // Same password as React
            demoUser.setPassword(passwordEncoder.encode("demo1234"));
            demoUser.setCreationDate(LocalDateTime.now());

            userRepository.save(demoUser);
            System.out.println("Usuario de demostración creado con éxito.");
        }
    }
}