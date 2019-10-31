package com.github.galleog.piggymetrics.keycloak.provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reader for property files.
 */
public class PropertiesReader {
    /**
     * Gets all properties from a classpath resource.
     *
     * @param resource the name of the resource
     * @return properties from the resource
     */
    public Properties getProperties(String resource) {
        Validate.notBlank(resource);

        Properties properties = new Properties();
        try (InputStream input = this.getClass().getResourceAsStream(StringUtils.prependIfMissing(resource, "/"))) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource " + resource);
        }
        return properties;
    }
}
