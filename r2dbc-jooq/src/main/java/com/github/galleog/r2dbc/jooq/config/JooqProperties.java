package com.github.galleog.r2dbc.jooq.config;

import lombok.Getter;
import lombok.Setter;
import org.jooq.SQLDialect;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the <a href="https://www.jooq.org/">JOOQ database library</a>.
 */
@Getter
@Setter
@ConfigurationProperties("spring.jooq")
public class JooqProperties {
    private SQLDialect sqlDialect = SQLDialect.DEFAULT;
    private String schema;
}
