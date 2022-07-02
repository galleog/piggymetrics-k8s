package com.github.galleog.protobuf.spring.cloud.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
public class ProtobufSpringCloudStreamAutoConfiguration {
    @Bean
    public MessageConverter protobufMessageConverter() {
        return new ProtobufMessageConverter();
    }
}
