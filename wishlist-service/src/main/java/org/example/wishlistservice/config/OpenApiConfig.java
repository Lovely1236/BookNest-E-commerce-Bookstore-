package org.example.wishlistservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wishlistOpenApi(
            @Value("${openapi.server.url}") String gatewayServerUrl) {
        return new OpenAPI()
                .servers(List.of(new Server().url(gatewayServerUrl)));
    }
}
