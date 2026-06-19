package com.cutm.smo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OpenAPIController {

    @GetMapping("/api-docs")
    public Map<String, Object> getOpenAPISpec() {
        return Map.of(
            "openapi", "3.0.0",
            "info", Map.of(
                "title", "PALMS API",
                "version", "1.0.0",
                "description", "Parallel Assembly Line Management System - RESTful API for garment factory management",
                "contact", Map.of(
                    "name", "Prem Sai Bollamoni",
                    "email", "premsai200804@gmail.com",
                    "url", "https://github.com/PremSaiBollamoni"
                )
            ),
            "servers", new Object[]{
                Map.of("url", "http://localhost:8080", "description", "Development Server"),
                Map.of("url", "https://api.palms.local", "description", "Production Server"),
                Map.of("url", "http://192.168.0.102:8080", "description", "Local Network Server")
            },
            "paths", Map.of(
                "/api/auth/login", Map.of(
                    "post", Map.of(
                        "summary", "Employee Login",
                        "description", "Authenticate employee and get access token",
                        "tags", new String[]{"Authentication"},
                        "requestBody", Map.of(
                            "required", true,
                            "content", Map.of(
                                "application/json", Map.of(
                                    "schema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                            "loginid", Map.of("type", "string"),
                                            "password", Map.of("type", "string")
                                        ),
                                        "required", new String[]{"loginid", "password"}
                                    )
                                )
                            )
                        ),
                        "responses", Map.of(
                            "200", Map.of("description", "Login successful"),
                            "401", Map.of("description", "Invalid credentials"),
                            "400", Map.of("description", "Missing required fields")
                        )
                    )
                ),
                "/api/hr/operations", Map.of(
                    "get", Map.of(
                        "summary", "List active operations",
                        "tags", new String[]{"Operations"},
                        "responses", Map.of(
                            "200", Map.of("description", "Operations list retrieved")
                        )
                    ),
                    "post", Map.of(
                        "summary", "Create new operation",
                        "tags", new String[]{"Operations"},
                        "responses", Map.of(
                            "200", Map.of("description", "Operation created"),
                            "409", Map.of("description", "Operation code already exists")
                        )
                    )
                ),
                "/api/attendance/today", Map.of(
                    "get", Map.of(
                        "summary", "Today's attendance records",
                        "tags", new String[]{"Attendance"},
                        "responses", Map.of(
                            "200", Map.of("description", "Attendance records retrieved")
                        )
                    )
                )
            ),
            "components", Map.of(
                "schemas", Map.of(
                    "LoginRequest", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "loginid", Map.of("type", "string"),
                            "password", Map.of("type", "string")
                        )
                    ),
                    "Operation", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "opId", Map.of("type", "integer"),
                            "opCode", Map.of("type", "string"),
                            "opName", Map.of("type", "string"),
                            "stage", Map.of("type", "string"),
                            "sam", Map.of("type", "number"),
                            "skillGrade", Map.of("type", "string"),
                            "targetPcs", Map.of("type", "integer"),
                            "sequenceNo", Map.of("type", "integer")
                        )
                    )
                )
            )
        );
    }

    @GetMapping("/swagger-ui.html")
    public String getSwaggerUI() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>PALMS API Documentation</title>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/4.15.5/swagger-ui.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/4.15.5/swagger-ui.min.js"></script>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script>
                    window.onload = function() {
                        SwaggerUIBundle({
                            url: "/api-docs",
                            dom_id: '#swagger-ui',
                            presets: [
                                SwaggerUIBundle.presets.apis,
                                SwaggerUIBundle.SwaggerUIStandalonePreset
                            ],
                            layout: "BaseLayout"
                        })
                    }
                </script>
            </body>
            </html>
            """;
    }
}
