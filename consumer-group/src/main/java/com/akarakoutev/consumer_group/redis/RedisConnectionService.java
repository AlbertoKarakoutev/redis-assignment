package com.akarakoutev.consumer_group.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class RedisConnectionService {

    private final GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    private final RedisPubSubReactiveCommands<String, String> reactiveCommands;

    public RedisConnectionService(
            GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool,
            RedisPubSubReactiveCommands<String, String> reactiveCommands
    ) {
        this.connectionPool = connectionPool;
        this.reactiveCommands = reactiveCommands;
    }

    public <T> T executeSync(Function<RedisCommands<String, String>, T> syncCommands) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            return syncCommands.apply(connection.sync());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void executeAsync(Consumer<RedisAsyncCommands<String, String>> asyncCommands) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            asyncCommands.accept(connection.async());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeReactive(Function<RedisReactiveCommands<String, String>, T> reactiveCommands) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            return reactiveCommands.apply(connection.reactive());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T executeReactivePubSub(Function<RedisPubSubReactiveCommands<String, String>, T> reactiveCommandSupplier) {
        return reactiveCommandSupplier.apply(reactiveCommands);
    }
}
