package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.ChangePasswordRequestDTO;
import com.redcheck.backend.dto.response.UserResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.DemoAccountRestrictedException;
import com.redcheck.backend.exception.InvalidCurrentPasswordException;
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
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getUsername(@AuthenticationPrincipal User currentUser) {
        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .username(currentUser.getActualUsername())
                .email(currentUser.getEmail())
                .hasPassword(currentUser.getPassword() != null)
                .build();
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount() {
        // We obtain the authenticated user via security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Check for the Spanish demo account
        if ("demo-es@redcheck.com".equalsIgnoreCase(currentUser.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Acción denegada. La cuenta de demostración no puede ser eliminada.\"}");
        }

        // Check for the English demo account
        if ("demo-en@redcheck.com".equalsIgnoreCase(currentUser.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Action denied. The demo account cannot be deleted.\"}");
        }

        userService.deleteUser(currentUser.getEmail());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User currentUser,
                                             @RequestBody ChangePasswordRequestDTO requestDTO) {
        try {
            userService.changePassword(currentUser.getEmail(), requestDTO);
        } catch (DemoAccountRestrictedException e) {
            // Same bilingual-by-account convention as deleteMyAccount above.
            boolean spanish = "demo-es@redcheck.com".equalsIgnoreCase(currentUser.getEmail());
            String message = spanish
                    ? "Acción denegada. La contraseña de la cuenta de demostración no se puede cambiar."
                    : "Action denied. The demo account's password cannot be changed.";
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"" + message + "\"}");
        } catch (InvalidCurrentPasswordException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }

        return ResponseEntity.noContent().build();
    }
}