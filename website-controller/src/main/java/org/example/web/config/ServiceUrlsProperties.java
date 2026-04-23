package org.example.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ServiceUrlsProperties(
        String auth,
        String book,
        String cart,
        String order,
        String wallet,
        String review,
        String notification,
        String wishlist
) {
}
