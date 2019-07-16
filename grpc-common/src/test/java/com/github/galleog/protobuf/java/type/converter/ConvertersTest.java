package com.github.galleog.protobuf.java.type.converter;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigIntegerConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.timestampConverter;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.galleog.protobuf.java.type.BigDecimalProto;
import com.github.galleog.protobuf.java.type.BigIntegerProto;
import com.github.galleog.protobuf.java.type.MoneyProto;
import com.google.protobuf.Timestamp;
import com.google.type.Date;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tests for {@link Converters}.
 */
class ConvertersTest {
    /**
     * Test for {@link Converters#timestampConverter()}.
     */
    @Test
    void shouldConvertTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(5);
        Timestamp timestamp = timestampConverter().convert(localDateTime);
        assertThat(timestampConverter().reverse().convert(timestamp)).isEqualTo(localDateTime);
    }

    /**
     * Test for {@link Converters#dateConverter()}.
     */
    @Test
    void shouldConvertDate() {
        LocalDate localDate = LocalDate.now().minusDays(7);
        Date date = dateConverter().convert(localDate);
        assertThat(dateConverter().reverse().convert(date)).isEqualTo(localDate);
    }

    /**
     * Test for {@link Converters#bigIntegerConverter()}.
     */
    @Test
    void shouldConvertBigInteger() {
        BigInteger bigInteger = BigInteger.valueOf(23L);
        BigIntegerProto.BigInteger converted = bigIntegerConverter().convert(bigInteger);
        assertThat(bigIntegerConverter().reverse().convert(converted)).isEqualTo(bigInteger);
    }

    /**
     * Test for {@link Converters#bigDecimalConverter()}.
     */
    @Test
    void shouldConvertBigDecimal() {
        BigDecimal bigDecimal = BigDecimal.valueOf(2340560000L, 2);
        BigDecimalProto.BigDecimal converted = bigDecimalConverter().convert(bigDecimal);
        assertThat(bigDecimalConverter().reverse().convert(converted)).isEqualTo(bigDecimal);
    }

    /**
     * Test for {@link Converters#moneyConverter()}.
     */
    @Test
    void shouldConvertMoney() {
        Money money = Money.of(234, "USD");
        MoneyProto.Money converted = moneyConverter().convert(money);
        assertThat(moneyConverter().reverse().convert(converted)).isEqualTo(money);
    }
}