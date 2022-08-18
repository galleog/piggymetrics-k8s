package com.github.galleog.r2dbc.jooq.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.r2dbc.jooq.transaction.TransactionAwareJooqWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Tests for {@link R2dbcJooqAutoConfiguration}.
 */
class R2dbcJooqAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(R2dbcJooqAutoConfiguration.class));

    /**
     * Test for failed auto-configuration without {@link DatabaseClient}.
     */
    @Test
    void shouldNotCreateTransactionAwareJooqWrapperWhenNoDatabaseClientIsAvailable() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(TransactionAwareJooqWrapper.class));
    }

    /**
     * Test for succeeded auto-configuration.
     */
    @Test
    void shouldCreateTransactionAwareJooqWrapper() {
        contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(TransactionAwareJooqWrapper.class));
    }
}