package com.github.galleog.piggymetrics.core.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Base abstract class for enumerations.
 */
public abstract class Enum<T extends Serializable & Comparable<? super T>>
        implements Serializable, Comparable<Enum<T>> {
    private static Map<Class<?>, Entry<?>> enumClasses = new WeakHashMap<>();

    private final T key;
    private transient final int hashCode;
    protected transient String toString = null;

    /**
     * Creates a new enumeration value by its unique ksy.
     *
     * @param key the value key
     */
    protected Enum(T key) {
        Validate.notNull(key);

        init(key);
        this.key = key;
        this.hashCode = 7 + getEnumClass().hashCode() + 3 * key.hashCode();
    }

    /**
     * Gets the enumeration value by its class and unique key.
     *
     * @param enumClass the class whose value should be retrieved
     * @param key       the unique key that defines the value
     * @throws NullPointerException     if {@code enumClass} or {@code key} are {@code null}
     * @throws IllegalArgumentException if {@code enumClass} is not an enumeration type or
     *                                  contains no value with the key {@code key}
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<?>> E valueOf(Class<? extends E> enumClass, Object key) {
        Validate.notNull(key);
        Entry<?> entry = getEntry(enumClass);
        Enum<?> value = null;
        if (entry != null) {
            value = entry.map.get(key);
        }

        Validate.isTrue(value != null, "Enum class %s does not have constant with key '%s'",
                ClassUtils.getShortClassName(enumClass), key.toString());
        return (E) value;
    }

    /**
     * Gets all the values of the specified enumeration.
     *
     * @param enumClass the enumeration class whose values are needed
     * @throws NullPointerException     if {@code enumClass} is {@code null}
     * @throws IllegalArgumentException if {@code enumClass} is not an enumeration type
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<?>> List<E> values(Class<? extends E> enumClass) {
        Entry<?> entry = getEntry(enumClass);
        return entry == null ? Collections.emptyList() : (List<E>) entry.unmodifiableList;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable & Comparable<? super T>> Entry<T> createEntry(
            Class<? extends Enum<T>> enumClass) {
        Entry<T> entry = new Entry<>();
        Class<?> cls = enumClass.getSuperclass();
        while (cls != null && cls != Enum.class) {
            Entry<T> loopEntry = (Entry<T>) enumClasses.get(cls);
            if (loopEntry != null) {
                entry.list.addAll(loopEntry.list);
                entry.map.putAll(loopEntry.map);
                break;  // stop here, as this will already have had superclasses added
            }
            cls = cls.getSuperclass();
        }
        return entry;
    }

    private static <E extends Enum<?>> Entry<?> getEntry(Class<? extends E> enumClass) {
        Validate.notNull(enumClass);
        Validate.isAssignableFrom(Enum.class, enumClass);

        Entry<?> entry = enumClasses.get(enumClass);
        if (entry == null) {
            try {
                // try to initialize the class
                Class.forName(enumClass.getName(), true, enumClass.getClassLoader());
                entry = enumClasses.get(enumClass);
            } catch (Exception ignore) {
                // ignore
            }
        }
        return entry;
    }

    /**
     * Gets the key of the enumeration value.
     */
    @JsonValue
    public final T getKey() {
        return key;
    }

    /**
     * Gets the class of the enumeration type.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Enum<T>> getEnumClass() {
        return (Class<? extends Enum<T>>) getClass();
    }

    /**
     * Creates a deserialized object from the cache.
     */
    @SuppressWarnings("unchecked")
    protected Object readResolve() {
        Entry<T> entry = (Entry<T>) enumClasses.get(getEnumClass());
        return entry == null ? null : entry.map.get(getKey());
    }

    @SuppressWarnings("unchecked")
    private void init(T key) {
        Class<? extends Enum<T>> enumClass = getEnumClass();
        Validate.validState(enumClass != null, "getEnumClass() must not be null");

        Class<?> cls = getClass();
        boolean ok = false;
        while (cls != null && cls != Enum.class) {
            if (cls == enumClass) {
                ok = true;
                break;
            }
            cls = cls.getSuperclass();
        }
        Validate.validState(ok, "getEnumClass() must return a superclass of this class");

        Entry<T> entry;
        synchronized (Enum.class) {
            entry = (Entry<T>) enumClasses.get(enumClass);
            if (entry == null) {
                entry = createEntry(enumClass);
                // use copy on write to avoid synchronization of enumClasses
                Map<Class<?>, Entry<?>> map = new WeakHashMap<>();
                map.putAll(enumClasses);
                map.put(enumClass, entry);
                enumClasses = map;
            }
        }
        Validate.validState(!entry.map.containsKey(key), "The Enum key must be unique, '%s' has already been added",
                key.toString());
        entry.map.put(key, this);
        entry.list.add(this);
    }

    @Override
    public int compareTo(Enum<T> o) {
        if (o == this) {
            return 0;
        }
        if (o.getClass() != this.getClass()) {
            throw new ClassCastException(this.getClass() + " cannot be compared to " + o.getClass());
        }
        return key.compareTo(o.key);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        Enum<?> other = (Enum<?>) obj;
        return key.equals(other.getKey());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        if (toString == null) {
            String shortName = ClassUtils.getShortClassName(getEnumClass());
            toString = shortName + "[" + getKey() + "]";
        }
        return toString;
    }

    private static class Entry<T extends Serializable & Comparable<? super T>> {
        final Map<T, Enum<T>> map = new HashMap<>();
        final List<Enum<T>> list = new ArrayList<>(25);
        final List<Enum<T>> unmodifiableList = Collections.unmodifiableList(list);
    }
}
