package com.github.galleog.sk8s.account;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for <a href="https://projects.spring.io/spring-data-jpa/">Spring Data JPA</a> auditing.
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {
}
