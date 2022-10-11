package com.github.galleog.piggymetrics.autoconfigure.kafka;

import reactor.kafka.sender.SenderOptions;

/**
 * Allows customizing {@link SenderOptions}.
 */
@FunctionalInterface
public interface SenderOptionsCustomizer<K, V> {
    /**
     * Customizes the given {@link SenderOptions}.
     *
     * @param options the options that should be changed
     * @return the customized result
     */
    SenderOptions<K, V> customize(SenderOptions<K, V> options);
}
