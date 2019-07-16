package com.github.galleog.piggymetrics.account.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Test configuration for <a href="https://projectreactor.io/">Project Reactor</a>.
 */
@TestConfiguration
public class ReactorTestConfig {
    @Bean
    public Scheduler jdbcScheduler() {
        return Schedulers.immediate();
    }
}
