package com.github.galleog.piggymetrics.core.enums.hibernate;

import com.github.galleog.piggymetrics.core.enums.Enum;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SingleColumnType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Base class for custom Hibernate types that are used to serialize {@link Enum} values to and from JDBC.
 */
public abstract class AbstractHibernateEnumType implements UserType, ParameterizedType {
    /**
     * Parameter name that defines the class of the enumeration.
     */
    public static final String PARAMETER_NAME = "enumClass";

    private Class<? extends Enum<?>> enumClass;

    @Override
    public int[] sqlTypes() {
        return new int[]{getKeyType().sqlType()};
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setParameterValues(Properties parameters) {
        Validate.notEmpty(parameters);
        String className = parameters.getProperty(PARAMETER_NAME);
        try {
            enumClass = (Class<? extends Enum<?>>) Class.forName(className);
        } catch (Exception e) {
            throw new HibernateException(e);
        }
    }

    @Override
    public Class<?> returnedClass() {
        return enumClass;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean equals(Object x, Object y) throws HibernateException {
        return ObjectUtils.equals(x, y);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int hashCode(Object x) throws HibernateException {
        return ObjectUtils.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        Object key = getKeyType().nullSafeGet(rs, names, session, owner);
        if (key == null) {
            return null;
        }
        return Enum.valueOf(enumClass, key);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        Object key = value == null ? null : ((Enum<?>) value).getKey();
        getKeyType().nullSafeSet(st, key, index, session);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    /**
     * Gets the Hibernate type used to store keys of the enumeration.
     */
    protected abstract SingleColumnType<?> getKeyType();
}
