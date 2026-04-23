package org.example.wishlistservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS is handled by the API Gateway. Avoid registering global CORS mappings here
        // to prevent duplicate Access-Control-Allow-Origin headers.
        // Leaving this method empty is intentional.
    }
}
