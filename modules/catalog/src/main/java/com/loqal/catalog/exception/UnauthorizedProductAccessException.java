package com.loqal.catalog.exception;

public class UnauthorizedProductAccessException extends RuntimeException {
    public UnauthorizedProductAccessException() {
        super("Unauthorized action: User does not own this product");
    }
}