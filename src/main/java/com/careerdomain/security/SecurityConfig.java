package com.careerdomain.security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import jakarta.servlet.DispatcherType;
import org.springframework.web.cors.*;
import java.util.List;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SecurityConfig {
    private final BearerTokenFilter bearerTokenFilter;
    private final String allowedOrigins;
    public SecurityConfig(BearerTokenFilter bearerTokenFilter, @Value("${app.cors.allowed-origins}") String allowedOrigins) { this.bearerTokenFilter = bearerTokenFilter; this.allowedOrigins = allowedOrigins; }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> { })

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
