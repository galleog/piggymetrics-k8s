package com.github.galleog.piggymetrics.notification.repository.jooq;

import static com.github.galleog.piggymetrics.notification.domain.NotificationType.BACKUP;
import static com.github.galleog.piggymetrics.notification.domain.NotificationType.REMIND;
import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENTS;
import static com.github.galleog.piggymetrics.notification.domain.Tables.RECIPIENT_NOTIFICATIONS;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.github.galleog.r2dbc.jooq.config.R2dbcJooqAutoConfiguration;
import com.github.galleog.r2dbc.jooq.transaction.TransactionAwareJooqWrapper;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.DateValue;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Tests for {@link JooqRecipientRepository}.
 */
@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
@Import(JooqRecipientRepositoryTest.DataSourceConfig.class)
@ImportAutoConfiguration(R2dbcJooqAutoConfiguration.class)
class JooqRecipientRepositoryTest {
    private static final String POSTGRES_IMAGE = "postgres:13.8-alpine";
    private static final String USERNAME_1 = "test1";
    private static final String USERNAME_2 = "test2";
    private static final String USERNAME_3 = "test3";
    private static final String EMAIL_1 = "test1@example.com";
    private static final String EMAIL_2 = "test2@example.com";
    private static final String EMAIL_3 = "test3@example.com";
    private static final LocalDate DAY_AGO = LocalDate.now().minusDays(1);
    private static final LocalDate QUARTER_AGO = LocalDate.now().minusDays(Frequency.QUARTERLY.getKey() + 1);
    private static final LocalDate WEEKLY_AGO = LocalDate.now().minusDays(Frequency.WEEKLY.getKey() + 1);

    private static final DbSetupTracker DB_SETUP_TRACKER = new DbSetupTracker();

    @Container
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private TransactionAwareJooqWrapper wrapper;

    private RecipientRepository repository;
    private DataSourceDestination destination;

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> url("r2dbc"));
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.datasource.url", () -> url("jdbc"));
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
    }

    private static String url(String prefix) {
        return String.format("%s:postgresql://%s:%s/%s", prefix, postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @BeforeEach
    void setUp() {
        repository = new JooqRecipientRepository(wrapper);

        destination = DataSourceDestination.with(dataSource);
    }

    private DateValue toDateValue(LocalDate date) {
        return DateValue.from(GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault())));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class DataSourceConfig {
        @Bean
        @LiquibaseDataSource
        DataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder()
                    .build();
        }
    }

    @Nested
    class FindTest {
        @BeforeEach
        void setUp() {
            var operation = sequenceOf(
                    deleteAllFrom(
                            RECIPIENT_NOTIFICATIONS.getName(),
                            RECIPIENTS.getName()
                    ),
                    insertInto(RECIPIENTS.getName())
                            .row()
                            .column(RECIPIENTS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENTS.EMAIL.getName(), EMAIL_1)
                            .end()
                            .row()
                            .column(RECIPIENTS.USERNAME.getName(), USERNAME_2)
                            .column(RECIPIENTS.EMAIL.getName(), EMAIL_2)
                            .end()
                            .row()
                            .column(RECIPIENTS.USERNAME.getName(), USERNAME_3)
                            .column(RECIPIENTS.EMAIL.getName(), EMAIL_3)
                            .end()
                            .build(),
                    insertInto(RECIPIENT_NOTIFICATIONS.getName())
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), BACKUP.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), true)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.QUARTERLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), QUARTER_AGO)
                            .end()
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_2)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), BACKUP.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), true)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.MONTHLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), WEEKLY_AGO)
                            .end()
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), REMIND.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), false)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.WEEKLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), WEEKLY_AGO)
                            .end()
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_2)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), REMIND.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), true)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.WEEKLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), null)
                            .end()
                            .build()
            );

            var dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqRecipientRepository#getByUsername(String)}.
         */
        @Test
        void shouldGetRecipientByUsername() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByUsername(USERNAME_1)
                    .as(StepVerifier::create)
                    .expectNextMatches(recipient -> {
                        assertThat(recipient.getUsername()).isEqualTo(USERNAME_1);
                        assertThat(recipient.getEmail()).isEqualTo(EMAIL_1);
                        assertThat(recipient.getNotifications().entrySet()).extracting(
                                Map.Entry::getKey,
                                entry -> entry.getValue().isActive(),
                                entry -> entry.getValue().getFrequency(),
                                entry -> entry.getValue().getNotifyDate()
                        ).containsExactlyInAnyOrder(
                                tuple(BACKUP, true, Frequency.QUARTERLY, QUARTER_AGO),
                                tuple(REMIND, false, Frequency.WEEKLY, WEEKLY_AGO)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqRecipientRepository#getByUsername(String)} without notifications.
         */
        @Test
        void shouldGetRecipientWithoutNotificationsByUsername() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByUsername(USERNAME_3)
                    .as(StepVerifier::create)
                    .expectNextMatches(recipient -> {
                        assertThat(recipient.getUsername()).isEqualTo(USERNAME_3);
                        assertThat(recipient.getEmail()).isEqualTo(EMAIL_3);
                        assertThat(recipient.getNotifications()).isEmpty();
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqRecipientRepository#getByUsername(String)} when there is no recipient with the specified name.
         */
        @Test
        void shouldNotGetRecipientByUsername() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.getByUsername("noname")
                    .as(StepVerifier::create)
                    .verifyComplete();
        }

        /**
         * Test for {@link JooqRecipientRepository#readyToNotify(NotificationType, LocalDate)} for {@link NotificationType#BACKUP}.
         */
        @Test
        void shouldFindRecipientsReadyToBackup() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.readyToNotify(BACKUP, LocalDate.now())
                    .as(StepVerifier::create)
                    .expectNextMatches(recipient -> {
                        assertThat(recipient.getUsername()).isEqualTo(USERNAME_1);
                        assertThat(recipient.getEmail()).isEqualTo(EMAIL_1);
                        assertThat(recipient.getNotifications().entrySet()).extracting(
                                Map.Entry::getKey,
                                entry -> entry.getValue().isActive(),
                                entry -> entry.getValue().getFrequency(),
                                entry -> entry.getValue().getNotifyDate()
                        ).containsExactlyInAnyOrder(
                                tuple(BACKUP, true, Frequency.QUARTERLY, QUARTER_AGO),
                                tuple(REMIND, false, Frequency.WEEKLY, WEEKLY_AGO)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqRecipientRepository#readyToNotify(NotificationType, LocalDate)} for {@link NotificationType#REMIND}.
         */
        @Test
        void shouldFindRecipientsReadyToRemind() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.readyToNotify(REMIND, LocalDate.now())
                    .as(StepVerifier::create)
                    .expectNextMatches(recipient -> {
                        assertThat(recipient.getUsername()).isEqualTo(USERNAME_2);
                        assertThat(recipient.getEmail()).isEqualTo(EMAIL_2);
                        assertThat(recipient.getNotifications().entrySet()).extracting(
                                Map.Entry::getKey,
                                entry -> entry.getValue().isActive(),
                                entry -> entry.getValue().getFrequency(),
                                entry -> entry.getValue().getNotifyDate()
                        ).containsExactlyInAnyOrder(
                                tuple(BACKUP, true, Frequency.MONTHLY, WEEKLY_AGO),
                                tuple(REMIND, true, Frequency.WEEKLY, null)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqRecipientRepository#readyToNotify(NotificationType, LocalDate)}
         * for {@link NotificationType#BACKUP} when all recipients have been notified recently.
         */
        @Test
        void shouldNotFindRecipientsReadyToBackup4DaysAgo() {
            DB_SETUP_TRACKER.skipNextLaunch();

            repository.readyToNotify(BACKUP, LocalDate.now().minusDays(4))
                    .as(StepVerifier::create)
                    .verifyComplete();
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            var operation = deleteAllFrom(
                    RECIPIENT_NOTIFICATIONS.getName(),
                    RECIPIENTS.getName()
            );

            var dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqRecipientRepository#save(Recipient)}.
         */
        @Test
        void shouldSaveRecipient() {
            var backup = NotificationSettings.builder()
                    .active(false)
                    .frequency(Frequency.MONTHLY)
                    .notifyDate(WEEKLY_AGO)
                    .build();
            var remind = NotificationSettings.builder()
                    .active(true)
                    .frequency(Frequency.WEEKLY)
                    .notifyDate(DAY_AGO)
                    .build();
            var recipient = Recipient.builder()
                    .username(USERNAME_1)
                    .email(EMAIL_1)
                    .notification(BACKUP, backup)
                    .notification(REMIND, remind)
                    .build();

            repository.save(recipient)
                    .as(StepVerifier::create)
                    .expectNextMatches(r -> {
                        var recipients = new Table(dataSource, RECIPIENTS.getName());
                        Assertions.assertThat(recipients)
                                .column(RECIPIENTS.USERNAME.getName()).containsValues(USERNAME_1)
                                .column(RECIPIENTS.EMAIL.getName()).containsValues(EMAIL_1);

                        var settings = new Table(dataSource, RECIPIENT_NOTIFICATIONS.getName());
                        Assertions.assertThat(settings)
                                .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName())
                                .containsValues(BACKUP.name(), REMIND.name())
                                .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName()).containsValues(false, true)
                                .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName())
                                .containsValues(Frequency.MONTHLY.getKey(), Frequency.WEEKLY.getKey())
                                .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName())
                                .containsValues(toDateValue(WEEKLY_AGO), toDateValue(DAY_AGO));

                        assertThat(r.getUsername()).isEqualTo(USERNAME_1);
                        assertThat(r.getEmail()).isEqualTo(EMAIL_1);
                        assertThat(r.getNotifications().entrySet()).extracting(
                                Map.Entry::getKey,
                                entry -> entry.getValue().isActive(),
                                entry -> entry.getValue().getFrequency(),
                                entry -> entry.getValue().getNotifyDate()
                        ).containsExactlyInAnyOrder(
                                tuple(BACKUP, false, Frequency.MONTHLY, WEEKLY_AGO),
                                tuple(REMIND, true, Frequency.WEEKLY, DAY_AGO)
                        );
                        return true;
                    }).verifyComplete();
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            var operation = sequenceOf(
                    deleteAllFrom(
                            RECIPIENT_NOTIFICATIONS.getName(),
                            RECIPIENTS.getName()
                    ),
                    insertInto(RECIPIENTS.getName())
                            .row()
                            .column(RECIPIENTS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENTS.EMAIL.getName(), EMAIL_1)
                            .end()
                            .build(),
                    insertInto(RECIPIENT_NOTIFICATIONS.getName())
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), BACKUP.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), true)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.QUARTERLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), QUARTER_AGO)
                            .end()
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), REMIND.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), false)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.WEEKLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), WEEKLY_AGO)
                            .end()
                            .build()
            );

            var dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqRecipientRepository#update(Recipient)}.
         */
        @Test
        void shouldUpdateRecipient() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var backup = NotificationSettings.builder()
                    .active(false)
                    .frequency(Frequency.MONTHLY)
                    .notifyDate(WEEKLY_AGO)
                    .build();
            var remind = NotificationSettings.builder()
                    .active(true)
                    .frequency(Frequency.WEEKLY)
                    .notifyDate(DAY_AGO)
                    .build();
            var recipient = Recipient.builder()
                    .username(USERNAME_1)
                    .email(EMAIL_2)
                    .notification(BACKUP, backup)
                    .notification(REMIND, remind)
                    .build();

            repository.update(recipient)
                    .as(StepVerifier::create)
                    .expectNextMatches(r -> {
                        var recipients = new Table(dataSource, RECIPIENTS.getName());
                        Assertions.assertThat(recipients)
                                .column(RECIPIENTS.USERNAME.getName()).containsValues(USERNAME_1)
                                .column(RECIPIENTS.EMAIL.getName()).containsValues(EMAIL_2);

                        var settings = new Table(dataSource, RECIPIENT_NOTIFICATIONS.getName());
                        Assertions.assertThat(settings)
                                .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName())
                                .containsValues(BACKUP.name(), REMIND.name())
                                .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName()).containsValues(false, true)
                                .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName()).containsValues(Frequency.MONTHLY.getKey(), Frequency.WEEKLY.getKey())
                                .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName()).containsValues(toDateValue(WEEKLY_AGO), toDateValue(DAY_AGO));

                        assertThat(r.getUsername()).isEqualTo(USERNAME_1);
                        assertThat(r.getEmail()).isEqualTo(EMAIL_2);
                        assertThat(r.getNotifications().entrySet()).extracting(
                                Map.Entry::getKey,
                                entry -> entry.getValue().isActive(),
                                entry -> entry.getValue().getFrequency(),
                                entry -> entry.getValue().getNotifyDate()
                        ).containsOnly(
                                tuple(BACKUP, false, Frequency.MONTHLY, WEEKLY_AGO),
                                tuple(REMIND, true, Frequency.WEEKLY, DAY_AGO)
                        );
                        return true;
                    }).verifyComplete();
        }

        /**
         * Test for {@link JooqRecipientRepository#update(Recipient)} when no notification settings for the specified user exists.
         */
        @Test
        void shouldNotUpdateRecipient() {
            DB_SETUP_TRACKER.skipNextLaunch();

            var recipient = Recipient.builder()
                    .username(USERNAME_2)
                    .email(EMAIL_1)
                    .build();

            repository.update(recipient)
                    .as(StepVerifier::create)
                    .verifyComplete();
        }
    }
}