package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.ForgotPasswordRequestDTO;
import com.redcheck.backend.dto.request.GoogleAuthRequestDTO;
import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.request.ResetPasswordRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.exception.InvalidResetTokenException;
import com.redcheck.backend.service.AuthService;
import com.redcheck.backend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO requestDTO) {
        return ResponseEntity.ok(authService.register(requestDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO requestDTO) {
        return ResponseEntity.ok(authService.login(requestDTO));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> loginWithGoogle(@RequestBody GoogleAuthRequestDTO requestDTO) {
        return ResponseEntity.ok(authService.loginWithGoogle(requestDTO));
    }

    // Always responds 200 with no body regardless of whether the email is
    // registered — see PasswordResetService#forgotPassword. Not
    // distinguishing the two here is deliberate: a different response would
    // let a caller enumerate which emails have a RedCheck account.
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequestDTO requestDTO) {
        passwordResetService.forgotPassword(requestDTO.email(), requestDTO.lang());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO requestDTO) {
        try {
            passwordResetService.resetPassword(requestDTO.token(), requestDTO.newPassword());
        } catch (InvalidResetTokenException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }

        return ResponseEntity.noContent().build();
    }
}