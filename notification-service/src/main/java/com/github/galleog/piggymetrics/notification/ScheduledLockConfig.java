package com.github.galleog.piggymetrics.notification;

import lombok.Getter;
import lombok.Setter;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.ScheduledLockConfiguration;
import net.javacrumbs.shedlock.spring.ScheduledLockConfigurationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import java.time.Duration;

/**
 * Configuration for <a href="https://github.com/lukas-krecan/ShedLock">ShedLock</a>.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties("shedlock")
public class ScheduledLockConfig {
    @Positive
    private int poolSize;
    @Min(0)
    private int lockAtMostFor;
    @Min(0)
    private int lockAtLeastFor;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @Bean
    public ScheduledLockConfiguration taskScheduler(LockProvider lockProvider) {
        return ScheduledLockConfigurationBuilder
                .withLockProvider(lockProvider)
                .withPoolSize(poolSize)
                .withDefaultLockAtMostFor(Duration.ofMinutes(lockAtMostFor))
                .withDefaultLockAtLeastFor(Duration.ofMinutes(lockAtLeastFor))
                .build();
    }
}
