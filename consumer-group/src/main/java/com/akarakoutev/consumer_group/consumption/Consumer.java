package com.akarakoutev.consumer_group.consumption;

import reactor.core.publisher.Mono;

public interface Consumer<T> {

    Mono<T> process(T message);

    Mono<Boolean> onActiveConsumer();

    String identify(T message);
}
