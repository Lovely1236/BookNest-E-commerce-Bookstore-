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
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RestTemplate restTemplate;

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

        AuthResponse response = authService.login(
            request.getEmail(),
            request.getPassword()
        );

        return ResponseEntity.ok(response);
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
    public void oauthSuccess(Authentication authentication, HttpServletResponse response) throws IOException {
        if (authentication == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        String email = authentication.getName();
        logger.info("OAuth2 login successful for email: {}", email);

        // Check if user exists, create if not
        boolean isNewUser = !userRepository.existsByEmail(email);
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setRole("ROLE_CUSTOMER");
                    newUser.setProvider("GITHUB");
                    return userRepository.save(newUser);
                });

        // Create wallet for new GitHub OAuth users
        if (isNewUser) {
            try {
                String walletServiceUrl = "http://wallet-service:8085/wallet/create";
                restTemplate.postForObject(walletServiceUrl, null, Object.class);
                logger.info("Wallet created successfully for GitHub OAuth user: {}", user.getUserId());
            } catch (Exception e) {
                logger.warn("Failed to create wallet for GitHub OAuth user: {}. Error: {}", user.getUserId(), e.getMessage());
            }
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(email);
        logger.info("Generated JWT token for user: {}", email);

        // Redirect to frontend with token - using hardcoded URL for now
        String redirectUrl = "http://localhost:5173/auth/callback?token=" + token;
        logger.info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
