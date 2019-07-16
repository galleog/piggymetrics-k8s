package com.github.galleog.piggymetrics.gateway.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.github.galleog.piggymetrics.gateway.handler.AccountHandler;
import com.github.galleog.piggymetrics.gateway.handler.NotificationHandler;
import com.github.galleog.piggymetrics.gateway.handler.StatisticsHandler;
import com.github.galleog.piggymetrics.gateway.handler.UserHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Configuration for Web routing.
 */
@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> routeIndex(@Value("classpath:/static/index.html") Resource indexHtml) {
        return route(GET("/"), request ->
                ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .syncBody(indexHtml));
    }

    @Bean
    public RouterFunction<ServerResponse> routeUserRequests(UserHandler handler) {
        return route(POST("/users"), handler::createUser);
    }

    @Bean
    public RouterFunction<ServerResponse> routeAccountRequests(AccountHandler handler) {
        return route().path("/accounts", builder ->
                builder.GET("/demo", handler::getDemoAccount)
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
                builder.GET("/current", handler::getCurrentAccountStatistics)
                        .GET("/{name}", handler::getStatisticsByAccountName)
        ).build();
    }
}
