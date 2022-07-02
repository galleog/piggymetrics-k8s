package com.github.galleog.piggymetrics.account.event;

import static com.github.galleog.piggymetrics.account.event.UserRegisteredEventConsumer.BASE_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.AccountApplication;
import com.github.galleog.piggymetrics.account.config.ReactorTestConfig;
import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto.UserRegisteredEvent;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Tests for {@link UserRegisteredEventConsumer}.
 */
@ActiveProfiles("test")
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ImportAutoConfiguration(exclude = {
        JooqAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        GrpcServerAutoConfiguration.class,
        GrpcServerFactoryAutoConfiguration.class,
})
@SpringBootTest(classes = {
        AccountApplication.class,
        ReactorTestConfig.class,
        TestChannelBinderConfiguration.class
})
class UserRegisteredEventConsumerTest {
    private static final String DESTINATION_NAME = "user-events";
    private static final String USERNAME = "test";
    private static final UserRegisteredEvent EVENT = UserRegisteredEvent.newBuilder()
            .setUserId(UUID.randomUUID().toString())
            .setUserName(USERNAME)
            .setEmail("test@example.com")
            .build();

    @Autowired
    private InputDestination input;

    @MockBean
    private AccountRepository accountRepository;
    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    /**
     * Test for {@link UserRegisteredEventConsumer#accept(UserRegisteredEvent)}.
     */
    @Test
    void shouldCreateAccount() {
        when(accountRepository.getByName(USERNAME)).thenReturn(Optional.empty());
        when(accountRepository.save(accountCaptor.capture()))
                .thenAnswer((Answer<Account>) invocation -> invocation.getArgument(0));

        input.send(MessageBuilder.withPayload(EVENT).build(), DESTINATION_NAME);

        assertThat(accountCaptor.getValue().getName()).isEqualTo(USERNAME);
        assertThat(accountCaptor.getValue().getSaving()).extracting(
                Saving::getMoneyAmount, Saving::getInterest, Saving::isDeposit, Saving::isCapitalization
        ).containsExactly(Money.of(BigDecimal.ZERO, BASE_CURRENCY), BigDecimal.ZERO, false, false);
        assertThat(accountCaptor.getValue().getItems()).isEmpty();
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#accept(UserRegisteredEvent)}
     * when an account with the same name already exists.
     */
    @Test
    void shouldNotCreateAccountWhenAlreadyExists() {
        Saving saving = Saving.builder()
                .moneyAmount(Money.of(BigDecimal.TEN, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .build();
        Account account = Account.builder()
                .name(USERNAME)
                .saving(saving)
                .build();
        when(accountRepository.getByName(USERNAME)).thenReturn(Optional.of(account));

        input.send(MessageBuilder.withPayload(EVENT).build(), DESTINATION_NAME);

        verify(accountRepository, never()).save(any(Account.class));
    }
}