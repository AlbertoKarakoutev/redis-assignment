package com.akarakoutev.consumer_group.exception;

public class MessageLockException extends RuntimeException {

    public MessageLockException(String message) {
        super(message);
    }

}
