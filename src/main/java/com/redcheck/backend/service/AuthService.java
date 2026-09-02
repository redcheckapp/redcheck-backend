package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDTO register(RegisterRequestDTO requestDTO) {
        // Check if the email already exists (error handling to be improved in future versions)
        if (userRepository.existsByEmail(requestDTO.email())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user with the encrypted password
        User user = User.builder()
                .username(requestDTO.username())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(requestDTO.password()))
                .build();

        userRepository.save(user);

        // Generate and return token
        String jwtToken = jwtService.generateToken(user);

        return AuthResponseDTO.builder()
                .token(jwtToken)
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO requestDTO) {
        // Authenticate user (throws exception if credentials are invalid)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        requestDTO.email(),
                        requestDTO.password()
                )
        );

        // Retrieve the authenticated user
        User user = userRepository.findByEmail(requestDTO.email())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user);

        return AuthResponseDTO.builder()
                .token(jwtToken)
                .build();
    }
}