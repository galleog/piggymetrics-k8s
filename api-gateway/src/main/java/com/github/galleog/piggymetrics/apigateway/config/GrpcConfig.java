package com.github.galleog.piggymetrics.apigateway.config;

import com.github.galleog.grpc.interceptor.LogClientInterceptor;
import io.grpc.ClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for gRPC.
 */
@Profile("!test")
@Configuration(proxyBeanMethods = false)
public class GrpcConfig {
    @GrpcGlobalClientInterceptor
    public ClientInterceptor logClientInterceptor() {
        return new LogClientInterceptor();
    }
}
