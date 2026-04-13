package org.example.authservice.controller;

import org.example.authservice.config.JwtUtil;
import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.entity.User;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${website.url:http://localhost:8090}")
    private String websiteUrl;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String token = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(
                new AuthResponse(token, "Login successful")
        );
    }
    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    // VALIDATE TOKEN
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(authService.validateToken(token));
    }

    // REFRESH TOKEN
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(Map.of("token", authService.refreshToken(token)));
    }

    // PROFILE
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestParam String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @GetMapping("/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtUtil.extractEmail(token);
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    // CHANGE PASSWORD
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestParam Long userId,
                                            @RequestParam String newPassword) {
        authService.changePassword(userId, newPassword);
        return ResponseEntity.ok("Password updated successfully");
    }
    @GetMapping("/oauth-success")
    public ResponseEntity<?> oauthSuccess(Authentication authentication) {

        String email = authentication.getName();

        // check if user exists
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setRole("ROLE_CUSTOMER");
                    newUser.setProvider("GITHUB");
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(email);

        // Redirect to website callback with token so UI can store session
        String redirect = websiteUrl + "/auth/callback?token=" + token;
        return ResponseEntity.status(302).header("Location", redirect).build();
    }
}
