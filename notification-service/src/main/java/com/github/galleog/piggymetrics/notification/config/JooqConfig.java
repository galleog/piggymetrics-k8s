package com.github.galleog.piggymetrics.notification.config;

import static com.github.galleog.piggymetrics.notification.domain.Public.PUBLIC;

import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.repository.jooq.NotificationSettingsRecordMapper;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordType;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultRecordMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures database schema for <a href="https://www.jooq.org/">jOOQ</a>.
 */
@Configuration
public class JooqConfig {
    private static final String SCHEMA_NAME = "notification_service";

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
    public RecordMapperProvider recordMapperProvider() {
        return new RecordMapperProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> type) {
                if (NotificationSettings.class.equals(type)) {
                    return new NotificationSettingsRecordMapper();
                }
                return new DefaultRecordMapper<>(recordType, type);
            }
        };
    }
}
