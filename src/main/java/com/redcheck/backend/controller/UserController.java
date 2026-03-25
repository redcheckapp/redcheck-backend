package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.UserResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getUsername(@AuthenticationPrincipal User currentUser){
        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .username(currentUser.getActualUsername())
                .build();
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(){
        // We obtain the authenticated user via security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.deleteUser(currentUser.getEmail());

        return ResponseEntity.noContent().build();
    }
}
