package com.github.galleog.piggymetrics.autoconfigure.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;

import java.util.function.Function;

/**
 * Helper for {@link KafkaReceiver}.
 */
@RequiredArgsConstructor
public class ReactiveKafkaReceiverHelper<K, V> implements SmartLifecycle {
    private final ReactiveKafkaConsumerTemplate<K, V> consumerTemplate;
    private final Function<Flux<ConsumerRecord<K, V>>, Mono<Void>> consumer;

    private Disposable disposable = null;

    @Override
    public synchronized void start() {
        if (this.disposable == null || this.disposable.isDisposed()) {
            var flux = this.consumerTemplate.receiveAutoAck();
            this.disposable = this.consumer.apply(flux).subscribe();
        }
    }

    @Override
    public synchronized void stop() {
        if (this.disposable != null && !this.disposable.isDisposed()) {
            this.disposable.dispose();
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return this.disposable != null && !this.disposable.isDisposed();
    }
}
