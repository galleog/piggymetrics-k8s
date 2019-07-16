package com.github.galleog.piggymetrics.exchange.service;

import static com.github.galleog.protobuf.java.type.converter.Converters.moneyConverter;

import com.github.galleog.piggymetrics.exchange.grpc.ExchangeServiceProto;
import com.github.galleog.piggymetrics.exchange.grpc.ReactorExchangeServiceGrpc;
import com.github.galleog.protobuf.java.type.MoneyProto;
import io.grpc.Status;
import org.javamoney.moneta.Money;
import reactor.core.publisher.Flux;

import javax.money.MonetaryException;
import javax.money.convert.MonetaryConversions;

/**
 * Service for currency exchange.
 */
public class ExchangeService extends ReactorExchangeServiceGrpc.ExchangeServiceImplBase {
    @Override
    public Flux<MoneyProto.Money> convert(Flux<ExchangeServiceProto.ConvertRequest> request) {
        return request.map(req -> {
            try {
                Money money = moneyConverter().reverse().convert(req.getMoney());
                return money.with(MonetaryConversions.getConversion(req.getCurrencyCode()));
            } catch (NullPointerException | IllegalArgumentException | ArithmeticException | MonetaryException e) {
                throw Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage())
                        .withCause(e)
                        .asRuntimeException();
            }
        }).map(money -> moneyConverter().convert(money));
    }
}
