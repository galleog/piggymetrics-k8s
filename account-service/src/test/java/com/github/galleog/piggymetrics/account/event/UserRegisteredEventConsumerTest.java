package com.github.galleog.piggymetrics.account.event;

import static com.github.galleog.piggymetrics.account.event.UserRegisteredEventConsumer.BASE_CURRENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.galleog.piggymetrics.account.domain.Account;
import com.github.galleog.piggymetrics.account.domain.Saving;
import com.github.galleog.piggymetrics.account.repository.AccountRepository;
import com.github.galleog.piggymetrics.auth.grpc.UserRegisteredEventProto;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tests for {@link UserRegisteredEventConsumer}.
 *
 * @author Oleg_Galkin
 */
@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {
    private static final String USERNAME = "test";
    private static final Flux<UserRegisteredEventProto.UserRegisteredEvent> EVENTS = Flux.just(
            UserRegisteredEventProto.UserRegisteredEvent.newBuilder()
                    .setUserId(UUID.randomUUID().toString())
                    .setUserName(USERNAME)
                    .setEmail("test@example.com")
                    .build()
    );

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionalOperator operator;
    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @InjectMocks
    private UserRegisteredEventConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(operator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateAccount() {
        when(accountRepository.getByName(USERNAME)).thenReturn(Mono.empty());
        when(accountRepository.save(accountCaptor.capture()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        consumer.apply(EVENTS)
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(accountCaptor.getValue().getName()).isEqualTo(USERNAME);
        assertThat(accountCaptor.getValue().getSaving()).extracting(
                Saving::getMoneyAmount, Saving::getInterest, Saving::isDeposit, Saving::isCapitalization
        ).containsExactly(Money.of(BigDecimal.ZERO, BASE_CURRENCY), BigDecimal.ZERO, false, false);
        assertThat(accountCaptor.getValue().getItems()).isEmpty();

        verify(operator).transactional(any(Mono.class));
    }

    /**
     * Test for {@link UserRegisteredEventConsumer#apply(Flux)} when an account with the same name already exists.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldNotCreateAccountWhenAlreadyExists() {
        var saving = Saving.builder()
                .moneyAmount(Money.of(BigDecimal.TEN, BASE_CURRENCY))
                .interest(BigDecimal.ZERO)
                .build();
        var account = Account.builder()
                .name(USERNAME)
                .saving(saving)
                .build();
        when(accountRepository.getByName(USERNAME)).thenReturn(Mono.just(account));

        consumer.apply(EVENTS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(accountRepository, never()).save(any(Account.class));
        verify(operator).transactional(any(Mono.class));
    }
}