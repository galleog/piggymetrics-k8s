package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.handler.HandlerUtils.USERNAME_CLAIM;
import static org.mockito.Mockito.spy;

import com.github.galleog.piggymetrics.apigateway.config.GrpcTestConfig;
import io.grpc.BindableService;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.JwtMutator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

/**
 * Base class for routing requests.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@Import(GrpcTestConfig.class)
public class BaseRouterTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Autowired
    WebTestClient webClient;

    /**
     * Creates a spy of a gRPC {@link BindableService} and cleans it up after tests.
     *
     * @param cls         the service class
     * @param serviceName the service name
     * @param <T>         the service type
     * @return a spy of the gRPC service
     * @throws IOException if unable to bind the service
     */
    <T extends BindableService> T spyGrpcService(Class<T> cls, String serviceName) throws IOException {
        T service = spy(cls);
        grpcCleanup.register(InProcessServerBuilder.forName(serviceName)
                .directExecutor()
                .addService(service)
                .build()
                .start());
        return service;
    }

    /**
     * Mocks a JWT token.
     */
    JwtMutator mockJwt(String userName) {
        return SecurityMockServerConfigurers.mockJwt()
                .jwt(builder -> builder.claim(USERNAME_CLAIM, userName));
    }
}
