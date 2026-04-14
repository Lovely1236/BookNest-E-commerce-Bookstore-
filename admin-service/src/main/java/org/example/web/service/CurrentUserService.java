package org.example.web.service;

import jakarta.servlet.http.HttpSession;

public interface CurrentUserService {
    StorefrontUser currentUser(HttpSession session);
}
