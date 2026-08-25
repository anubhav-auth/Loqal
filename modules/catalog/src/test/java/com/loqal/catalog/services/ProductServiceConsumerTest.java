package com.loqal.catalog.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.loqal.catalog.entity.ProcessedEvent;
import com.loqal.catalog.entity.Product;
import com.loqal.catalog.repository.ProcessedEventRepository;
import com.loqal.catalog.repository.ProductRepository;
import com.loqal.contracts.events.StockReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceConsumerTest {

    private ProductRepository productRepository;
    private ProcessedEventRepository processedEventRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private ProductService productService;

    private final UUID productId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(String.class), any(String.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        productService = new ProductService(productRepository, processedEventRepository, kafkaTemplate,
                new ObjectMapper().registerModule(new ParameterNamesModule()));
        ReflectionTestUtils.setField(productService, "orderCancellationTopic", "order-cancel");
        ReflectionTestUtils.setField(productService, "stockReservationResultTopic", "stock-reservation-result");
    }

    private Product product(int quantity) {
        Product p = new Product();
        p.setId(productId);
        p.setName("Widget");
        p.setPriceMinor(1000L);
        p.setQuantity(quantity);
        p.setMerchantId(UUID.randomUUID());
        return p;
    }

    private String reservationPayload(int quantity) {
        return "{\"orderId\":\"" + orderId + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}]}";
    }

    private String cancellationPayload(int quantity) {
        return "{\"orderId\":\"" + orderId + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}]}";
    }

    @Test
    void consumeOrderCreationRequest_success() {
        when(processedEventRepository.findById(orderId)).thenReturn(Mono.empty());
        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Mono.just(product(10)));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(processedEventRepository.save(any(ProcessedEvent.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        productService.consumeOrderCreationRequest(reservationPayload(2));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, timeout(2000)).send(eq("stock-reservation-result"), payloadCaptor.capture());

        String json = payloadCaptor.getValue();
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(json.contains(orderId.toString()));
    }

    @Test
    void consumeOrderCreationRequest_insufficientStock() {
        when(processedEventRepository.findById(orderId)).thenReturn(Mono.empty());
        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Mono.just(product(1)));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(processedEventRepository.save(any(ProcessedEvent.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        productService.consumeOrderCreationRequest(reservationPayload(5));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, timeout(2000)).send(eq("stock-reservation-result"), payloadCaptor.capture());

        String json = payloadCaptor.getValue();
        assertTrue(json.contains("\"status\":\"FAILED\""));
        assertTrue(json.contains(orderId.toString()));
    }

    @Test
    void consumeOrderCreationRequest_duplicate() {
        ProcessedEvent existingEvent = new ProcessedEvent(orderId, StockReservationResponse.STATUS_SUCCESS, null, false);
        when(processedEventRepository.findById(orderId)).thenReturn(Mono.just(existingEvent));

        productService.consumeOrderCreationRequest(reservationPayload(2));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, timeout(2000)).send(eq("stock-reservation-result"), payloadCaptor.capture());

        String json = payloadCaptor.getValue();
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        verify(productRepository, never()).findByIdWithPessimisticLock(any());
    }

    @Test
    void consumeOrderCancellation_success() {
        when(processedEventRepository.findById(orderId)).thenReturn(Mono.empty());
        Product existing = product(5);
        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Mono.just(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(processedEventRepository.save(any(ProcessedEvent.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        productService.consumeOrderCancellation(cancellationPayload(3));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, timeout(2000)).save(productCaptor.capture());
        assertEquals(8, productCaptor.getValue().getQuantity());
        verify(processedEventRepository, timeout(2000)).save(any(ProcessedEvent.class));
    }

    @Test
    void consumeOrderCancellation_duplicate() {
        ProcessedEvent cancelled = new ProcessedEvent(orderId, "CANCELLED", null, false);
        when(processedEventRepository.findById(orderId)).thenReturn(Mono.just(cancelled));

        productService.consumeOrderCancellation(cancellationPayload(3));

        verify(productRepository, never()).findByIdWithPessimisticLock(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    void consumeOrderCreationRequest_invalidJson() {
        productService.consumeOrderCreationRequest("not valid json {{{");
        verify(kafkaTemplate, never()).send(any(String.class), any(String.class));
    }

    @Test
    void consumeOrderCancellation_invalidJson() {
        productService.consumeOrderCancellation("bad json!!!");
        verify(productRepository, never()).findByIdWithPessimisticLock(any());
    }
}
