package com.akarakoutev.consumer_group.consumption;

import reactor.core.publisher.Mono;

public interface Consumer<T> {

    /**
     * Process a message with a specific consumer.
     * @param message The message to be processed
     * @return A {@code Mono<T>} containing the now processed message
     */
    Mono<T> process(T message);

    /**
     * Check if a consumer is currently active
     * @return A {@code Mono<Boolean>} containing the result of the check
     */
    Mono<Boolean> onActiveConsumer();

    /**
     * Uniquely identify a message
     * @param message The message to uniquely identify
     * @return The unique message identifier
     */
    String identify(T message);
}
