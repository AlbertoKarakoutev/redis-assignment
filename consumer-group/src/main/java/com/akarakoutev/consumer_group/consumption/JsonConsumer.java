package com.akarakoutev.consumer_group.consumption;

import com.akarakoutev.consumer_group.redis.RedisConnectionService;
import com.akarakoutev.consumer_group.redis.RedisLockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class JsonConsumer extends AbstractConsumer<JsonNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonConsumer.class);

    protected final ObjectMapper objectMapper;

    public JsonConsumer(
            RedisLockService redisLockService,
            RedisConnectionService redisConnectionService,
            ObjectMapper objectMapper
    ) {
        super(redisLockService, redisConnectionService);
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode parse(ChannelMessage<String, String> rawMessage) {
        try {
            return objectMapper.readTree(rawMessage.getMessage());
        } catch (IOException e) {
            LOGGER.error("Could not parse raw message {} as JSON", rawMessage.getMessage(), e);
        }
        return null;
    }
}