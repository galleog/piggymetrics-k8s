package com.github.galleog.piggymetrics.apigateway.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.github.galleog.piggymetrics.account.grpc.ReactorAccountServiceGrpc.AccountServiceImplBase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
    private static final String ACCOUNT_NAME = "test";

    /**
     * Test for gRPC {@link Status#NOT_FOUND}.
     */
    @Test
    public void shouldReturnNotFoundIfStatusNotFoundThrown() throws Exception {
        AccountServiceImplBase accountService = spyGrpcService(AccountServiceImplBase.class, AccountHandler.ACCOUNT_SERVICE);
        StatusRuntimeException ex = Status.NOT_FOUND.asRuntimeException();
        doReturn(Mono.error(ex)).when(accountService).getAccount(any());

        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .get()
                .uri("/accounts/current")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/accounts/current")
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.error").isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase())
                .jsonPath("$.message").isEqualTo(ex.getMessage());
    }

    /**
     * Test for thrown {@link org.springframework.core.codec.CodecException}.
     */
    @Test
    public void shouldReturnBadRequestIfCodecExceptionIsThrown() {
        webClient.mutateWith(mockJwt(ACCOUNT_NAME))
                .put()
                .uri("/accounts/current")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody("{\"items\": [], \"saving\": {\"interest\": 0.0, \"deposit\": false, \"capitalization\": false}}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/accounts/current")
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.error").isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .jsonPath("$.message").isNotEmpty();
    }
}