package com.github.galleog.piggymetrics.apigateway.config;

import com.google.common.collect.ImmutableList;
import io.grpc.inprocess.InProcessChannelBuilder;
import net.devh.boot.grpc.client.channelfactory.AbstractChannelFactory;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import net.devh.boot.grpc.client.config.GrpcChannelsProperties;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for gRPC that issues in-process requests.
 */
@TestConfiguration
public class GrpcTestConfig {
    @Bean
    public GrpcChannelFactory inProcessChannelFactory(GrpcChannelsProperties properties,
                                                      GlobalClientInterceptorRegistry globalClientInterceptorRegistry) {
        return new InProcessChannelFactory(properties, globalClientInterceptorRegistry);
    }

    private static class InProcessChannelFactory extends AbstractChannelFactory<InProcessChannelBuilder> {
        InProcessChannelFactory(GrpcChannelsProperties properties,
                                       GlobalClientInterceptorRegistry globalClientInterceptorRegistry) {
            super(properties, globalClientInterceptorRegistry, ImmutableList.of());
        }

        @Override
        protected InProcessChannelBuilder newChannelBuilder(String name) {
            return InProcessChannelBuilder.forName(name)
                    .directExecutor();
        }

        @Override
        protected void configureSecurity(InProcessChannelBuilder builder, String name) {
        }
    }
}
