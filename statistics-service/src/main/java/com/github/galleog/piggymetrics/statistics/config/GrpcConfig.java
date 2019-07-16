package com.github.galleog.piggymetrics.statistics.config;

import com.github.galleog.grpc.interceptor.LogServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC.
 */
@Configuration
public class GrpcConfig {
    @Bean
    public GlobalServerInterceptorConfigurer globalServerInterceptorConfigurer() {
        return registry -> registry.addServerInterceptors(new LogServerInterceptor());
    }
}
