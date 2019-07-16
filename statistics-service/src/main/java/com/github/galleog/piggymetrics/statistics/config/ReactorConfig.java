package com.github.galleog.piggymetrics.statistics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

/**
 * Configuration for <a href="https://projectreactor.io/">Project Reactor</a>.
 */
@Configuration
public class ReactorConfig {
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int connectionPoolSize;

    @Bean
    public Scheduler jdbcScheduler() {
        return Schedulers.fromExecutorService(Executors.newFixedThreadPool(connectionPoolSize));
    }
}
