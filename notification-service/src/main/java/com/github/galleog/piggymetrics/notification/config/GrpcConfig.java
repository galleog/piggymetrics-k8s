package com.github.galleog.piggymetrics.notification.config;

import com.github.galleog.grpc.interceptor.LogClientInterceptor;
import com.github.galleog.grpc.interceptor.LogServerInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC.
 */
@Configuration(proxyBeanMethods = false)
public class GrpcConfig {
    @GrpcGlobalClientInterceptor
    public ClientInterceptor logClientInterceptor() {
        return new LogClientInterceptor();
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor logServerInterceptor() {
        return new LogServerInterceptor();
    }
}
