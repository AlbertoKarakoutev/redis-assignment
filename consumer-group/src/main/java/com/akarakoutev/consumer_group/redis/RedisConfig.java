package com.akarakoutev.consumer_group.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RedisConfig {

    @Bean
    @Lazy
    RedisURI redisUri(@Value("${spring.redis.host}") String redisHost, @Value("${spring.redis.port}") int redisPort) {
        return RedisURI.create(redisHost, redisPort);
    }

    @Bean
    @Lazy
    RedisClient redisClient(RedisURI redisUri) {
        return RedisClient.create(redisUri);
    }

    @Bean
    @Lazy
    public RedisPubSubReactiveCommands<String, String> reactivePubSubCommands(RedisClient redisClient) {
        return redisClient.connectPubSub().reactive();
    }

    @Bean
    @Lazy
    public GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool(RedisClient redisClient) {
        return ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, new GenericObjectPoolConfig<>());
    }
}
