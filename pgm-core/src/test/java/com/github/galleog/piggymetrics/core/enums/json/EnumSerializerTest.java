package com.github.galleog.piggymetrics.core.enums.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.JacksonTester;

/**
 * Tests for enum serialization to JSON.
 */
class EnumSerializerTest {
    private JacksonTester<TestBean> json;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

    /**
     * Test for serialization of non-null values.
     */
    @Test
    void shouldSerialize() throws Exception {
        TestBean bean = new TestBean(OperationEnum.PLUS, IntegerEnum.TWO);
        assertThat(json.write(bean)).extractingJsonPathStringValue("$.operation").isEqualTo("Plus");
        assertThat(json.write(bean)).extractingJsonPathNumberValue("$.integer").isEqualTo(2);
    }

    /**
     * Test for serialization of null values.
     */
    @Test
    void shouldSerializeNulls() throws Exception {
        TestBean bean = new TestBean();
        assertThat(json.write(bean)).doesNotHaveJsonPathValue("$.operation");
        assertThat(json.write(bean)).doesNotHaveJsonPathValue("$.integer");
    }
}
