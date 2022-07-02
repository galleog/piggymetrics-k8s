package com.github.galleog.piggymetrics.apigateway.handler;

import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom {@link org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler}.
 */
@Slf4j
@Component
@Order(-1)
public class RestExceptionHandler extends DefaultErrorWebExceptionHandler {
    public RestExceptionHandler(ErrorAttributes errorAttributes, WebProperties properties, ServerProperties serverProperties,
                                ServerCodecConfigurer serverCodecConfigurer, ApplicationContext applicationContext) {
        super(errorAttributes, properties.getResources(), serverProperties.getError(), applicationContext);
        setMessageReaders(serverCodecConfigurer.getReaders());
        setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return route(all(), this::renderErrorResponse);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        Mono<Void> result = super.handle(exchange, throwable);
        logger.warn(formatError(throwable, exchange.getRequest()), throwable);
        return result;
    }

    private String formatError(Throwable error, ServerHttpRequest request) {
        String reason = error.getClass().getSimpleName() + ": " + error.getMessage();
        String path = request.getURI().getRawPath();
        return "Resolved [" + reason + "] for HTTP " + request.getMethod() + " " + path;
    }
}
