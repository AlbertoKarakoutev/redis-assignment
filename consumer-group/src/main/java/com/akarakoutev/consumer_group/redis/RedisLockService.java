package com.akarakoutev.consumer_group.redis;

import com.akarakoutev.consumer_group.lock.LockService;
import com.akarakoutev.consumer_group.exception.MessageLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisLockService implements LockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLockService.class);

    private final RedisConnectionService redisConnectionService;
    private final long redisLockDurationSeconds;

    @Autowired
    public RedisLockService(
            RedisConnectionService redisConnectionService,
            @Value("${spring.redis.lock.duration-seconds}") long redisLockDurationSeconds
    ) {
        this.redisConnectionService = redisConnectionService;
        this.redisLockDurationSeconds = redisLockDurationSeconds;
    }

    @Override
    public synchronized Mono<Boolean> acquireLock(String key) {
        return redisConnectionService.executeReactive(reactiveCommands ->
            reactiveCommands
                    .setnx(key, "")
                    .doOnError(error -> LOGGER.error("Error acquiring lock on key {}", key, error))
                    .doOnNext(lockAcquired -> LOGGER.debug("Acquiring lock {} successful: {}", key, lockAcquired))
                    .filter(lockAcquired -> lockAcquired)
                    .flatMap(__ -> reactiveCommands
                            .expire(key, redisLockDurationSeconds)
                            .doOnError(error -> LOGGER.error("Error setting expiration", error))
                            .doOnNext(expirationSet -> LOGGER.debug("Setting expiration on lock {} successful: {}", key, expirationSet))
                            .filter(expirationSet -> expirationSet)
                            .switchIfEmpty(Mono.error(new MessageLockException(String.format("Could not set expiration on lock for key %s", key)))))
        );
    }

    @Override
    public synchronized Mono<Boolean> releaseLock(String key) {
        return redisConnectionService.executeReactive(reactiveCommands ->
                reactiveCommands
                        .del(key)
                        .flatMap(deletedKeyCount -> Mono.just(deletedKeyCount != null && deletedKeyCount > 0))
                        .doOnNext(lockReleased -> LOGGER.debug("Releasing lock {} successful: {}", key, lockReleased))
        );
    }
}
