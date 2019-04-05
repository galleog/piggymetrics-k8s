package com.github.galleog.piggymetrics.gateway.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc;
import com.github.galleog.piggymetrics.gateway.dto.User;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

/**
 * Tests for {@link RestExceptionHandler}.
 */
@RunWith(SpringRunner.class)
public class RestExceptionHandlerTest extends BaseRouterTest {
    private ReactorAccountServiceGrpc.AccountServiceImplBase accountService;

    @Before
    public void setUp() throws Exception {
        accountService = spy(new ReactorAccountServiceGrpc.AccountServiceImplBase() {
        });

        grpcCleanup.register(InProcessServerBuilder.forName(AccountHandler.ACCOUNT_SERVICE)
                .directExecutor()
                .addService(accountService)
                .build()
                .start());
    }

    /**
     * Test for gRPC {@link Status#NOT_FOUND}.
     */
    @Test
    public void shouldReturnNotFoundIfStatusNotFoundThrown() {
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
    public void shouldReturnBadRequestIfStatusAlreadyExistsThrown() {
        StatusRuntimeException ex = Status.ALREADY_EXISTS.asRuntimeException();
        doReturn(Mono.error(ex)).when(accountService).createAccount(any());

        User user = User.builder()
                .username(ACCOUNT_NAME)
                .password(PASSWORD)
                .build();
        webClient.post()
                .uri("/accounts")
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
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody("{\"username\": \"test\", \"password\": \"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/accounts")
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.error").isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .jsonPath("$.message").isNotEmpty();
    }
}