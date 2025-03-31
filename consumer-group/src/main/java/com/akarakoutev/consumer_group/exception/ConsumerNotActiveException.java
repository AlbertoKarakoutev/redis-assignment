package com.akarakoutev.consumer_group.exception;

public class ConsumerNotActiveException extends RuntimeException {

    public ConsumerNotActiveException(String message) {
        super(message);
    }

}
