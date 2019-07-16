package com.github.galleog.piggymetrics.gateway.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.github.galleog.piggymetrics.auth.grpc.ReactorUserServiceGrpc.UserServiceImplBase;
import com.github.galleog.piggymetrics.auth.grpc.UserServiceProto;
import com.github.galleog.piggymetrics.gateway.model.auth.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests for routing user requests.
 */
@RunWith(SpringRunner.class)
public class UserRequestRouterTest extends BaseRouterTest {
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";
    private static final String MASKED_PASSWORD = "******";

    @Captor
    private ArgumentCaptor<Mono<UserServiceProto.User>> userCaptor;

    private UserServiceImplBase userService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        userService = spyGrpcService(UserServiceImplBase.class, UserHandler.AUTH_SERVICE);
    }

    /**
     * Test for POST /users.
     */
    @Test
    public void shouldCreateNewUser() {
        UserServiceProto.User userProto = UserServiceProto.User.newBuilder()
                .setUserName(USERNAME)
                .setPassword(MASKED_PASSWORD)
                .build();
        doReturn(Mono.just(userProto)).when(userService).createUser(userCaptor.capture());

        User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();
        webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(user)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .value(u -> {
                    assertThat(u.getUsername()).isEqualTo(USERNAME);
                    assertThat(u.getPassword()).isEqualTo(MASKED_PASSWORD);
                });

        StepVerifier.create(userCaptor.getValue())
                .expectNextMatches(u -> {
                    assertThat(u.getUserName()).isEqualTo(USERNAME);
                    assertThat(u.getPassword()).isEqualTo(PASSWORD);
                    return true;
                }).verifyComplete();
    }
}