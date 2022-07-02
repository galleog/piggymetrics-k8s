package com.github.galleog.piggymetrics.core.enums.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EnumDeserializer}.
 */
class EnumDeserializerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test for deserialization of non-null values.
     */
    @Test
    void shouldDeserialize() throws Exception {
        TestBean bean = objectMapper.readValue("{\"operation\" : \"Minus\", \"integer\" : 1}",
                TestBean.class);
        assertThat(bean.getOperation()).isSameAs(OperationEnum.MINUS);
        assertThat(bean.getInteger()).isSameAs(IntegerEnum.ONE);
    }

    /**
     * Test for deserialization of null values.
     */
    @Test
    void shouldDeserializeNulls() throws Exception {
        TestBean bean = objectMapper.readValue("{\"operation\" : null}", TestBean.class);
        assertThat(bean.getOperation()).isNull();
        assertThat(bean.getInteger()).isNull();
    }

    /**
     * Test for a deserialization exception when the key name is invalid.
     */
    @Test
    void testDeserializeAbsentKey() {
        assertThatExceptionOfType(JsonMappingException.class).isThrownBy(() ->
                objectMapper.readValue("{\"integer\" : {\"one\"}}", TestBean.class)
        );
    }

    /**
     * Test for a deserialization exception when a key value is invalid.
     */
    @Test
    void testDeserializeInvalidKey() {
        assertThatExceptionOfType(JsonMappingException.class).isThrownBy(() ->
                objectMapper.readValue("{\"integer\" : 3}", TestBean.class)
        );
    }
}