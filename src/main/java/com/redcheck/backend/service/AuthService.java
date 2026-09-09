package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.GoogleAuthRequestDTO;
import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.InvalidGoogleTokenException;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.security.GoogleTokenVerifierService;
import com.redcheck.backend.security.GoogleUserInfo;
import com.redcheck.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthResponseDTO register(RegisterRequestDTO requestDTO) {
        // Check if the email or username already exists (error handling to be improved in future versions)
        if (userRepository.existsByEmail(requestDTO.email())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(requestDTO.username())) {
            throw new RuntimeException("Username already registered");
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
        // Resolve the identifier (either email or username, both unique) to
        // the actual user first — UserDetails/Spring Security's own
        // authentication flow in this app is keyed on email (see
        // User#getUsername), so authenticate() below always needs the real
        // email regardless of which identifier the caller typed. Thrown as
        // the same BadCredentialsException a wrong password would produce,
        // so "no such user" and "wrong password" aren't distinguishable
        // from the response — same as before this change, just now with an
        // extra case that can hit it.
        User user = userRepository.findByEmailOrUsername(requestDTO.emailOrUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Authenticate user (throws exception if the password is invalid)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        requestDTO.password()
                )
        );

        String jwtToken = jwtService.generateToken(user);

        return AuthResponseDTO.builder()
                .token(jwtToken)
                .build();
    }

    public AuthResponseDTO loginWithGoogle(GoogleAuthRequestDTO requestDTO) {
        GoogleUserInfo googleUser = googleTokenVerifierService.verify(requestDTO.idToken());

        if (!googleUser.emailVerified()) {
            throw new InvalidGoogleTokenException();
        }

        User user = userRepository.findByGoogleId(googleUser.googleId())
                .orElseGet(() -> userRepository.findByEmail(googleUser.email())
                        // An existing password-based account signing in with Google for
                        // the first time gets linked in place, rather than this trying
                        // (and failing) to insert a second row against the unique email
                        // constraint.
                        .map(existing -> {
                            existing.setGoogleId(googleUser.googleId());
                            return userRepository.save(existing);
                        })
                        .orElseGet(() -> createGoogleUser(googleUser)));

        String jwtToken = jwtService.generateToken(user);

        return AuthResponseDTO.builder()
                .token(jwtToken)
                .build();
    }

    private User createGoogleUser(GoogleUserInfo googleUser) {
        User user = User.builder()
                .username(generateUniqueUsername(googleUser))
                .email(googleUser.email())
                .googleId(googleUser.googleId())
                .build();

        return userRepository.save(user);
    }

    // Google gives no username, only an email and a display name — derive a
    // reasonable, URL/display-safe candidate from whichever is available and
    // disambiguate against existing usernames (unique, same as email).
    private String generateUniqueUsername(GoogleUserInfo googleUser) {
        String source = googleUser.name() != null ? googleUser.name() : googleUser.email().split("@")[0];
        String base = source.toLowerCase().replaceAll("[^a-z0-9]+", "");
        if (base.isBlank()) {
            base = "user";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}