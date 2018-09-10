package com.github.galleog.piggymetrics.core.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import org.apache.commons.lang3.SerializationUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Enum}.
 */
class EnumTest {
    /**
     * Test for {@link Enum#compareTo(Enum)}.
     */
    @Test
    void testCompareTo() {
        assertThat(ColorEnum.BLUE.compareTo(ColorEnum.BLUE)).isEqualTo(0);
        assertThat(ColorEnum.RED.compareTo(ColorEnum.BLUE)).isGreaterThan(0);
        assertThat(ColorEnum.BLUE.compareTo(ColorEnum.RED)).isLessThan(0);

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
                ColorEnum.RED.compareTo(null)
        );
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
                ColorEnum.RED.compareTo(GreekLetterEnum.ALPHA)
        );
    }

    /**
     * Test for {@link Enum#equals(Object)}.
     */
    @Test
    void shouldBeEqual() {
        assertThat(ColorEnum.RED).isEqualTo(ColorEnum.RED);
        assertThat(ColorEnum.valueOf("Red")).isEqualTo(ColorEnum.RED);
        assertThat(ColorEnum.RED).isNotEqualTo(ColorEnum.BLUE);
        assertThat(GreekLetterEnum.ALPHA).isEqualTo(ExtendedGreekLetterEnum.ALPHA);
        assertThat(ExtendedGreekLetterEnum.valueOf("Alpha")).isEqualTo(GreekLetterEnum.ALPHA);
        assertThat(ExtendedGreekLetterEnum.GAMMA).isEqualTo(ExtendedGreekLetterEnum.GAMMA);
        assertThat(OperationEnum.PLUS).isEqualTo(OperationEnum.PLUS);
        assertThat(OperationEnum.valueOf("Minus")).isEqualTo(OperationEnum.MINUS);
        assertThat(OperationEnum.PLUS).isNotEqualTo(OperationEnum.MINUS);
        assertThat(ColorEnum.RED).isNotEqualTo(GreekLetterEnum.ALPHA);
    }

    /**
     * Test for {@link Enum#hashCode}.
     */
    @Test
    void shouldHaveSameHashCode() {
        assertThat(ColorEnum.RED.hashCode()).isEqualTo(ColorEnum.RED.hashCode());
        assertThat(ColorEnum.valueOf("Red").hashCode()).isEqualTo(ColorEnum.RED.hashCode());
        assertThat(GreekLetterEnum.ALPHA.hashCode()).isEqualTo(ExtendedGreekLetterEnum.ALPHA.hashCode());
        assertThat(ExtendedGreekLetterEnum.valueOf("Alpha").hashCode())
                .isEqualTo(GreekLetterEnum.ALPHA.hashCode());
    }

    /**
     * Test for {@link Enum#valueOf(Class, Object)}.
     */
    @Test
    void shouldHaveCorrectValueOf() {
        assertThat(ColorEnum.valueOf("Red")).isSameAs(ColorEnum.RED);
        assertThat(ColorEnum.valueOf("Blue")).isSameAs(ColorEnum.BLUE);
        assertThat(ColorEnum.valueOf("Green")).isSameAs(ColorEnum.GREEN);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                ColorEnum.valueOf("Pink")
        );

        assertThat(OperationEnum.valueOf("Plus")).isSameAs(OperationEnum.PLUS);
        assertThat(OperationEnum.valueOf("Minus")).isSameAs(OperationEnum.MINUS);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                OperationEnum.valueOf("Multiply")
        );

        assertThat(GreekLetterEnum.valueOf("Alpha")).isSameAs(GreekLetterEnum.ALPHA);
        assertThat(GreekLetterEnum.valueOf("Beta")).isSameAs(GreekLetterEnum.BETA);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                GreekLetterEnum.valueOf("Gamma")
        );

        assertThat(ExtendedGreekLetterEnum.valueOf("Alpha")).isSameAs(GreekLetterEnum.ALPHA);
        assertThat(ExtendedGreekLetterEnum.valueOf("Beta")).isSameAs(GreekLetterEnum.BETA);
        assertThat(ExtendedGreekLetterEnum.valueOf("Gamma")).isSameAs(ExtendedGreekLetterEnum.GAMMA);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                ExtendedGreekLetterEnum.valueOf("Delta")
        );
    }

    /**
     * Test for {@link Enum#values(Class)}.
     */
    @Test
    void shouldHaveCorrectValues() {
        assertThat(ColorEnum.values()).containsOnly(ColorEnum.RED, ColorEnum.BLUE, ColorEnum.GREEN);
        assertThat(OperationEnum.values()).containsOnly(OperationEnum.PLUS, OperationEnum.MINUS);
        assertThat(GreekLetterEnum.values()).containsOnly(GreekLetterEnum.ALPHA, GreekLetterEnum.BETA);
        Assertions.<GreekLetterEnum>assertThat(Enum.values(ExtendedGreekLetterEnum.class)).containsOnly(
                GreekLetterEnum.ALPHA, GreekLetterEnum.BETA, ExtendedGreekLetterEnum.GAMMA
        );
    }

    /**
     * Test for serialization/deserialization.
     */
    @Test
    void shouldBeSerializable() {
        assertThat(SerializationUtils.clone(ColorEnum.RED)).isSameAs(ColorEnum.RED);
        assertThat(SerializationUtils.clone(ColorEnum.BLUE)).isSameAs(ColorEnum.BLUE);
        assertThat(SerializationUtils.clone(ColorEnum.GREEN)).isSameAs(ColorEnum.GREEN);
        assertThat(SerializationUtils.clone(OperationEnum.PLUS)).isSameAs(OperationEnum.PLUS);
        assertThat(SerializationUtils.clone(OperationEnum.MINUS)).isSameAs(OperationEnum.MINUS);
        assertThat(SerializationUtils.clone(GreekLetterEnum.ALPHA)).isSameAs(GreekLetterEnum.ALPHA);
        assertThat(SerializationUtils.clone(GreekLetterEnum.BETA)).isSameAs(GreekLetterEnum.BETA);
        assertThat(SerializationUtils.clone(ExtendedGreekLetterEnum.ALPHA)).isSameAs(GreekLetterEnum.ALPHA);
        assertThat(SerializationUtils.clone(ExtendedGreekLetterEnum.BETA)).isSameAs(GreekLetterEnum.BETA);
        assertThat(SerializationUtils.clone(ExtendedGreekLetterEnum.GAMMA)).isSameAs(ExtendedGreekLetterEnum.GAMMA);
    }

    /**
     * Test for a duplicated key.
     */
    @Test
    void shouldNotAllowDuplicatedKeys() {
        try {
            DuplicatedKeyEnum.GREEN.getKey();
            fail("ExceptionInInitializerError must be thrown");
        } catch (ExceptionInInitializerError e) {
            assertThat(e.getException()).isInstanceOf(IllegalStateException.class);
        }
    }

    /**
     * Test for {@link Enum#getEnumClass()} that returns {@code null}.
     */
    @Test
    void shouldNotAllowNullClassName() {
        try {
            NullClassEnum.PLUS.getKey();
            fail("ExceptionInInitializerError must be thrown");
        } catch (ExceptionInInitializerError e) {
            assertThat(e.getException()).isInstanceOf(IllegalStateException.class);
        }
    }

    /**
     * Test for invalid {@link Enum#getEnumClass()}.
     */
    @Test
    void shouldNotAllowInvalidClassName() {
        try {
            InvalidClassEnum.PLUS.getKey();
            fail("ExceptionInInitializerError must be thrown");
        } catch (ExceptionInInitializerError e) {
            assertThat(e.getException()).isInstanceOf(IllegalStateException.class);
        }
    }
}
