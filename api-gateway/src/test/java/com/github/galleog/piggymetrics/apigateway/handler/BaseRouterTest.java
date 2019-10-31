package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.handler.HandlerUtils.USERNAME_CLAIM;
import static org.mockito.Mockito.spy;
import static org.springframework.security.oauth2.jwt.JwtClaimNames.SUB;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

import com.github.galleog.piggymetrics.apigateway.config.GrpcTestConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.BindableService;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import java.io.IOException;
import java.time.Instant;

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
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                ImmutableMap.of("alg", "none"),
                ImmutableMap.of(SUB, "user", "scope", "profile", USERNAME_CLAIM, userName));
        return new JwtMutator(jwt);
    }

    private static class JwtMutator implements WebTestClientConfigurer, MockServerConfigurer {
        private Jwt jwt;

        private JwtMutator(Jwt jwt) {
            this.jwt = jwt;
        }

        @Override
        public void beforeServerCreated(WebHttpHandlerBuilder builder) {
            configurer().beforeServerCreated(builder);
        }

        @Override
        public void afterConfigureAdded(WebTestClient.MockServerSpec<?> serverSpec) {
            configurer().afterConfigureAdded(serverSpec);
        }

        @Override
        public void afterConfigurerAdded(
                WebTestClient.Builder builder,
                @Nullable WebHttpHandlerBuilder httpHandlerBuilder,
                @Nullable ClientHttpConnector connector) {
            CsrfWebFilter filter = new CsrfWebFilter();
            filter.setRequireCsrfProtectionMatcher(e -> MatchResult.notMatch());
            httpHandlerBuilder.filters(filters -> filters.add(0, filter));
            configurer().afterConfigurerAdded(builder, httpHandlerBuilder, connector);
        }

        private <T extends WebTestClientConfigurer & MockServerConfigurer> T configurer() {
            return mockAuthentication(new JwtAuthenticationToken(this.jwt, ImmutableList.of()));
        }
    }
}
