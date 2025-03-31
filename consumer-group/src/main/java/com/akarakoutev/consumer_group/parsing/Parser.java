package com.akarakoutev.consumer_group.parsing;

import io.lettuce.core.pubsub.api.reactive.ChannelMessage;

public interface Parser<T> {

    T parse(ChannelMessage<String, String> rawMessage);

}
