package com.github.galleog.piggymetrics.apigateway.handler;

import static com.github.galleog.piggymetrics.apigateway.config.RouterConfig.DEMO_ACCOUNT;
import static com.github.galleog.piggymetrics.apigateway.handler.HandlerUtils.getCurrentUser;
import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;

import com.github.galleog.piggymetrics.apigateway.model.statistics.DataPoint;
import com.github.galleog.piggymetrics.apigateway.model.statistics.ItemMetric;
import com.github.galleog.piggymetrics.apigateway.model.statistics.StatisticalMetric;
import com.github.galleog.piggymetrics.statistics.grpc.ReactorStatisticsServiceGrpc;
import com.github.galleog.piggymetrics.statistics.grpc.StatisticsServiceProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Request handler for statistics.
 */
@Slf4j
@Component
public class StatisticsHandler {
    @VisibleForTesting
    static final String STATISTICS_SERVICE = "statistics-service";

    @GrpcClient(STATISTICS_SERVICE)
    private ReactorStatisticsServiceGrpc.ReactorStatisticsServiceStub statisticsServiceStub;

    /**
     * Gets statistical metrics for the current user.
     *
     * @param request the server request
     * @return a list of data points for the current user
     */
    public Mono<ServerResponse> getCurrentAccountStatistics(ServerRequest request) {
        return getStatistics(getCurrentUser(request));
    }

    /**
     * Gets statistical metrics for the demo account.
     *
     * @param request the server request
     * @return a list of data points for the demo account
     */
    public Mono<ServerResponse> getDemoStatistics(ServerRequest request) {
        return getStatistics(Mono.just(DEMO_ACCOUNT));
    }

    private Mono<ServerResponse> getStatistics(Mono<String> userName) {
        Mono<StatisticsServiceProto.ListDataPointsRequest> request = userName.map(name ->
                StatisticsServiceProto.ListDataPointsRequest.newBuilder()
                        .setAccountName(name)
                        .build()
        );
        Flux<DataPoint> flux = request.as(statisticsServiceStub::listDataPoints)
                .map(this::toDataPoint);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(flux, DataPoint.class);
    }

    private DataPoint toDataPoint(StatisticsServiceProto.DataPoint dataPoint) {
        return DataPoint.builder()
                .accountName(dataPoint.getAccountName())
                .date(dateConverter().reverse().convert(dataPoint.getDate()))
                .metrics(dataPoint.getMetricsList().stream()
                        .map(this::toItemMetric)
                        .collect(ImmutableList.toImmutableList()))
                .statistics(dataPoint.getStatisticsMap().entrySet()
                        .stream()
                        .collect(ImmutableMap.toImmutableMap(
                                entry -> StatisticalMetric.valueOf(entry.getKey()),
                                entry -> bigDecimalConverter().reverse().convert(entry.getValue())
                        )))
                .build();
    }

    private ItemMetric toItemMetric(StatisticsServiceProto.ItemMetric metric) {
        return ItemMetric.builder()
                .type(metric.getType())
                .title(metric.getTitle())
                .moneyAmount(bigDecimalConverter().reverse().convert(metric.getMoneyAmount()))
                .build();
    }
}
