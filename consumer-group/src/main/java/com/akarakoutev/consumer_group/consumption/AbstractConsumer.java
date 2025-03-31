package com.akarakoutev.consumer_group.consumption;

import com.akarakoutev.consumer_group.lock.LockService;
import com.akarakoutev.consumer_group.exception.ConsumerNotActiveException;
import com.akarakoutev.consumer_group.exception.ConsumerRegistrationException;
import com.akarakoutev.consumer_group.parsing.Parser;
import com.akarakoutev.consumer_group.recording.Recorder;
import com.akarakoutev.consumer_group.redis.RedisConnectionService;
import com.akarakoutev.consumer_group.validation.Validator;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public abstract class AbstractConsumer<T> implements Consumer<T>, Parser<T>, Validator<T>, Recorder<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConsumer.class);

    public static final String PROCESSED_MESSAGES_KEY = "messages:processed";
    static final String PUBLISHED_MESSAGES_CHANNEL = "messages:published";
    static final String CONSUMER_ID_KEY = "consumer:ids";
    private static final String LOCK_KEY_FORMAT = "lock:%s";

    private boolean consumerActive = true;
    private final LockService lockService;
    protected final UUID consumerId;
    protected final RedisConnectionService redisConnectionService;

    public AbstractConsumer(
            LockService lockService,
            RedisConnectionService redisConnectionService
    ) {
        this.lockService = lockService;
        this.consumerId = UUID.randomUUID();
        this.redisConnectionService = redisConnectionService;

        init(PUBLISHED_MESSAGES_CHANNEL);
    }

    /**
     * {@inheritDoc}
     * @return A {@code Mono<Boolean>} containing the result of the check
     */
    @Override
    public Mono<Boolean> onActiveConsumer() {
        return redisConnectionService.executeReactive(reactiveCommands -> reactiveCommands.hexists(CONSUMER_ID_KEY, consumerId.toString()))
                .doOnNext(consumerActive -> LOGGER.debug("Checked active state of consumer {}: {}", consumerId, consumerActive))
                .doOnNext(thisConsumerActive -> consumerActive = thisConsumerActive);
    }

    /**
     * Initialize the consumer. Register it as an active consumer via the {@code consumerId}. The consumer is registered for the duration of 10 seconds
     * and is responsible for refreshing this time. If the time is not refreshed, the consumer will not consume messages and will not be considered active.
     * After registration, the consumer is subscribed to all supplied channels abd begins reactively listening to Redis Pub/Sub messages.
     * @param subscriptionChannels The redis pub/sub channels, to which to subscribe
     */
    private void init(String... subscriptionChannels) {
        redisConnectionService.executeReactive(reactiveCommands ->
            reactiveCommands
                    .hset(CONSUMER_ID_KEY, consumerId.toString(), "active")
                    .filter(registered -> registered)
                    .switchIfEmpty(Mono.error(new ConsumerRegistrationException(String.format("Could not register consumer %s", consumerId))))
                    .flatMap(__ -> reactiveCommands.hexpire(CONSUMER_ID_KEY, Duration.of(10, ChronoUnit.SECONDS), consumerId.toString()).collectList())
                    .filter(response -> response.get(0) == 1L)
                    .switchIfEmpty(Mono.error(new ConsumerRegistrationException(String.format("Could not set consumer expiration time for consumer %s", consumerId))))
                    .flatMap(__ ->
                            redisConnectionService.executeReactivePubSub(reactivePubSubCommands ->
                                    reactivePubSubCommands
                                            .subscribe(subscriptionChannels)
                                            .then()
                                            .doOnSuccess(___ ->
                                                    reactivePubSubCommands
                                                            .observeChannels()
                                                            .doOnNext(____ -> {
                                                                if (!consumerActive) throw new ConsumerNotActiveException(String.format("Consumer %s is not active", consumerId));
                                                            })
                                                            .doOnNext(this::process)
                                                            .subscribe()
                                            )
                            )
                    )
                    .block()
        );
    }

    /**
     * Process an incoming raw message. Main business logic for message processing. The message is parsed and validated.
     * A lock is acquired in order to ensure it is only processed by a single consumer. It is them validated and processed by the defined processor.
     * The result is recorded. The acquired lock is released at the end of the operation.
     * @param rawMessage The unprocessed incoming {@code ChannelMessage}
     * @see ChannelMessage
     */
    private void process(ChannelMessage<String, String> rawMessage) {
        T message = parse(rawMessage);
        validate(message);
        String redisLockKey = String.format(LOCK_KEY_FORMAT, identify(message));
        onActiveConsumer()
                .flatMap(__ -> lockService.acquireLock(redisLockKey))
                .flatMap(__ -> process(message))
                .flatMap(this::record)
                .doOnError(error -> LOGGER.error("Error processing message {}", message, error))
                .flatMap(__ -> lockService.releaseLock(redisLockKey))
                .subscribe();
    }

    /**
     * Refresh the consumer activity. If this is not called the consumer will be considered stale and will not process messages
     */
    @Scheduled(fixedRate = 8000)
    private void keepAlive() {
        if (!consumerActive) return;
        LOGGER.info("Refreshing consumer {}", consumerId);
        redisConnectionService.executeAsync(asyncCommands ->
                asyncCommands.hexpire(CONSUMER_ID_KEY, Duration.of(10, ChronoUnit.SECONDS), consumerId.toString())
        );
    }
}
