package com.github.galleog.piggymetrics.account.config;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.piggymetrics.account.AccountApplication;
import com.github.galleog.piggymetrics.account.grpc.AccountServiceProto;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;

/**
 * Tests for {@link FunctionConfig}.
 */
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {
        KafkaAutoConfiguration.class,
        GrpcServerAutoConfiguration.class,
        GsonAutoConfiguration.class,
        IntegrationAutoConfiguration.class
})
@SpringBootTest(classes = {
        AccountApplication.class,
        TestChannelBinderConfiguration.class
})
class FunctionConfigTest {
    private static final String USD = "USD";
    private static final String NAME = "test";
    private static final String NOTE = "note";

    @Autowired
    private Sinks.Many<AccountServiceProto.Account> sink;
    @Autowired
    private OutputDestination output;

    /**
     * Emitting an account should send an AccountUpdatedEvent.
     */
    @Test
    void shouldSendAccountUpdatedEvent() {
        var savingAmount = Money.of(1500, USD);
        var interest = BigDecimal.valueOf(3.32);
        var saving = AccountServiceProto.Saving.newBuilder()
                .setMoney(moneyConverter().convert(savingAmount))
                .setInterest(bigDecimalConverter().convert(interest))
                .setDeposit(true)
                .build();

        var rentAmount = Money.of(1200, USD);
        var rent = AccountServiceProto.Item.newBuilder()
                .setType(AccountServiceProto.ItemType.EXPENSE)
                .setTitle("Rent")
                .setMoney(moneyConverter().convert(rentAmount))
                .setPeriod(AccountServiceProto.TimePeriod.MONTH)
                .setIcon("home")
                .build();

        var mealAmount = Money.of(20, USD);
        var meal = AccountServiceProto.Item.newBuilder()
                .setType(AccountServiceProto.ItemType.EXPENSE)
                .setTitle("Meal")
                .setMoney(moneyConverter().convert(mealAmount))
                .setPeriod(AccountServiceProto.TimePeriod.DAY)
                .setIcon("meal")
                .build();

        var account = AccountServiceProto.Account.newBuilder()
                .setName(NAME)
                .addItems(rent)
                .addItems(meal)
                .setSaving(saving)
                .setNote(NOTE)
                .build();

        var result = sink.tryEmitNext(account);

        assertThat(result.isSuccess()).isTrue();
        assertThat(output.receive().getPayload()).isEqualTo(
                AccountServiceProto.AccountUpdatedEvent.newBuilder()
                        .setAccountName(NAME)
                        .addItems(rent)
                        .addItems(meal)
                        .setSaving(saving)
                        .setNote(NOTE)
                        .build()
                        .toByteArray()
        );
    }
}