package com.github.galleog.piggymetrics.statistics.config;

import com.github.galleog.grpc.interceptor.LogServerInterceptor;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC.
 */
@Configuration(proxyBeanMethods = false)
public class GrpcConfig {
    @GrpcGlobalServerInterceptor
    public ServerInterceptor logServerInterceptor() {
        return new LogServerInterceptor();
    }
}
