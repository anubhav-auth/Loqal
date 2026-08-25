package com.loqal.payments.gateway;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockPaymentProviderTest {

    private final MockPaymentProvider provider = new MockPaymentProvider();

    @Test
    void nameReturnsMock() {
        assertEquals("mock", provider.name());
    }

    @Test
    void createOrderReturnsDeterministicId() {
        StepVerifier.create(provider.createOrder("receipt_1", 1000L, "INR"))
                .assertNext(id -> assertTrue(id.startsWith("mock_order_")))
                .verifyComplete();
    }

    @Test
    void createOrderSequentialCallsIncrementId() {
        StepVerifier.create(provider.createOrder("r1", 1000L, "INR")
                        .zipWith(provider.createOrder("r2", 2000L, "INR")))
                .assertNext(tuple -> {
                    String first = tuple.getT1();
                    String second = tuple.getT2();
                    assertEquals("mock_order_", first.substring(0, first.lastIndexOf('_') + 1));
                    long firstNum = Long.parseLong(first.substring(first.lastIndexOf('_') + 1));
                    long secondNum = Long.parseLong(second.substring(second.lastIndexOf('_') + 1));
                    assertEquals(firstNum + 1, secondNum);
                })
                .verifyComplete();
    }

    @Test
    void refundReturnsDeterministicId() {
        StepVerifier.create(provider.refund("pay_xxx", 500L))
                .assertNext(id -> assertTrue(id.startsWith("mock_refund_")))
                .verifyComplete();
    }

    @Test
    void refundSequentialCallsIncrementId() {
        StepVerifier.create(provider.refund("pay_a", 100L)
                        .zipWith(provider.refund("pay_b", 200L)))
                .assertNext(tuple -> {
                    String first = tuple.getT1();
                    String second = tuple.getT2();
                    long firstNum = Long.parseLong(first.substring(first.lastIndexOf('_') + 1));
                    long secondNum = Long.parseLong(second.substring(second.lastIndexOf('_') + 1));
                    assertEquals(firstNum + 1, secondNum);
                })
                .verifyComplete();
    }

    @Test
    void verifyWebhookSignature_valid() {
        assertTrue(provider.verifyWebhookSignature("{}", "mock-signature"));
    }

    @Test
    void verifyWebhookSignature_invalidSignature() {
        assertFalse(provider.verifyWebhookSignature("{}", "wrong-signature"));
    }

    @Test
    void verifyWebhookSignature_nullSignature() {
        assertFalse(provider.verifyWebhookSignature("{}", null));
    }

    @Test
    void fakePaymentIdHasPrefix() {
        String id = MockPaymentProvider.fakePaymentId();
        assertTrue(id.startsWith("mock_pay_"));
        assertTrue(id.length() > 9);
    }
}
