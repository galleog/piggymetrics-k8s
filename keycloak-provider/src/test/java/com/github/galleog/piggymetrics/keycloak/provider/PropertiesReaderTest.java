package com.github.galleog.piggymetrics.keycloak.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleEntry;
import java.util.Properties;

/**
 * Tests for {@link PropertiesReader}.
 */
class PropertiesReaderTest {
    private static final String TEST_RESOURCE = "test.properties";

    /**
     * Test for {@link PropertiesReader#getProperties(String)}.
     */
    @Test
    void shouldGetProperties() {
        Properties properties = new PropertiesReader().getProperties(TEST_RESOURCE);
        assertThat(properties).containsOnly(
                new SimpleEntry<>("prop1", "value1"), new SimpleEntry<>("prop2", "value2")
        );
    }
}