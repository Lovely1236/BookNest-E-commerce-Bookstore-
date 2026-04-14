package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI adminOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info().title("Admin Service API").version("v0.0.1").description("Admin service API documentation"));
        // If an OPENAPI_SERVER_URL is provided, add it to the generated OpenAPI servers.
        if (openapiServerUrl != null && !openapiServerUrl.isBlank()) {
            Server s = new Server();
            s.setUrl(openapiServerUrl);
            openAPI.setServers(Collections.singletonList(s));
        }
        return openAPI;
    }

    @Value("${OPENAPI_SERVER_URL:}")
    private String openapiServerUrl;
}
