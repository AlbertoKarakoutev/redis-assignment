package com.akarakoutev.consumer_group.consumption;

import com.akarakoutev.consumer_group.consumption.domain.Message;
import com.akarakoutev.consumer_group.exception.MessageValidationException;
import com.akarakoutev.consumer_group.redis.RedisConnectionService;
import com.akarakoutev.consumer_group.redis.RedisLockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@Scope("prototype")
public class MessageIdJsonConsumer extends JsonConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageIdJsonConsumer.class);

    public MessageIdJsonConsumer(
            RedisLockService redisLockService,
            RedisConnectionService redisConnectionService,
            ObjectMapper objectMapper
    ) {
        super(redisLockService, redisConnectionService, objectMapper);
    }

    /**
     * {@inheritDoc}
     * @param message The message to validate
     */
    @Override
    public void validate(JsonNode message) {
        if (!message.has(Message.MESSAGE_ID_KEY)) throw new MessageValidationException(String.format("Could not find expected key %s", Message.MESSAGE_ID_KEY));
        LOGGER.debug("Validating message {}", identify(message));
    }

    /**
     * {@inheritDoc} into a Redis Stream
     * @param message The message to record
     * @return A {@code Mono<Boolean>} which contains the result of the recording operation
     */
    @Override
    public Mono<Boolean> record(JsonNode message) {
        Iterator<Map.Entry<String, JsonNode>> iterator = message.fields();
        String[] messageProps = Stream.generate(() -> null)
                .takeWhile(__ -> iterator.hasNext())
                .map(__ -> iterator.next())
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue().textValue()))
                .toArray(String[]::new);

        return redisConnectionService.executeReactive(reactiveCommands -> reactiveCommands.xadd(PROCESSED_MESSAGES_KEY, (Object[]) messageProps))
                .doOnNext(messageStreamId -> LOGGER.debug("Recorded message {} in stream '{}' with ID: {}", identify(message), PROCESSED_MESSAGES_KEY, messageStreamId))
                .flatMap(messageStreamId -> Mono.just(messageStreamId != null));
    }

    /**
     * {@inheritDoc}
     * @param messageJsonNode The json node message to be processed
     * @return A {@code Mono<JsonNode>} containing the now processed message
     */
    @Override
    public Mono<JsonNode> process(JsonNode messageJsonNode) {
        LOGGER.debug("Processing message {} by consumer {}", identify(messageJsonNode), consumerId);
        try {
            Message message = objectMapper.treeToValue(messageJsonNode, Message.class);
            Message processedMessage = new Message(message.messageId(), UUID.randomUUID(), consumerId);
            return Mono.just(objectMapper.valueToTree(processedMessage));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String identify(JsonNode message) {
        return message.get(Message.MESSAGE_ID_KEY).textValue();
    }
}
