package com.akarakoutev.consumer_group.validation;

public interface Validator<T> {

    /**
     * Validate a message
     * @param message The message to validate
     */
    void validate(T message);

}
