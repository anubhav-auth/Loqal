package com.loqal.contracts.exception;

/** HTTP 422 in the shared error envelope. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
