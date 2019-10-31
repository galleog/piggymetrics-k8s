package com.github.galleog.piggymetrics.apigateway.handler;

import io.grpc.Status;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * {@link DefaultErrorAttributes} that handles gRPC-specific errors.
 */
@Component
public class ErrorAttributes extends DefaultErrorAttributes {
    static final String STATUS_KEY = "status";
    private static final String ERROR_KEY = "error";

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Map<String, Object> result = super.getErrorAttributes(request, includeStackTrace);
        if ((int) result.get(STATUS_KEY) == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            HttpStatus status = errorStatus(getError(request));
            result.put(STATUS_KEY, status.value());
            result.put(ERROR_KEY, status.getReasonPhrase());
        }
        return result;
    }

    private HttpStatus errorStatus(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return ((ResponseStatusException) error).getStatus();
        }
        if (error instanceof CodecException) {
            return HttpStatus.BAD_REQUEST;
        }

        Status status = Status.fromThrowable(error);
        switch (status.getCode()) {
            case UNAUTHENTICATED:
                return HttpStatus.UNAUTHORIZED;
            case NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS:
            case INVALID_ARGUMENT:
                return HttpStatus.BAD_REQUEST;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
