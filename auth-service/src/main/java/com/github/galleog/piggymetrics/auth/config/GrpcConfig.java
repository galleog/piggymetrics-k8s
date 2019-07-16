package com.github.galleog.piggymetrics.auth.config;

import com.github.galleog.grpc.interceptor.LogServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for gRPC.
 */
@Configuration
@Profile("!test")
public class GrpcConfig {
    @Bean
    public GlobalServerInterceptorConfigurer globalServerInterceptorConfigurer() {
        return registry -> registry.addServerInterceptors(new LogServerInterceptor());
    }
}
