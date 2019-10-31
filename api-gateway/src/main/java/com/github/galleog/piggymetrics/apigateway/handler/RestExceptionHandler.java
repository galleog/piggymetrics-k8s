package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.handler.ErrorAttributes.STATUS_KEY;
import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Custom {@link org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler}.
 */
@Slf4j
@Component
public class RestExceptionHandler extends AbstractErrorWebExceptionHandler {
    public RestExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                                ServerCodecConfigurer serverCodecConfigurer, ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, applicationContext);
        setMessageReaders(serverCodecConfigurer.getReaders());
        setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return route(all(), this::errorResponse);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        Mono<Void> result = super.handle(exchange, throwable);
        logger.warn(formatError(throwable, exchange.getRequest()), throwable);
        return result;
    }

    private Mono<ServerResponse> errorResponse(ServerRequest request) {
        Map<String, Object> errorAttributes = getErrorAttributes(request, false);
        return ServerResponse.status(HttpStatus.valueOf((int) errorAttributes.get(STATUS_KEY)))
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(errorAttributes);
    }

    private String formatError(Throwable error, ServerHttpRequest request) {
        String reason = error.getClass().getSimpleName() + ": " + error.getMessage();
        String path = request.getURI().getRawPath();
        return "Resolved [" + reason + "] for HTTP " + request.getMethod() + " " + path;
    }
}
