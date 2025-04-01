package com.akarakoutev.consumer_group;

import com.akarakoutev.consumer_group.consumption.AbstractConsumer;
import com.akarakoutev.consumer_group.redis.RedisConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StateReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateReportService.class);

    private final RedisConnectionService redisConnectionService;
    private static long CURRENT_STREAM_SIZE = 0;
    private static final int REPORT_SECONDS = 5;

    @Autowired
    public StateReportService(RedisConnectionService redisConnectionService) {
        this.redisConnectionService = redisConnectionService;
    }

    /**
     * Report the processing speed at a constant rate.
     */
    @Scheduled(fixedRate = REPORT_SECONDS * 1000)
    private void reportProcessedMessages() {
        redisConnectionService.executeReactive(reactiveCommands ->
                reactiveCommands.xlen(AbstractConsumer.PROCESSED_MESSAGES_KEY)
                        .doOnNext(streamSize -> LOGGER.info("Average processing speed for the last {} seconds: {} m/s", REPORT_SECONDS, (streamSize - CURRENT_STREAM_SIZE) / REPORT_SECONDS))
                        .doOnNext(streamSize -> CURRENT_STREAM_SIZE = streamSize)
                        .subscribe()
        );
    }
}
