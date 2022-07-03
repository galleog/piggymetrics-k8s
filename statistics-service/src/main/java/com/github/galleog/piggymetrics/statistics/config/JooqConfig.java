package com.github.galleog.piggymetrics.statistics.config;

import static com.github.galleog.piggymetrics.statistics.domain.Public.PUBLIC;

import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.repository.jooq.ItemMetricRecordMapper;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordType;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;

/**
 * Configures database schema for <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Configuration(proxyBeanMethods = false)
public class JooqConfig {
    @Value("${spring.jooq.schema}")
    private String schema;

    @Bean
    @Profile("!test")
    public Settings settings() {
        return new Settings()
                .withRenderMapping(
                        new RenderMapping()
                                .withSchemata(
                                        new MappedSchema()
                                                .withInput(PUBLIC.getName())
                                                .withOutput(schema)
                                )
                );
    }

    @Bean
    public RecordMapperProvider recordMapperProvider() {
        return new RecordMapperProvider() {
            @Override
            @NonNull
            @SuppressWarnings("unchecked")
            public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> type) {
                if (ItemMetric.class.equals(type)) {
                    return new ItemMetricRecordMapper();
                }
                return new DefaultRecordMapper<>(recordType, type);
            }
        };
    }
}
