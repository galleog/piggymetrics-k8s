package com.github.galleog.protobuf.spring.cloud.stream;

import com.google.protobuf.AbstractMessageLite;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.MimeType;

import java.lang.reflect.InvocationTargetException;

/**
 * A {@link org.springframework.messaging.converter.MessageConverter} to convert a
 * protocol buffers objects, when expected content type is "application/x-protobuf".
 *
 * @author disc99
 */
public class ProtobufMessageConverter extends AbstractMessageConverter {

    public ProtobufMessageConverter() {
        super(new MimeType("application", "x-protobuf"));
    }

    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        return AbstractMessageLite.class.isAssignableFrom(clazz);
    }

    @Override
    protected Object convertToInternal(@NonNull Object payload, MessageHeaders headers, Object conversionHint) {
        return ((AbstractMessageLite<?, ?>) payload).toByteArray();
    }

    @Override
    protected Object convertFromInternal(Message<?> message, @NonNull Class<?> targetClass, Object conversionHint) {
        Object payload = message.getPayload();
        if (conversionHint instanceof MethodParameter) {
            return convert((MethodParameter) conversionHint, payload);
        }
        return payload;
    }

    private Object convert(MethodParameter conversionHint, Object payload)  {
        MethodParameter param = conversionHint.nestedIfOptional();
        if (Message.class.isAssignableFrom(param.getParameterType())) {
            param = param.nested();
        }

        try {
            return param.getNestedParameterType()
                    .getMethod("parseFrom", byte[].class)
                    .invoke(null, payload);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unsupported input: " + payload, e);
        }
    }
}