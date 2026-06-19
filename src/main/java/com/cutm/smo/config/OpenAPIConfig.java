package com.cutm.smo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PALMS API")
                        .version("1.0.0")
                        .description("Parallel Assembly Line Management System - RESTful API for garment factory management")
                        .contact(new Contact()
                                .name("Prem Sai Bollamoni")
                                .email("premsai200804@gmail.com")
                                .url("https://github.com/PremSaiBollamoni"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://github.com/PremSaiBollamoni/PALMS-Backend")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.palms.local")
                                .description("Production Server"),
                        new Server()
                                .url("http://192.168.0.102:8080")
                                .description("Local Network Server")
                ));
    }
}
