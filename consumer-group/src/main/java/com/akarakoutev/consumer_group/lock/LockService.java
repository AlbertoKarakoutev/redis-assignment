package com.akarakoutev.consumer_group.lock;

import reactor.core.publisher.Mono;

public interface LockService {

    Mono<Boolean> acquireLock(String key);

    Mono<Boolean> releaseLock(String key);

}
