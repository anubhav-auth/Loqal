package com.loqal.contracts.exception;

/** HTTP 422 in the shared error envelope — coupon invalid/expired/limit-reached. */
public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String message) {
        super(message);
    }
}
