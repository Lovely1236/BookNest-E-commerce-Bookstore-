package org.example.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleCurrentUserService implements CurrentUserService {

    private final BackendGateway backendGateway;

    @Override
    public StorefrontUser currentUser(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            return new StorefrontUser(false, null, false);
        }

        try {
            var user = backendGateway.getMap(backendGateway.serviceUrls().auth() + "/me", token);
            String role = String.valueOf(user.getOrDefault("role", ""));
            boolean isAdmin = role.toUpperCase().contains("ADMIN");
            return new StorefrontUser(true, token, isAdmin);
        } catch (BackendGatewayException ex) {
            return new StorefrontUser(false, null, false);
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return authorizationHeader.trim();
    }
}
