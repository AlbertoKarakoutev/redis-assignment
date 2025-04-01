package com.akarakoutev.consumer_group.consumption;

import com.akarakoutev.consumer_group.consumption.domain.Message;
import com.akarakoutev.consumer_group.exception.MessageValidationException;
import com.akarakoutev.consumer_group.redis.RedisConnectionService;
import com.akarakoutev.consumer_group.redis.RedisLockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.sync.RedisServerCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class MessageIdJsonConsumerUnitTest {

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    private MessageIdJsonConsumer messageIdJsonConsumer;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private RedisConnectionService redisConnectionService;

    private static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:latest"));

    @BeforeAll
    public static void setupClass() {
        redisContainer.withExposedPorts(6379).start();
    }

    @AfterAll
    public static void teardownClass() {
        redisContainer.stop();
    }

    @BeforeEach
    public void setup() {
        redisConnectionService.executeSync(RedisServerCommands::flushdb);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.redis.lock.duration-seconds", () -> 10);
        registry.add("consumer.group.size", () -> 0);
        registry.add("spring.task.scheduling.pool.size", () -> 1);
    }


    @Test
    public void testProcess() throws IOException {
        JsonNode messageJson = objectMapper.readTree(generateMessageIdJsonString());

        Message message = objectMapper.treeToValue(messageIdJsonConsumer.process(messageJson).block(), Message.class);

        assertEquals(messageJson.get(Message.MESSAGE_ID_KEY).textValue(), message.messageId().toString());
        assertNotNull(message.processingResult());
        assertEquals(messageIdJsonConsumer.getConsumerId(), message.processingConsumerId());
    }

    @Test
    public void testOnActiveConsumer() {
        MessageIdJsonConsumer testConsumer = new MessageIdJsonConsumer(redisLockService, redisConnectionService, objectMapper);

        // Consumer is active
        assertTrue(() -> redisConnectionService.executeSync(syncCommands ->
                syncCommands.hexists(AbstractConsumer.CONSUMER_ID_KEY, testConsumer.getConsumerId().toString())
        ));

        // Deactivate consumer
        redisConnectionService.executeSync(syncCommands ->
            syncCommands.hdel(AbstractConsumer.CONSUMER_ID_KEY, testConsumer.getConsumerId().toString())
        );

        // Consumer is not active
        assertFalse(() -> redisConnectionService.executeSync(syncCommands ->
                syncCommands.hexists(AbstractConsumer.CONSUMER_ID_KEY, testConsumer.getConsumerId().toString())
        ));

        assertFalse(testConsumer.onActiveConsumer().block());
    }

    @Test
    public void testIdentify() throws JsonProcessingException {
        JsonNode messageJson = objectMapper.readTree(generateMessageIdJsonString());

        Message message = objectMapper.treeToValue(messageJson, Message.class);
        assertEquals(message.messageId().toString(), messageIdJsonConsumer.identify(messageJson));
    }

    @Test
    public void testRecord() throws JsonProcessingException {
        assertEquals(0, (long) redisConnectionService.executeSync(syncCommands -> syncCommands.xlen(AbstractConsumer.PROCESSED_MESSAGES_KEY)));

        JsonNode messageJson = objectMapper.readTree(generateMessageIdJsonString());
        JsonNode processedMessageJson = messageIdJsonConsumer.process(messageJson).block();
        if (processedMessageJson == null) fail();

        assertEquals(Boolean.TRUE, messageIdJsonConsumer.record(processedMessageJson).block());
        assertEquals(1, (long) redisConnectionService.executeSync(syncCommands -> syncCommands.xlen(AbstractConsumer.PROCESSED_MESSAGES_KEY)));
        StreamMessage<String, String> recordedMessage = redisConnectionService
                .executeSync(syncCommands -> syncCommands.xread(XReadArgs.StreamOffset.from(AbstractConsumer.PROCESSED_MESSAGES_KEY, "0")))
                .get(0);

        Message message = objectMapper.convertValue(recordedMessage.getBody(), Message.class);

        assertEquals(messageJson.get(Message.MESSAGE_ID_KEY).textValue(), message.messageId().toString());
        assertNotNull(message.processingResult());
        assertEquals(messageIdJsonConsumer.getConsumerId(), message.processingConsumerId());
    }

    @Test
    public void testValidate() throws JsonProcessingException {
        JsonNode messageJson = objectMapper.readTree(generateMessageIdJsonString());
        assertNotThrows(MessageValidationException.class, () -> messageIdJsonConsumer.validate(messageJson));

        JsonNode emptyJson = objectMapper.readTree("{}");
        assertThrows(MessageValidationException.class, () -> messageIdJsonConsumer.validate(emptyJson));
    }

    private void assertThrows(Class<? extends Throwable> throwableClass, Runnable thrower) {
        if (!doesThrow(throwableClass, thrower)) fail();
    }

    private void assertNotThrows(Class<? extends Throwable> throwableClass, Runnable thrower) {
        if (doesThrow(throwableClass, thrower)) fail();
    }

    private boolean doesThrow(Class<? extends Throwable> throwableClass, Runnable thrower) {
        try {
            thrower.run();
        } catch (Exception e) {
            if (throwableClass == null || e.getClass().isAssignableFrom(throwableClass)) {
                return true;
            }
        }
        return false;
    }

    private String generateMessageIdJsonString() {
        return String.format("{\"%s\":\"%s\"}", Message.MESSAGE_ID_KEY, UUID.randomUUID());
    }
}
