package com.github.galleog.r2dbc.jooq.transaction;

import io.r2dbc.spi.Connection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Publisher;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.springframework.r2dbc.core.ConnectionAccessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Wrapper for Jooq queries that allows them to participate in Spring transactions.
 */
@RequiredArgsConstructor
public class TransactionAwareJooqWrapper {
    private final ConnectionAccessor connectionAccessor;
    private final SQLDialect sqlDialect;
    private final Settings settings;

    /**
     * Executes a Jooq query within a transaction-aware connection.
     *
     * @param fn the Jooq query that is executed within the connection
     * @return the resulting {@link Mono}
     */
    public <T> Mono<T> withDSLContext(Function<DSLContext, Publisher<T>> fn) {
        return connectionAccessor.inConnection(con -> Mono.from(fn.apply(createDSLContext(con))));
    }

    /**
     * Executes a Jooq query within a transaction-aware connection.
     *
     * @param fn the Jooq query that is executed within the connection
     * @return the resulting {@link Flux}
     */
    public <T> Flux<T> withDSLContextMany(Function<DSLContext, Publisher<T>> fn) {
        return connectionAccessor.inConnectionMany(con -> Flux.from(fn.apply(createDSLContext(con))));
    }

    private DSLContext createDSLContext(Connection connection) {
        return DSL.using(connection, sqlDialect, settings);
    }
}
