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

        if (!userRepository.existsByEmail("demo-es@redcheck.com")) {
            User demoUserEs = new User();
            demoUserEs.setUsername("Usuario Demo");
            demoUserEs.setEmail("demo-es@redcheck.com");
            demoUserEs.setPassword(passwordEncoder.encode("demo1234"));
            demoUserEs.setCreationDate(LocalDateTime.now());

            userRepository.save(demoUserEs);
            System.out.println("Usuario de demostración (ES) creado con éxito.");
        }

        if (!userRepository.existsByEmail("demo-en@redcheck.com")) {
            User demoUserEn = new User();
            demoUserEn.setUsername("Demo User");
            demoUserEn.setEmail("demo-en@redcheck.com");
            demoUserEn.setPassword(passwordEncoder.encode("demo1234"));
            demoUserEn.setCreationDate(LocalDateTime.now());

            userRepository.save(demoUserEn);
            System.out.println("Usuario de demostración (EN) creado con éxito.");
        }
    }
}