package com.github.galleog.piggymetrics.autoconfigure.jooq;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import io.r2dbc.spi.ConnectionFactory;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep1;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Integration tests for {@link TransactionAwareJooqWrapper}.
 */
@AutoConfigureDataR2dbc
@ImportAutoConfiguration(R2dbcJooqAutoConfiguration.class)
@SpringBootTest(classes = TransactionAwareJooqWrapperIntegrationTest.Config.class)
class TransactionAwareJooqWrapperIntegrationTest {
    private static final String TABLE_NAME = "test";
    private static final String FIELD_NAME = "name";

    @Autowired
    private TransactionAwareJooqWrapper wrapper;
    @Autowired
    private TransactionalOperator operator;
    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        var operation = deleteAllFrom(TABLE_NAME);

        var dbSetup = new DbSetup(DataSourceDestination.with(dataSource), operation);
        dbSetup.launch();
    }

    /**
     * Jooq doesn't participate in Spring transactions because it loses Reactor context.
     */
    @Test
    void shouldNotParticipateInCommitTransaction() {
        var ctx = DSL.using(new TransactionAwareConnectionFactoryProxy(connectionFactory), SQLDialect.H2);
        Mono.from(insert(ctx))
                .flatMap(r -> Mono.error(CustomException::new))
                .as(operator::transactional)
                .as(StepVerifier::create)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(CustomException.class);
                    var test = new Table(dataSource, TABLE_NAME);
                    Assertions.assertThat(test).hasNumberOfRows(1);
                    return true;
                }).verify(Duration.ofSeconds(3));
    }

    /**
     * No rows should be inserted after transaction rollback.
     */
    @Test
    void shouldInsertRowAfterCommit() {
        wrapper.withDSLContext(this::insert)
                .flatMap(r -> Mono.error(CustomException::new))
                .as(operator::transactional)
                .as(StepVerifier::create)
                .expectErrorMatches(t -> {
                    assertThat(t).isInstanceOf(CustomException.class);
                    var test = new Table(dataSource, TABLE_NAME);
                    Assertions.assertThat(test).isEmpty();
                    return true;
                }).verify(Duration.ofSeconds(3));
    }

    /**
     * A row should be inserted after transaction commit.
     */
    @Test
    void shouldNotInsertRowAfterRollback() {
        wrapper.withDSLContext(this::insert)
                .as(operator::transactional)
                .as(StepVerifier::create)
                .expectNextMatches(i -> {
                    assertThat(i).isEqualTo(1);
                    var test = new Table(dataSource, TABLE_NAME);
                    Assertions.assertThat(test).hasNumberOfRows(1);
                    return true;
                }).verifyComplete();
    }

    private InsertValuesStep1<Record, Object> insert(DSLContext ctx) {
        return ctx.insertInto(table(TABLE_NAME), field(FIELD_NAME))
                .values("foo");
    }

    @EnableTransactionManagement
    @Configuration(proxyBeanMethods = false)
    @Import(EmbeddedDataSourceConfiguration.class)
    static class Config {
    }

    private static class CustomException extends RuntimeException {
    }
}