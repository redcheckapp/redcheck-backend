package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // So React can call this API
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody RegisterRequestDTO requestDTO){
        return ResponseEntity.ok(authService.register(requestDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO requestDTO){
        return ResponseEntity.ok(authService.login(requestDTO));
    }
}
