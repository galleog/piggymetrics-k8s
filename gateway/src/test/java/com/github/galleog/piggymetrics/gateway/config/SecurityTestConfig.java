package com.github.galleog.piggymetrics.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Test configuration that disables WebFlux security.
 */
@TestConfiguration
public class SecurityTestConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.authorizeExchange()
                .anyExchange().permitAll()
                .and()
                .csrf().disable();
        return http.build();
    }
}
