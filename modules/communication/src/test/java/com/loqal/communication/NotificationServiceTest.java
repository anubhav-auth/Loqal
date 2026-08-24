package com.loqal.communication;

import com.loqal.communication.entity.Notification;
import com.loqal.communication.notify.NotificationChannel;
import com.loqal.communication.notify.NotificationService;
import com.loqal.communication.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationChannel channel;
    private NotificationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(NotificationRepository.class);
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        channel = mock(NotificationChannel.class);
        service = new NotificationService(repository, channel);
    }

    @Test
    void rendersTemplateVariables() {
        when(channel.send(any(), any(), any())).thenReturn(true);

        StepVerifier.create(service.send(null, Notification.CHANNEL_EMAIL,
                        "user@test.com", "order-confirmed",
                        "Order {{orderId}} confirmed",
                        "Hi {{name}}, your order {{orderId}} is on its way! {{unknown}}",
                        Map.of("name", "Priya", "orderId", "123")))
                .assertNext(n -> {
                    assertEquals("Hi Priya, your order 123 is on its way! ",
                            n.getBody());
                    assertEquals(Notification.STATUS_SENT, n.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void failedDispatchMarksFailed() {
        when(channel.send(any(), any(), any())).thenReturn(false);

        StepVerifier.create(service.send(null, Notification.CHANNEL_EMAIL,
                        "user@test.com", "t", "s", "b", Map.of()))
                .assertNext(n -> assertEquals(Notification.STATUS_FAILED, n.getStatus()))
                .verifyComplete();
    }

    @Test
    void cooldownSuppressesRepeatWithinWindow() {
        when(channel.send(any(), any(), any())).thenReturn(true);
        String recipient = "repeat@test.com";
        String template = "same";

        StepVerifier.create(service.send(null, Notification.CHANNEL_EMAIL,
                        recipient, template, "s", "b", Map.of()))
                .assertNext(n -> assertEquals(Notification.STATUS_SENT, n.getStatus()))
                .verifyComplete();

        StepVerifier.create(service.send(null, Notification.CHANNEL_EMAIL,
                        recipient, template, "s", "b", Map.of()))
                .assertNext(n -> assertEquals(Notification.STATUS_RATE_LIMITED, n.getStatus()))
                .verifyComplete();
    }

    @Test
    void unsupportedChannelFails() {
        StepVerifier.create(service.send(null, "PUSH",
                        "device-token", "t", "s", "b", Map.of()))
                .assertNext(n -> {
                    assertEquals(Notification.STATUS_FAILED, n.getStatus());
                    Mockito.verify(channel, Mockito.never())
                            .send(any(), any(), any());
                })
                .verifyComplete();
    }
}
