package com.github.galleog.piggymetrics.notification.repository;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.piggymetrics.notification.domain.Frequency;
import com.github.galleog.piggymetrics.notification.domain.NotificationSettings;
import com.github.galleog.piggymetrics.notification.domain.NotificationType;
import com.github.galleog.piggymetrics.notification.domain.Recipient;
import com.google.common.collect.ImmutableMap;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.DateValue;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link RecipientRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RecipientRepositoryTest {
    private static final String ACCOUNT_1_NAME = "test1";
    private static final String ACCOUNT_2_NAME = "test2";
    private static final String EMAIL_1 = "test1@example.com";
    private static final String EMAIL_2 = "test2@example.com";
    private static final LocalDate DAY_AGO = LocalDate.now().minusDays(1);
    private static final LocalDate QUARTER_AGO = LocalDate.now().minusDays(Frequency.QUARTERLY.getKey() + 1);
    private static final LocalDate WEEKLY_AGO = LocalDate.now().minusDays(Frequency.WEEKLY.getKey() + 1);

    @Container
    private static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer();
    private static final DbSetupTracker DB_SETUP_TRACKER = new DbSetupTracker();

    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RecipientRepository repository;
    private TransactionTemplate transactionTemplate;
    private DataSourceDestination destination;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

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
                            Recipient.NOTIFICATIONS_TABLE_NAME,
                            Recipient.TABLE_NAME
                    ),
                    insertInto(Recipient.TABLE_NAME)
                            .row()
                            .column("id", 1)
                            .column("account_name", ACCOUNT_1_NAME)
                            .column("email", EMAIL_1)
                            .column("version", 1)
                            .end()
                            .row()
                            .column("id", 2)
                            .column("account_name", ACCOUNT_2_NAME)
                            .column("email", EMAIL_2)
                            .column("version", 1)
                            .end()
                            .build(),
                    insertInto(Recipient.NOTIFICATIONS_TABLE_NAME)
                            .row()
                            .column("notification_type", NotificationType.BACKUP.name())
                            .column("recipient_id", 1)
                            .column("active", true)
                            .column("frequency", Frequency.QUARTERLY.getKey())
                            .column("last_notified_date", QUARTER_AGO)
                            .end()
                            .row()
                            .column("notification_type", NotificationType.REMIND.name())
                            .column("recipient_id", 1)
                            .column("active", false)
                            .column("frequency", Frequency.WEEKLY.getKey())
                            .column("last_notified_date", WEEKLY_AGO)
                            .end()
                            .row()
                            .column("notification_type", NotificationType.BACKUP.name())
                            .column("recipient_id", 2)
                            .column("active", true)
                            .column("frequency", Frequency.MONTHLY.getKey())
                            .column("last_notified_date", WEEKLY_AGO)
                            .end()
                            .row()
                            .column("notification_type", NotificationType.REMIND.name())
                            .column("recipient_id", 2)
                            .column("active", true)
                            .column("frequency", Frequency.WEEKLY.getKey())
                            .column("last_notified_date", null)
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            DB_SETUP_TRACKER.launchIfNecessary(dbSetup);
        }

        /**
         * Test for {@link RecipientRepository#findByAccountName(String)}.
         */
        @Test
        void shouldFindByAccountName() {
            DB_SETUP_TRACKER.skipNextLaunch();

            Optional<Recipient> recipient = repository.findByAccountName(ACCOUNT_1_NAME);
            assertThat(recipient).isPresent();
            assertThat(recipient.get().getAccountName()).isEqualTo(ACCOUNT_1_NAME);
            assertThat(recipient.get().getEmail()).isEqualTo(EMAIL_1);
            assertThat(recipient.get().getScheduledNotifications().get(NotificationType.BACKUP)).extracting(
                    NotificationSettings::isActive,
                    NotificationSettings::getFrequency,
                    NotificationSettings::getLastNotifiedDate
            ).containsExactly(true, Frequency.QUARTERLY, QUARTER_AGO);
            assertThat(recipient.get().getScheduledNotifications().get(NotificationType.REMIND)).extracting(
                    NotificationSettings::isActive,
                    NotificationSettings::getFrequency,
                    NotificationSettings::getLastNotifiedDate
            ).containsExactly(false, Frequency.WEEKLY, WEEKLY_AGO);
        }

        /**
         * Test for {@link RecipientRepository#readyToNotify(NotificationType, LocalDate)} for {@link NotificationType#BACKUP}.
         */
        @Test
        void shouldFindReadyToBackup() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<Recipient> recipients = repository.readyToNotify(NotificationType.BACKUP, LocalDate.now());
            assertThat(recipients).extracting(Recipient::getAccountName)
                    .containsExactly(ACCOUNT_1_NAME);
        }

        /**
         * Test for {@link RecipientRepository#readyToNotify(NotificationType, LocalDate)} for {@link NotificationType#REMIND}.
         */
        @Test
        void shouldFindReadyToRemind() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<Recipient> recipients = repository.readyToNotify(NotificationType.REMIND, LocalDate.now());
            assertThat(recipients).extracting(Recipient::getAccountName)
                    .containsExactly(ACCOUNT_2_NAME);
        }

        /**
         * Test for {@link RecipientRepository#readyToNotify(NotificationType, LocalDate)}
         * for {@link NotificationType#BACKUP} when all recipients have been notified recently.
         */
        @Test
        void shouldNotFindReadyToBackup4DaysAgo() {
            DB_SETUP_TRACKER.skipNextLaunch();

            List<Recipient> recipients = repository.readyToNotify(NotificationType.BACKUP, LocalDate.now().minusDays(4));
            assertThat(recipients).isEmpty();
        }
    }

    @Nested
    class SaveTest {
        @BeforeEach
        void setUp() {
            Operation operation = deleteAllFrom(
                    Recipient.NOTIFICATIONS_TABLE_NAME,
                    Recipient.TABLE_NAME
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for {@link RecipientRepository#save(Object)}.
         */
        @Test
        void shouldSaveRecipient() {
            NotificationSettings backup = NotificationSettings.builder()
                    .active(false)
                    .frequency(Frequency.MONTHLY)
                    .lastNotifiedDate(WEEKLY_AGO)
                    .build();
            NotificationSettings remind = NotificationSettings.builder()
                    .active(true)
                    .frequency(Frequency.WEEKLY)
                    .lastNotifiedDate(DAY_AGO)
                    .build();
            Recipient recipient = Recipient.builder()
                    .accountName(ACCOUNT_1_NAME)
                    .email(EMAIL_1)
                    .scheduledNotification(NotificationType.BACKUP, backup)
                    .scheduledNotification(NotificationType.REMIND, remind)
                    .build();
            transactionTemplate.execute(status -> repository.save(recipient));

            Table recipients = new Table(dataSource, Recipient.TABLE_NAME);
            Assertions.assertThat(recipients)
                    .column("account_name").containsValues(ACCOUNT_1_NAME)
                    .column("email").containsValues(EMAIL_1);

            Table settings = new Table(dataSource, Recipient.NOTIFICATIONS_TABLE_NAME);
            Assertions.assertThat(settings)
                    .column("notification_type")
                    .containsValues(NotificationType.BACKUP.name(), NotificationType.REMIND.name())
                    .column("active").containsValues(false, true)
                    .column("frequency").containsValues(Frequency.MONTHLY.getKey(), Frequency.WEEKLY.getKey())
                    .column("last_notified_date").containsValues(toDateValue(WEEKLY_AGO), toDateValue(DAY_AGO));
        }
    }

    @Nested
    class UpdateTest {
        @BeforeEach
        void setUp() {
            Operation operation = sequenceOf(
                    deleteAllFrom(
                            Recipient.NOTIFICATIONS_TABLE_NAME,
                            Recipient.TABLE_NAME
                    ),
                    insertInto(Recipient.TABLE_NAME)
                            .row()
                            .column("id", 1)
                            .column("account_name", ACCOUNT_1_NAME)
                            .column("email", EMAIL_1)
                            .column("version", 1)
                            .end()
                            .build(),
                    insertInto(Recipient.NOTIFICATIONS_TABLE_NAME)
                            .row()
                            .column("notification_type", NotificationType.BACKUP.name())
                            .column("recipient_id", 1)
                            .column("active", true)
                            .column("frequency", Frequency.QUARTERLY.getKey())
                            .column("last_notified_date", QUARTER_AGO)
                            .end()
                            .row()
                            .column("notification_type", NotificationType.REMIND.name())
                            .column("recipient_id", 1)
                            .column("active", false)
                            .column("frequency", Frequency.WEEKLY.getKey())
                            .column("last_notified_date", WEEKLY_AGO)
                            .end()
                            .build()
            );
            DbSetup dbSetup = new DbSetup(destination, operation);
            dbSetup.launch();
        }

        /**
         * Test for modification of an existing {@link Recipient}.
         */
        @Test
        void shouldUpdateRecipient() {
            NotificationSettings backup = NotificationSettings.builder()
                    .active(false)
                    .frequency(Frequency.MONTHLY)
                    .lastNotifiedDate(WEEKLY_AGO)
                    .build();
            NotificationSettings remind = NotificationSettings.builder()
                    .active(true)
                    .frequency(Frequency.WEEKLY)
                    .lastNotifiedDate(DAY_AGO)
                    .build();

            transactionTemplate.execute(status -> {
                Optional<Recipient> optional = repository.findByAccountName(ACCOUNT_1_NAME);
                assertThat(optional).isPresent();
                Recipient recipient = optional.get();
                recipient.updateSettings(EMAIL_2, ImmutableMap.of(
                        NotificationType.BACKUP, backup,
                        NotificationType.REMIND, remind
                ));
                repository.save(recipient);
                return null;
            });

            Table recipients = new Table(dataSource, Recipient.TABLE_NAME);
            Assertions.assertThat(recipients)
                    .column("account_name").containsValues(ACCOUNT_1_NAME)
                    .column("email").containsValues(EMAIL_2);

            Table settings = new Table(dataSource, Recipient.NOTIFICATIONS_TABLE_NAME);
            Assertions.assertThat(settings)
                    .column("notification_type")
                    .containsValues(NotificationType.BACKUP.name(), NotificationType.REMIND.name())
                    .column("active").containsValues(false, true)
                    .column("frequency").containsValues(Frequency.MONTHLY.getKey(), Frequency.WEEKLY.getKey())
                    .column("last_notified_date").containsValues(toDateValue(WEEKLY_AGO), toDateValue(DAY_AGO));
        }
    }
}