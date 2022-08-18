package com.github.galleog.piggymetrics.notification.config;

import io.r2dbc.spi.ConnectionFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.r2dbc.R2dbcLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for <a href="https://github.com/lukas-krecan/ShedLock">ShedLock</a>.
 */
@Profile("!test")
@Configuration(proxyBeanMethods = false)
public class ScheduledLockConfig {
    @Bean
    public LockProvider lockProvider(ConnectionFactory connectionFactory) {
        return new R2dbcLockProvider(connectionFactory);
    }
}
