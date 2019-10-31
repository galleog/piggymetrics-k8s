package com.github.galleog.piggymetrics.core.enums.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.galleog.piggymetrics.core.enums.Enum;
import org.apache.commons.lang3.Validate;

import java.io.IOException;

/**
 * <a href="https://github.com/FasterXML/jackson">Jackson JSON</a> deserializer to convert
 * the key of an enumeration to the enumeration itself.
 */
public class EnumDeserializer<E extends Enum<?>> extends StdScalarDeserializer<E> implements ContextualDeserializer {
    private final JavaType keyType;

    /**
     * Default constructor.
     * <p/>
     * It is needed just to create an instance of the deserializer declared in the
     * {@link com.fasterxml.jackson.databind.annotation.JsonDeserialize} annotation. The really used deserializer
     * will be created by {@link #createContextual(DeserializationContext, BeanProperty)}.
     */
    public EnumDeserializer() {
        this(null, null);
    }

    private EnumDeserializer(Class<?> enumClass, JavaType keyType) {
        super(enumClass);
        this.keyType = keyType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property != null) {
            // get enumeration class
            Class<?> cls = property.getType().getRawClass();
            Validate.validState(Enum.class.isAssignableFrom(cls), "Class %s must be a subclass of Enum", cls.getName());

            TypeFactory typeFactory = ctxt.getTypeFactory();
            JavaType[] types = typeFactory.findTypeParameters(typeFactory.constructType(cls), Enum.class);
            if (types == null || types.length != 1) {
                throw new IllegalStateException("Can not find the type parameter for Enum of type " + cls.getName());
            }
            return new EnumDeserializer<E>(cls, types[0]);
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.getCurrentToken().isScalarValue()) {
            ctxt.reportWrongTokenException(keyType, JsonToken.START_OBJECT, "Scalar value expected");
        }

        Object key = ctxt.readValue(p, keyType);
        return key != null ? Enum.valueOf((Class<? extends E>) handledType(), key) : null;
    }
}
