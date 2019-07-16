package com.github.galleog.piggymetrics.statistics.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.bigDecimalConverter;
import static com.github.galleog.protobuf.java.type.converter.Converters.dateConverter;

import com.github.galleog.piggymetrics.statistics.domain.DataPoint;
import com.github.galleog.piggymetrics.statistics.domain.ItemMetric;
import com.github.galleog.piggymetrics.statistics.grpc.ReactorStatisticsServiceGrpc;
import com.github.galleog.piggymetrics.statistics.grpc.StatisticsServiceProto;
import com.github.galleog.piggymetrics.statistics.repository.DataPointRepository;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.stream.Collectors;

/**
 * Service to get account statistics.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StatisticsService extends ReactorStatisticsServiceGrpc.StatisticsServiceImplBase {
    private final Scheduler jdbcScheduler;
    private final DataPointRepository dataPointRepository;

    @Override
    public Flux<StatisticsServiceProto.DataPoint> listDataPoints(Mono<StatisticsServiceProto.ListDataPointsRequest> request) {
        return request.flatMapMany(req ->
                Flux.defer(() -> Flux.fromStream(dataPointRepository.listByAccountName(req.getAccountName())))
                        .switchIfEmpty(Flux.error(
                                Status.NOT_FOUND
                                        .withDescription("No statistics found for account '" + req.getAccountName() + "'")
                                        .asRuntimeException()
                                )
                        ).subscribeOn(jdbcScheduler)
        ).map(this::toDataPointProto);
    }

    private StatisticsServiceProto.DataPoint toDataPointProto(DataPoint dataPoint) {
        return StatisticsServiceProto.DataPoint.newBuilder()
                .setAccountName(dataPoint.getAccountName())
                .setDate(dateConverter().convert(dataPoint.getDate()))
                .addAllMetrics(dataPoint.getMetrics().stream()
                        .map(this::toItemMetricProto)
                        .collect(Collectors.toList()))
                .putAllStatistics(dataPoint.getStatistics().entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().name(),
                                entry -> bigDecimalConverter().convert(entry.getValue())
                        )))
                .build();
    }

    private StatisticsServiceProto.ItemMetric toItemMetricProto(ItemMetric metric) {
        return StatisticsServiceProto.ItemMetric.newBuilder()
                .setType(StatisticsServiceProto.ItemType.valueOf(metric.getType().name()))
                .setTitle(metric.getTitle())
                .setMoneyAmount(bigDecimalConverter().convert(metric.getMoneyAmount()))
                .build();
    }
}
