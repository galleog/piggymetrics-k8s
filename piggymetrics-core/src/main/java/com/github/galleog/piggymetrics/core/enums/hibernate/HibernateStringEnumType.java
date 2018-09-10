package com.github.galleog.piggymetrics.core.enums.hibernate;

import org.hibernate.type.SingleColumnType;
import org.hibernate.type.StringType;

/**
 * Hibernate type used for enumerations whose keys are strings.
 */
public class HibernateStringEnumType extends AbstractHibernateEnumType {
    /**
     * Type class name.
     */
    public static final String CLASS_NAME = "com.github.galleog.piggymetrics.core.enums.hibernate.HibernateStringEnumType";

    @Override
    protected SingleColumnType<?> getKeyType() {
        return StringType.INSTANCE;
    }
}
