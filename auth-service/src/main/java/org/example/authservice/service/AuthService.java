package org.example.authservice.service;

import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(String email, String password);

    void logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    User getUserByEmail(String email);

    void changePassword(Long userId, String newPassword);
}