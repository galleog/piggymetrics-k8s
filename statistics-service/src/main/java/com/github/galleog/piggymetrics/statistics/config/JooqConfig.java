package com.github.galleog.piggymetrics.statistics.config;

import static com.github.galleog.piggymetrics.statistics.domain.Public.PUBLIC;

import com.github.galleog.piggymetrics.autoconfigure.jooq.JooqProperties;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures database schema for <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Profile("!test")
@Configuration(proxyBeanMethods = false)
public class JooqConfig {
    @Bean
    public Settings settings(JooqProperties properties) {
        return new Settings()
                .withRenderMapping(
                        new RenderMapping()
                                .withSchemata(
                                        new MappedSchema()
                                                .withInput(PUBLIC.getName())
                                                .withOutput(properties.getSchema())
                                )
                );
    }
}
