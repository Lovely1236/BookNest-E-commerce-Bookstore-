package org.example.web.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SimpleCurrentUserService implements CurrentUserService {

    @Override
    public StorefrontUser currentUser(HttpSession session) {
        // Minimal placeholder implementation: unauthenticated user
        return new StorefrontUser(false, null, false);
    }
}
