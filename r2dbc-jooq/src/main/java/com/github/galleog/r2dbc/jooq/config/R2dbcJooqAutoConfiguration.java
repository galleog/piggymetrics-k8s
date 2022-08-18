package com.github.galleog.r2dbc.jooq.config;

import com.github.galleog.r2dbc.jooq.transaction.TransactionAwareJooqWrapper;
import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Auto-configuration for the <a href="https://www.jooq.org/">JOOQ database library</a> using R2DBC {@link ConnectionFactory}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DatabaseClient.class)
@EnableConfigurationProperties(JooqProperties.class)
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
public class R2dbcJooqAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    TransactionAwareJooqWrapper transactionAwareJooqWrapper(DatabaseClient databaseClient, JooqProperties properties,
                                                            ObjectProvider<Settings> settings) {
        return new TransactionAwareJooqWrapper(databaseClient, properties.getSqlDialect(), settings.getIfAvailable());
    }
}
