package com.github.galleog.piggymetrics.gateway.handler;

import com.github.galleog.piggymetrics.gateway.config.GrpcTestConfig;
import com.github.galleog.piggymetrics.gateway.config.SecurityTestConfig;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Base class for routing requests.
 */
@ActiveProfiles("test")
@WithMockUser(BaseRouterTest.ACCOUNT_NAME)
@Import({SecurityTestConfig.class, GrpcTestConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseRouterTest {
    static final String ACCOUNT_NAME = "test";
    static final String PASSWORD = "secret";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Autowired
    protected WebTestClient webClient;
}
