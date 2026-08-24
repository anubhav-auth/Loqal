package com.loqal.payments.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.contracts.events.RefundRequestedEvent;
import com.loqal.contracts.events.Topics;
import com.loqal.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRefundConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.REFUND_REQUESTED, groupId = Topics.GROUP_PAYMENTS)
    public void onRefundRequested(String payload) {
        try {
            RefundRequestedEvent event = objectMapper.readValue(payload, RefundRequestedEvent.class);
            paymentService.consumeRefundRequest(event).subscribe();
        } catch (Exception e) {
            log.error("Failed to process refund-requested payload: {}", e.getMessage());
        }
    }
}
