package com.loqal.contracts.exception;

/** HTTP 409 in the shared error envelope. */
public class InvalidOrderStatusException extends RuntimeException {
    public InvalidOrderStatusException(String message) {
        super(message);
    }
}
