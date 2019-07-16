package com.github.galleog.piggymetrics.exchange.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.piggymetrics.exchange.grpc.ExchangeServiceProto;
import com.github.galleog.protobuf.java.type.MoneyProto;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

/**
 * Test for {@link ExchangeService}.
 */
class ExchangeServiceTest {
    private static final String USD = "USD";
    private static final String EUR = "EUR";
    private static final BigDecimal TEN = BigDecimal.valueOf(10, 2);

    private ExchangeService exchangeService;

    @BeforeEach
    void setUp() {
        exchangeService = new ExchangeService();
    }

    /**
     * Test for {@link ExchangeService#convert(Flux)} for the same currency.
     */
    @Test
    void shouldConvertToSameCurrency() {
        MoneyProto.Money money = moneyConverter().convert(Money.of(TEN, USD));
        ExchangeServiceProto.ConvertRequest request = ExchangeServiceProto.ConvertRequest.newBuilder()
                .setMoney(money)
                .setCurrencyCode(USD)
                .build();
        StepVerifier.create(exchangeService.convert(Flux.just(request)))
                .expectNext(money)
                .verifyComplete();
    }

    /**
     * Test for {@link ExchangeService#convert(Flux)} for {@link BigDecimal#ZERO}.
     */
    @Test
    void shouldConvertZero() {
        MoneyProto.Money money = moneyConverter().convert(Money.of(BigDecimal.ZERO, EUR));
        ExchangeServiceProto.ConvertRequest request = ExchangeServiceProto.ConvertRequest.newBuilder()
                .setMoney(money)
                .setCurrencyCode(USD)
                .build();
        StepVerifier.create(exchangeService.convert(Flux.just(request)))
                .expectNextMatches(m -> {
                    assertThat(m.getAmount()).isEqualTo(bigDecimalConverter().convert(BigDecimal.ZERO));
                    assertThat(m.getCurrencyCode()).isEqualTo(USD);
                    return true;
                }).verifyComplete();
    }

    /**
     * Test for {@link ExchangeService#convert(Flux)}.
     */
    @Test
    void shouldConvertTen() {
        MoneyProto.Money money = moneyConverter().convert(Money.of(TEN, EUR));
        ExchangeServiceProto.ConvertRequest request = ExchangeServiceProto.ConvertRequest.newBuilder()
                .setMoney(money)
                .setCurrencyCode(USD)
                .build();
        StepVerifier.create(exchangeService.convert(Flux.just(request)))
                .expectNextMatches(m -> {
                    assertThat(m.getAmount()).isNotNull();
                    assertThat(m.getCurrencyCode()).isEqualTo(USD);
                    return true;
                }).verifyComplete();
    }
}