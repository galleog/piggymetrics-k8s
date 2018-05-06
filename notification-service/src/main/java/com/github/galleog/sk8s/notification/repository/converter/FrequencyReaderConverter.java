package com.github.galleog.sk8s.notification.repository.converter;

import com.github.galleog.sk8s.notification.domain.Frequency;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class FrequencyReaderConverter implements Converter<Integer, Frequency> {

    @Override
    public Frequency convert(Integer days) {
        return Frequency.withDays(days);
    }
}
