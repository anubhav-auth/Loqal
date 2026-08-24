package com.loqal.contracts.events;

/**
 * Kafka topic names for the order saga. Single source of truth.
 * Overrides via environment properties must match these defaults.
 */
public final class Topics {
    private Topics() {}

    public static final String ORDER_CREATION_REQUESTED = "order-creation-requested";
    public static final String STOCK_RESERVATION_RESULT = "stock-reservation-result";
    public static final String ORDER_CANCEL = "order-cancel";
    public static final String ORDER_CANCEL_DLT = "order-cancel-dlt";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String REFUND_REQUESTED = "refund-requested";
    public static final String REFUND_COMPLETED = "refund-completed";

    /** Consumer group shared by modules of the monolith that coordinate the saga. */
    public static final String GROUP_ORDERS = "order-service-group";
    public static final String GROUP_CATALOG = "product-service-group";
    public static final String GROUP_PAYMENTS = "payment-service-group";
}
