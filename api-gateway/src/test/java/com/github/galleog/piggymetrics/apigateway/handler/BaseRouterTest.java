package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.handler.HandlerUtils.USERNAME_CLAIM;
import static org.mockito.Mockito.spy;

import com.asarkar.grpc.test.GrpcCleanupExtension;
import com.asarkar.grpc.test.Resources;
import com.github.galleog.piggymetrics.apigateway.config.GrpcTestConfig;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.JwtMutator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Duration;

/**
 * Base class for routing requests.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@ExtendWith({
        SpringExtension.class,
        MockitoExtension.class,
        GrpcCleanupExtension.class
})
@Import(GrpcTestConfig.class)
class BaseRouterTest {
    @Autowired
    WebTestClient webClient;

    private Resources resources;

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
        Server server = InProcessServerBuilder.forName(serviceName)
                .directExecutor()
                .addService(service)
                .build();
        resources.register(server.start(), Duration.ofSeconds(1));
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
