package org.example.web.service;

public record StorefrontUser(
                Long userId,
                String email,
                String fullName,
                Long mobile,
                String membership,
                String token,
                Long walletId,
                String role,
                boolean authenticated
) {
        public boolean isAdmin() {
                return role != null && (role.equalsIgnoreCase("ROLE_ADMIN") || role.equalsIgnoreCase("ADMIN"));
        }
}
