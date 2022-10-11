package com.github.galleog.piggymetrics.autoconfigure.kafka;

import reactor.kafka.receiver.ReceiverOptions;

/**
 * Allows customizing {@link ReceiverOptions}.
 */
@FunctionalInterface
public interface ReceiverOptionsCustomizer<K, V> {
    /**
     * Customizes the given {@link ReceiverOptions}.
     *
     * @param options the options that should be changed
     * @return the customized result
     */
    ReceiverOptions<K, V> customize(ReceiverOptions<K, V> options);
}
