package com.github.galleog.piggymetrics.apigateway.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.github.galleog.piggymetrics.apigateway.handler.AccountHandler;
import com.github.galleog.piggymetrics.apigateway.handler.NotificationHandler;
import com.github.galleog.piggymetrics.apigateway.handler.StatisticsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Configuration for Web routing.
 */
@Configuration(proxyBeanMethods = false)
public class RouterConfig {
    public static final String DEMO_ACCOUNT = "demo";

    @Bean
    public RouterFunction<ServerResponse> routeAccountRequests(AccountHandler handler) {
        return route().path("/accounts", builder ->
                builder.GET("/demo", request -> handler.getDemoAccount())
                        .GET("/current", handler::getCurrentAccount)
                        .PUT("/current", contentType(MediaType.APPLICATION_JSON), handler::updateCurrentAccount)
        ).build();
    }

    @Bean
    public RouterFunction<ServerResponse> routeNotificationRequests(NotificationHandler handler) {
        return route().path("/notifications", builder ->
                builder.GET("/current", handler::getCurrentNotificationsSettings)
                        .PUT("/current", contentType(MediaType.APPLICATION_JSON), handler::updateCurrentNotificationsSettings)
        ).build();
    }

    @Bean
    public RouterFunction<ServerResponse> routeDataPointRequests(StatisticsHandler handler) {
        return route().path("/statistics", builder ->
                builder.GET("/demo", request -> handler.getDemoStatistics())
                        .GET("/current", handler::getCurrentAccountStatistics)

        ).build();
    }
}
