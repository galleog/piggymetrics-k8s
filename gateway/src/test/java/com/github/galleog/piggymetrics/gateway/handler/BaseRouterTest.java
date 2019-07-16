package com.github.galleog.piggymetrics.gateway.handler;

import static org.mockito.Mockito.spy;

import com.github.galleog.piggymetrics.gateway.config.GrpcTestConfig;
import com.github.galleog.piggymetrics.gateway.config.SecurityTestConfig;
import io.grpc.BindableService;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

/**
 * Base class for routing requests.
 */
@ActiveProfiles("test")
@Import({SecurityTestConfig.class, GrpcTestConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseRouterTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Autowired
    protected WebTestClient webClient;

    /**
     * Creates a spy of a gRPC {@link BindableService} and cleans it up after tests.
     *
     * @param cls         the service class
     * @param serviceName the service name
     * @param <T>         the service type
     * @return a spy of the gRPC service
     * @throws IOException if unable to bind the service
     */
    protected <T extends BindableService> T spyGrpcService(Class<T> cls, String serviceName) throws IOException {
        T service = spy(cls);
        grpcCleanup.register(InProcessServerBuilder.forName(serviceName)
                .directExecutor()
                .addService(service)
                .build()
                .start());
        return service;
    }
}
