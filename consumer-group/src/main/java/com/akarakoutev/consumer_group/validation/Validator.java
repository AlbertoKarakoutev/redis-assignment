package com.akarakoutev.consumer_group.validation;

public interface Validator<T> {

    void validate(T message);

}
