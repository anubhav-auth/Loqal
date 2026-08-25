package com.loqal.payments.controller;

import com.loqal.payments.gateway.PaymentProvider;
import com.loqal.payments.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentProvider paymentProvider;

    @Mock
    private PaymentService paymentService;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentProvider, paymentService);
    }

    @Test
    void handleWebhook_invalidSignature_returns400() {
        when(paymentProvider.verifyWebhookSignature("payload", "bad-sig"))
                .thenReturn(false);

        StepVerifier.create(controller.handleWebhook("payload", "bad-sig"))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(400))
                .verifyComplete();
    }

    @Test
    void handleWebhook_validSignature_returns200() {
        when(paymentProvider.verifyWebhookSignature("payload", "valid-sig"))
                .thenReturn(true);
        when(paymentService.handleCapturedWebhook("payload"))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.handleWebhook("payload", "valid-sig"))
                .assertNext(r -> assertThat(r.getStatusCode().is2xxSuccessful()).isTrue())
                .verifyComplete();
    }

    @Test
    void handleWebhook_serviceError_returns500() {
        when(paymentProvider.verifyWebhookSignature("payload", "valid-sig"))
                .thenReturn(true);
        when(paymentService.handleCapturedWebhook("payload"))
                .thenReturn(Mono.error(new RuntimeException("processing failed")));

        StepVerifier.create(controller.handleWebhook("payload", "valid-sig"))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(500))
                .verifyComplete();
    }

    @Test
    void handleWebhook_nullSignature_invalidSignature() {
        when(paymentProvider.verifyWebhookSignature("payload", null))
                .thenReturn(false);

        StepVerifier.create(controller.handleWebhook("payload", null))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(400))
                .verifyComplete();
    }
}
