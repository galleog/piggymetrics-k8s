package com.github.galleog.piggymetrics.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main <a href="https://projects.spring.io/spring-boot/">Spring Boot</a> application class.
 */
@SpringBootApplication
@EnableTransactionManagement
public class StatisticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsApplication.class, args);
    }
}
