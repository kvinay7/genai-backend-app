package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.example.backend.filters.AuthFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Safe for stateless API
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())  // Permit all, but AuthFilter enforces token check
            .addFilterBefore(new AuthFilter(), BasicAuthenticationFilter.class);  // Explicitly add AuthFilter to chain
        return http.build();
    }
}