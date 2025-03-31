package com.akarakoutev.consumer_group.recording;

import reactor.core.publisher.Mono;

public interface Recorder<T> {

    Mono<Boolean> record(T message);

}
