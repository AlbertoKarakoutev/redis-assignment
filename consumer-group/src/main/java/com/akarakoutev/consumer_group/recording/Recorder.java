package com.akarakoutev.consumer_group.recording;

import reactor.core.publisher.Mono;

public interface Recorder<T> {

    /**
     * Record a processed message
     * @param message The message to record
     * @return A {@code Mono<Boolean>} which contains the result of the recording operation
     */
    Mono<Boolean> record(T message);

}
