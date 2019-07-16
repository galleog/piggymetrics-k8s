package com.github.galleog.piggymetrics.gateway.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc.AccountServiceImplBase;
import com.github.galleog.piggymetrics.auth.grpc.ReactorUserServiceGrpc.UserServiceImplBase;
import com.github.galleog.piggymetrics.gateway.model.auth.User;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

/**
 * Tests for {@link RestExceptionHandler}.
 */
@RunWith(SpringRunner.class)
public class RestExceptionHandlerTest extends BaseRouterTest {
    private static final String ACCOUNT_NAME = "test";
    private static final String PASSWORD = "secret";

    /**
     * Test for gRPC {@link Status#NOT_FOUND}.
     */
    @Test
    @WithMockUser(ACCOUNT_NAME)
    public void shouldReturnNotFoundIfStatusNotFoundThrown() throws Exception {
        AccountServiceImplBase accountService = spyGrpcService(AccountServiceImplBase.class, AccountHandler.ACCOUNT_SERVICE);
        StatusRuntimeException ex = Status.NOT_FOUND.asRuntimeException();
        doReturn(Mono.error(ex)).when(accountService).getAccount(any());

        webClient.get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/accounts/current")
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.error").isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase())
                .jsonPath("$.message").isEqualTo(ex.getMessage());
    }

    /**
     * Test for gRPC {@link Status#ALREADY_EXISTS}.
     */
    @Test
    public void shouldReturnBadRequestIfStatusAlreadyExistsThrown() throws Exception {
        UserServiceImplBase userService = spyGrpcService(UserServiceImplBase.class, UserHandler.AUTH_SERVICE);
        StatusRuntimeException ex = Status.ALREADY_EXISTS.asRuntimeException();
        doReturn(Mono.error(ex)).when(userService).createUser(any());

        User user = User.builder()
                .username(ACCOUNT_NAME)
                .password(PASSWORD)
                .build();
        webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(user)
                .exchange()
                .expectStatus().isBadRequest();
    }

    /**
     * Test for thrown {@link org.springframework.core.codec.CodecException}.
     */
    @Test
    public void shouldReturnBadRequestIfCodecExceptionIsThrown() {
        webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody("{\"username\": \"test\", \"password\": \"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/users")
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.error").isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .jsonPath("$.message").isNotEmpty();
    }
}