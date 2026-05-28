package com.kraft.lotto.web;

public class CollectionLockException extends RuntimeException {

    public CollectionLockException(String message, Exception cause) {
        super(message, cause);
    }
}
