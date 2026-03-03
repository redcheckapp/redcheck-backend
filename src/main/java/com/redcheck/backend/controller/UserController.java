package com.redcheck.backend.controller;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(){
        // We obtain the authenticated user via security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        userService.deleteUser(currentUser.getEmail());

        return ResponseEntity.noContent().build();
    }
}
