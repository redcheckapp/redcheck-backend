package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.UserResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
                .email(currentUser.getEmail())
                .build();
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(){
        // We obtain the authenticated user via security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Comprobación para la cuenta de demo en español
        if("demo-es@redcheck.com".equalsIgnoreCase(currentUser.getEmail())){
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Acción denegada. La cuenta de demostración no puede ser eliminada.\"}");
        }

        // Comprobación para la cuenta de demo en inglés
        if("demo-en@redcheck.com".equalsIgnoreCase(currentUser.getEmail())){
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Action denied. The demo account cannot be deleted.\"}");
        }

        userService.deleteUser(currentUser.getEmail());

        return ResponseEntity.noContent().build();
    }
}
