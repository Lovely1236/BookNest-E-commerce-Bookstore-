package org.example.web.service;

public interface CurrentUserService {
    StorefrontUser currentUser(String authorizationHeader);
}
