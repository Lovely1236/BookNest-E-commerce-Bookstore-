package org.example.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class WebsiteControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsiteControllerApplication.class, args);
    }
}
