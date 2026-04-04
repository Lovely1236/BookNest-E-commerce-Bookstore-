package org.example.authservice.service;

import org.example.authservice.config.JwtUtil;
import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.entity.User;
import org.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // REGISTER
    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMobile(request.getMobile());
        user.setRole("ROLE_CUSTOMER");

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token, "User registered successfully");
    }
    // LOGIN
    @Override
    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(email);
    }

    // LOGOUT (JWT is stateless)
    @Override
    public void logout(String token) {
        // No server-side logic needed
        // Client should delete token
    }

    // VALIDATE TOKEN
    @Override
    public boolean validateToken(String token) {

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return jwtUtil.validateToken(token);
    }
    // REFRESH TOKEN
    @Override
    public String refreshToken(String token) {

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        String email = jwtUtil.extractEmail(token);

        return jwtUtil.generateToken(email);
    }
    // GET USER
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).get();
    }

    //  CHANGE PASSWORD
    @Override
    public void changePassword(Long userId, String newPassword) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }
}