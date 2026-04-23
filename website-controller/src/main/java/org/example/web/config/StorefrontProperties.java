package org.example.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storefront")
public record StorefrontProperties(
        Long userId,
        String email,
        String fullName,
        Long mobile,
        String membership,
        Long walletId
) {
}
