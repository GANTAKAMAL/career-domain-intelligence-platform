package com.careerdomain.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/** Lets Spring Boot own the driver, template and transaction manager. */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.careerdomain.repository")
@Profile("!test")
public class Neo4jConfiguration {
    private final String uri;
    private final String username;
    private final String password;
    private final String databasePassword;
    private final String authenticationSecret;

    public Neo4jConfiguration(
            @Value("${spring.neo4j.uri}") String uri,
            @Value("${spring.neo4j.authentication.username}") String username,
            @Value("${spring.neo4j.authentication.password}") String password,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${app.auth.secret}") String authenticationSecret) {
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.databasePassword = databasePassword;
        this.authenticationSecret = authenticationSecret;
    }

    /** The graph is required; never try a guessed or anonymous password. */
    @PostConstruct
    void validateRequiredCredentials() {
        if (uri == null || uri.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Knowledge Graph configuration is incomplete. Set NEO4J_URI, "
                            + "NEO4J_USERNAME and NEO4J_PASSWORD before starting the application.");
        }
        if (databasePassword == null || databasePassword.isBlank()) {
            throw new IllegalStateException(
                    "Database configuration is incomplete. Set DB_PASSWORD before starting the application.");
        }
        if (authenticationSecret == null || authenticationSecret.length() < 32) {
            throw new IllegalStateException(
                    "Authentication configuration is incomplete. Set APP_AUTH_SECRET to a value of at least 32 characters.");
        }
    }
}
