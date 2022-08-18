package com.github.galleog.piggymetrics.notification;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main <a href="https://projects.spring.io/spring-boot/">Spring Boot</a> application class.
 */
@EnableScheduling
@SpringBootApplication
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@EnableTransactionManagement
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
