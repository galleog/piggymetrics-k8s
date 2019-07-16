package com.github.galleog.piggymetrics.auth.config;

import io.grpc.inprocess.InProcessServerBuilder;
import net.devh.boot.grpc.server.config.GrpcServerProperties;
import net.devh.boot.grpc.server.serverfactory.AbstractGrpcServerFactory;
import net.devh.boot.grpc.server.serverfactory.GrpcServerFactory;
import net.devh.boot.grpc.server.service.GrpcServiceDefinition;
import net.devh.boot.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

/**
 * Test configuration for gRPC that issues in-process requests.
 */
@TestConfiguration
public class GrpcTestConfig {
    public static final String SERVICE_NAME = "auth-service";

    @Bean
    public GrpcServerFactory inProcessServerFactory(GrpcServerProperties properties, GrpcServiceDiscoverer serviceDiscoverer) {
        GrpcServerFactory factory = new InProcessServerFactory(properties);
        for (GrpcServiceDefinition service : serviceDiscoverer.findGrpcServices()) {
            factory.addService(service);
        }
        return factory;
    }

    private static final class InProcessServerFactory extends AbstractGrpcServerFactory<InProcessServerBuilder> {
        InProcessServerFactory(GrpcServerProperties properties) {
            super(properties, ImmutableList.of());
        }

        @Override
        protected InProcessServerBuilder newServerBuilder() {
            return InProcessServerBuilder.forName(SERVICE_NAME)
                    .directExecutor();
        }
    }
}
