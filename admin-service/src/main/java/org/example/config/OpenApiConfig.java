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

    @Value("${OPENAPI_SERVER_URL:http://localhost:8095}")
    private String openapiServerUrl;

    @Bean
    public OpenAPI adminOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info().title("Admin Service API").version("v0.0.1").description("Admin service API documentation"));
        
        // Always add the configured server URL
        if (openapiServerUrl != null && !openapiServerUrl.isBlank()) {
            Server server = new Server();
            server.setUrl(openapiServerUrl);
            server.setDescription("Admin service server");
            openAPI.setServers(Collections.singletonList(server));
        }
        
        return openAPI;
    }
}
