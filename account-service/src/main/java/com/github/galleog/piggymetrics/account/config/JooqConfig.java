package com.github.galleog.piggymetrics.account.config;

import static com.github.galleog.piggymetrics.account.domain.Public.PUBLIC;

import com.github.galleog.piggymetrics.account.domain.Item;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.ItemRecordMapper;
import com.github.galleog.piggymetrics.account.repository.SavingRecordMapper;
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
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

/**
 * Configures database schema for <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Configuration
public class JooqConfig {
    private static final String SCHEMA_NAME = "account_service";

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int connectionPoolSize;

    @Bean
    @Profile("!test")
    public Settings settings() {
        return new Settings()
                .withRenderMapping(
                        new RenderMapping()
                                .withSchemata(
                                        new MappedSchema()
                                                .withInput(PUBLIC.getName())
                                                .withOutput(SCHEMA_NAME)
                                )
                );
    }

    @Bean
    public Scheduler jdbcScheduler() {
        return Schedulers.fromExecutorService(Executors.newFixedThreadPool(connectionPoolSize));
    }

    @Bean
    public RecordMapperProvider recordMapperProvider() {
        return new RecordMapperProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> type) {
                if (Item.class.equals(type)) {
                    return new ItemRecordMapper();
                }
                if (Saving.class.equals(type)) {
                    return new SavingRecordMapper();
                }
                return new DefaultRecordMapper<>(recordType, type);
            }
        };
    }
}
