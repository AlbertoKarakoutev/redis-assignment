package com.akarakoutev.consumer_group.consumption.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record Message (
        @JsonProperty(MESSAGE_ID_KEY) UUID messageId,
        @JsonProperty("processing_result") UUID processingResult,
        @JsonProperty("processing_consumer_id") UUID processingConsumerId
) {

    public static final String MESSAGE_ID_KEY = "message_id";

    public Message(@JsonProperty(MESSAGE_ID_KEY) UUID messageId) {
        this(messageId, null, null);
    }
}
