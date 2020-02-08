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

import com.github.galleog.piggymetrics.notification.config.JooqConfig;
import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.github.galleog.piggymetrics.notification.repository.RecipientRepository;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.DateValue;
import org.assertj.db.type.Table;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for {@link JooqRecipientRepository}.
 */
@JooqTest
@ActiveProfiles("test")
@Import(JooqConfig.class)
class JooqRecipientRepositoryTest {
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

    @Autowired
    private DataSource dataSource;
    @Autowired
    private DSLContext dsl;

    private RecipientRepository repository;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
        repository = new JooqRecipientRepository(dsl);

        destination = DataSourceDestination.with(dataSource);
    }

    private DateValue toDateValue(LocalDate date) {
        return DateValue.from(GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault())));
    }

    @Nested
    class FindTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
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
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_1)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), REMIND.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), false)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.WEEKLY.getKey())
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName(), WEEKLY_AGO)
                            .end()
                            .row()
                            .column(RECIPIENT_NOTIFICATIONS.USERNAME.getName(), USERNAME_2)
                            .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName(), BACKUP.name())
                            .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName(), true)
                            .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName(), Frequency.MONTHLY.getKey())
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

            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqRecipientRepository#getByUsername(String)}.
         */
        @Test
        void shouldGetRecipientByUsername() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<Recipient> recipient = repository.getByUsername(USERNAME_1);
            assertThat(recipient).isPresent();
            assertThat(recipient.get().getUsername()).isEqualTo(USERNAME_1);
            assertThat(recipient.get().getEmail()).isEqualTo(EMAIL_1);
            assertThat(recipient.get().getNotifications().entrySet()).extracting(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isActive(),
                    entry -> entry.getValue().getFrequency(),
                    entry -> entry.getValue().getNotifyDate()
            ).containsOnly(
                    tuple(BACKUP, true, Frequency.QUARTERLY, QUARTER_AGO),
                    tuple(REMIND, false, Frequency.WEEKLY, WEEKLY_AGO)
            );
        }

        /**
         * Test for {@link JooqRecipientRepository#getByUsername(String)} without notifications.
         */
        @Test
        void shouldGetRecipientWithoutNotificationsByUsername() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<Recipient> recipient = repository.getByUsername(USERNAME_3);
            assertThat(recipient).isPresent();
            assertThat(recipient.get().getUsername()).isEqualTo(USERNAME_3);
            assertThat(recipient.get().getEmail()).isEqualTo(EMAIL_3);
            assertThat(recipient.get().getNotifications()).isEmpty();
        }

        /**
         * Test for {@link JooqRecipientRepository#readyToNotify(NotificationType, LocalDate)} for {@link NotificationType#BACKUP}.
         */
        @Test
        void shouldFindRecipientsReadyToBackup() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<Recipient> recipients = repository.readyToNotify(BACKUP, LocalDate.now());
            assertThat(recipients).extracting(Recipient::getUsername)
                    .containsExactly(USERNAME_1);
        }

        /**
         * Test for {@link JooqRecipientRepository#readyToNotify(NotificationType, LocalDate)} for {@link NotificationType#REMIND}.
         */
        @Test
        void shouldFindRecipientsReadyToRemind() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<Recipient> recipients = repository.readyToNotify(REMIND, LocalDate.now());
            assertThat(recipients).extracting(Recipient::getUsername)
                    .containsExactly(USERNAME_2);
        }

        /**
         * Test for {@link JooqRecipientRepository#readyToNotify(NotificationType, LocalDate)}
         * for {@link NotificationType#BACKUP} when all recipients have been notified recently.
         */
        @Test
        void shouldNotFindRecipientsReadyToBackup4DaysAgo() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<Recipient> recipients = repository.readyToNotify(BACKUP, LocalDate.now().minusDays(4));
            assertThat(recipients).isEmpty();
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            Operation operation = deleteAllFrom(
                    RECIPIENT_NOTIFICATIONS.getName(),
                    RECIPIENTS.getName()
            );

            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link JooqRecipientRepository#save(Recipient)}.
         */
        @Test
        void shouldSaveRecipient() {
            NotificationSettings backup = NotificationSettings.builder()
                    .active(false)
                    .frequency(Frequency.MONTHLY)
                    .notifyDate(WEEKLY_AGO)
                    .build();
            NotificationSettings remind = NotificationSettings.builder()
                    .active(true)
                    .frequency(Frequency.WEEKLY)
                    .notifyDate(DAY_AGO)
                    .build();
            Recipient recipient = Recipient.builder()
                    .username(USERNAME_1)
                    .email(EMAIL_1)
                    .notification(BACKUP, backup)
                    .notification(REMIND, remind)
                    .build();
            repository.save(recipient);

            Table recipients = new Table(dataSource, RECIPIENTS.getName());
            Assertions.assertThat(recipients)
                    .column(RECIPIENTS.USERNAME.getName()).containsValues(USERNAME_1)
                    .column(RECIPIENTS.EMAIL.getName()).containsValues(EMAIL_1);

            Table settings = new Table(dataSource, RECIPIENT_NOTIFICATIONS.getName());
            Assertions.assertThat(settings)
                    .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName())
                    .containsValues(BACKUP.name(), REMIND.name())
                    .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName()).containsValues(false, true)
                    .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName()).containsValues(Frequency.MONTHLY.getKey(), Frequency.WEEKLY.getKey())
                    .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName()).containsValues(toDateValue(WEEKLY_AGO), toDateValue(DAY_AGO));
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
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

            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link JooqRecipientRepository#update(Recipient)}.
         */
        @Test
        void shouldUpdateRecipient() {
            DB_SETUP_TRACKER.skipNextLaunch();

            NotificationSettings backup = NotificationSettings.builder()
                    .active(false)
                    .frequency(Frequency.MONTHLY)
                    .notifyDate(WEEKLY_AGO)
                    .build();
            NotificationSettings remind = NotificationSettings.builder()
                    .active(true)
                    .frequency(Frequency.WEEKLY)
                    .notifyDate(DAY_AGO)
                    .build();
            Recipient recipient = Recipient.builder()
                    .username(USERNAME_1)
                    .email(EMAIL_2)
                    .notification(BACKUP, backup)
                    .notification(REMIND, remind)
                    .build();

            Recipient updated = repository.update(recipient).get();

            Table recipients = new Table(dataSource, RECIPIENTS.getName());
            Assertions.assertThat(recipients)
                    .column(RECIPIENTS.USERNAME.getName()).containsValues(USERNAME_1)
                    .column(RECIPIENTS.EMAIL.getName()).containsValues(EMAIL_2);

            Table settings = new Table(dataSource, RECIPIENT_NOTIFICATIONS.getName());
            Assertions.assertThat(settings)
                    .column(RECIPIENT_NOTIFICATIONS.NOTIFICATION_TYPE.getName())
                    .containsValues(BACKUP.name(), REMIND.name())
                    .column(RECIPIENT_NOTIFICATIONS.ACTIVE.getName()).containsValues(false, true)
                    .column(RECIPIENT_NOTIFICATIONS.FREQUENCY.getName()).containsValues(Frequency.MONTHLY.getKey(), Frequency.WEEKLY.getKey())
                    .column(RECIPIENT_NOTIFICATIONS.NOTIFY_DATE.getName()).containsValues(toDateValue(WEEKLY_AGO), toDateValue(DAY_AGO));

            assertThat(updated.getUsername()).isEqualTo(USERNAME_1);
            assertThat(updated.getEmail()).isEqualTo(EMAIL_2);
            assertThat(recipient.getNotifications().entrySet()).extracting(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isActive(),
                    entry -> entry.getValue().getFrequency(),
                    entry -> entry.getValue().getNotifyDate()
            ).containsOnly(
                    tuple(BACKUP, false, Frequency.MONTHLY, WEEKLY_AGO),
                    tuple(REMIND, true, Frequency.WEEKLY, DAY_AGO)
            );
        }

        /**
         * Test for {@link JooqRecipientRepository#update(Recipient)} when no notification settings for the specified user exists.
         */
        @Test
        void shouldNotUpdateRecipient() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Recipient recipient = Recipient.builder()
                    .username(USERNAME_2)
                    .email(EMAIL_1)
                    .build();
            assertThat(repository.update(recipient)).isNotPresent();
        }
    }
}