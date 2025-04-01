package com.akarakoutev.consumer_group.lock;

import reactor.core.publisher.Mono;

public interface LockService {

    /**
     * Acquire a lock for a key. After a lock is acquired, it can not be acquired from anywhere else until it is released.
     * @param key The key, for which to acquire the lock
     * @return A {@code Mono<Boolean>} containing the result of the lock creation
     */
    Mono<Boolean> acquireLock(String key);

    /**
     * Release an acquired redis lock
     * @param key The key, for which to release the lock
     * @return A {@code Mono<Boolean>} containing the result of the lock releasing
     */
    Mono<Boolean> releaseLock(String key);

}
