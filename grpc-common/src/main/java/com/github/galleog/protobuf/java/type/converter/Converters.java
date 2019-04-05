package com.github.galleog.protobuf.java.type.converter;

import com.github.galleog.protobuf.java.type.BigDecimalProto;
import com.github.galleog.protobuf.java.type.BigIntegerProto;
import com.github.galleog.protobuf.java.type.MoneyProto;
import com.google.common.base.Converter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converters for <a href="https://developers.google.com/protocol-buffers/">Protobuf</a> messages.
 *
 * @author Oleg_Galkin
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Converters {
    private static final Converter<LocalDateTime, Timestamp> TIMESTAMP_CONVERTER = new TimestampConverter();
    private static final Converter<BigInteger, BigIntegerProto.BigInteger> BIG_INTEGER_CONVERTER = new BigIntegerConverter();
    private static final Converter<BigDecimal, BigDecimalProto.BigDecimal> BIG_DECIMAL_CONVERTER = new BigDecimalConverter();
    private static final Converter<Money, MoneyProto.Money> MONEY_CONVERTER = new MoneyConverter();

    /**
     * Returns {@link Converter} to convert {@link LocalDateTime} to {@link Timestamp}.
     */
    public static Converter<LocalDateTime, Timestamp> timestampConverter() {
        return TIMESTAMP_CONVERTER;
    }

    /**
     * Returns {@link Converter} to convert {@link BigInteger} to {@link BigIntegerProto.BigInteger}.
     */
    public static Converter<BigInteger, BigIntegerProto.BigInteger> bigIntegerConverter() {
        return BIG_INTEGER_CONVERTER;
    }

    /**
     * Returns {@link Converter} to convert {@link BigDecimal} to {@link BigDecimalProto.BigDecimal}.
     */
    public static Converter<BigDecimal, BigDecimalProto.BigDecimal> bigDecimalConverter() {
        return BIG_DECIMAL_CONVERTER;
    }

    /**
     * Returns {@link Converter} to convert {@link Money} to {@link MoneyProto.Money}.
     */
    public static Converter<Money, MoneyProto.Money> moneyConverter() {
        return MONEY_CONVERTER;
    }

    private static final class TimestampConverter extends Converter<LocalDateTime, Timestamp> {
        @Override
        protected Timestamp doForward(LocalDateTime dateTime) {
            Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
            return Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
        }

        @Override
        protected LocalDateTime doBackward(Timestamp timestamp) {
            Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
    }

    private static final class BigIntegerConverter extends Converter<BigInteger, BigIntegerProto.BigInteger> {
        @Override
        protected BigIntegerProto.BigInteger doForward(BigInteger value) {
            ByteString bytes = ByteString.copyFrom(value.toByteArray());
            return BigIntegerProto.BigInteger.newBuilder()
                    .setValue(bytes)
                    .build();
        }

        @Override
        protected BigInteger doBackward(BigIntegerProto.BigInteger value) {
            return new BigInteger(value.getValue().toByteArray());
        }
    }

    private static final class BigDecimalConverter extends Converter<BigDecimal, BigDecimalProto.BigDecimal> {
        @Override
        protected BigDecimalProto.BigDecimal doForward(BigDecimal value) {
            return BigDecimalProto.BigDecimal.newBuilder()
                    .setScale(value.scale())
                    .setIntVal(bigIntegerConverter().convert(value.unscaledValue()))
                    .build();
        }

        @Override
        protected BigDecimal doBackward(BigDecimalProto.BigDecimal value) {
            BigInteger intVal = bigIntegerConverter().reverse().convert(value.getIntVal());
            return new BigDecimal(intVal, value.getScale());
        }
    }

    private static final class MoneyConverter extends Converter<Money, MoneyProto.Money> {
        @Override
        protected MoneyProto.Money doForward(Money money) {
            return MoneyProto.Money.newBuilder()
                    .setCurrencyCode(money.getCurrency().getCurrencyCode())
                    .setAmount(bigDecimalConverter().convert(money.getNumber().numberValue(BigDecimal.class)))
                    .build();
        }

        @Override
        protected Money doBackward(MoneyProto.Money money) {
            BigDecimal amount = bigDecimalConverter().reverse().convert(money.getAmount());
            return Money.of(amount, money.getCurrencyCode());
        }
    }
}
