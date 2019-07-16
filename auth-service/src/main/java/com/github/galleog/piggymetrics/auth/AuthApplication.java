package com.github.galleog.piggymetrics.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main <a href="https://projects.spring.io/spring-boot/">Spring Boot</a> application class.
 */
@SpringBootApplication
@EnableBinding(Source.class)
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
