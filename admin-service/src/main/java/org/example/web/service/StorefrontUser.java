package org.example.web.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StorefrontUser {
    private boolean authenticated;
    private String token;
    private boolean admin;

    public boolean authenticated() { return authenticated; }
    public String token() { return token; }
    public boolean isAdmin() { return admin; }
}
