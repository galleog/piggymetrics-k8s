package com.github.galleog.piggymetrics.apigateway.config;

import com.github.galleog.grpc.interceptor.LogClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for gRPC.
 */
@Profile("!test")
@Configuration
public class GrpcConfig {
    @Bean
    public GlobalClientInterceptorConfigurer globalClientInterceptorConfigurer() {
        return registry -> registry.addClientInterceptors(new LogClientInterceptor());
    }
}
