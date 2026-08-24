package com.loqal.app.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loqal.contracts.events.StockReservationRequest;
import com.loqal.orders.entity.Order;
import com.loqal.orders.entity.OrderItem;
import com.loqal.orders.repository.OrderRepository;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.loqal.catalog.entity.Product;
import com.loqal.catalog.repository.ProductRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

/**
 * End-to-end saga test (PRD §7.3): order creation → stock reservation via
 * Kafka → confirmation. Uses real Postgres and Kafka via Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderSagaIT {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    static final org.testcontainers.containers.PostgreSQLContainer<?> POSTGRES =
            new org.testcontainers.containers.PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("loqaldb");

    static final org.testcontainers.containers.KafkaContainer KAFKA =
            new org.testcontainers.containers.KafkaContainer(
                    org.testcontainers.utility.DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @BeforeAll
    static void startContainers() {
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost()
                + ":" + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.flyway.url", () -> "jdbc:postgresql://" + POSTGRES.getHost()
                + ":" + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);

        // RSA keys for the identity module (throwaway pair, generated per run)
        var keys = TestKeys.generate();
        registry.add("jwt.rsa.private-key", () -> keys.privateKeyBase64());
        registry.add("jwt.rsa.public-key", () -> keys.publicKeyBase64());
        registry.add("razorpay.key-id", () -> "test");
        registry.add("razorpay.key-secret", () -> "test");
        registry.add("razorpay.webhook-secret", () -> "test");
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test");
        registry.add("spring.security.oauth2.client.registration.google.redirect-uri", () -> "http://localhost/callback");
    }

    private Product seedProduct(int quantity) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.markNew();
        product.setName("saga-product-" + UUID.randomUUID());
        product.setPriceMinor(1999L);
        product.setQuantity(quantity);
        return productRepository.save(product).block(Duration.ofSeconds(10));
    }

    @Test
    void stockReservationConfirmsPaidOrder() throws Exception {
        Product product = seedProduct(10);

        // 1. Create a paid order awaiting stock reservation
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        order.setCurrentStatus(com.loqal.orders.dto.OrderStatus.ORDER_PAYMENT_COMPLETE);
        order.setFinalAmountMinor(1999L);
        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setQuantity(2);
        item.setPriceAtPurchaseMinor(1999L);
        item.setOrderId(order.getId());
        order.setItems(List.of(item));
        Order saved = orderRepository.save(order).block(Duration.ofSeconds(10));

        // 2. Publish the stock reservation request the outbox would emit
        StockReservationRequest request =
                new StockReservationRequest(order.getId(), List.of(new com.loqal.contracts.events.ProductOrderRequest(product.getId(), 2)));
        kafkaTemplate.send("order-creation-requested", objectMapper.writeValueAsString(request)).get(10, java.util.concurrent.TimeUnit.SECONDS);

        // 3. Catalog must have decremented stock...
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Product updated = productRepository.findById(product.getId()).block(Duration.ofSeconds(5));
            assert updated != null;
            assert updated.getQuantity() == 8;
        });

        // ...and orders must have consumed the result and confirmed
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Order updated = orderRepository.findById(saved.getId()).block(Duration.ofSeconds(5));
            assert updated != null;
            assert updated.getCurrentStatus() == com.loqal.orders.dto.OrderStatus.ORDER_CONFIRMED;
        });
    }

    @Test
    void insufficientStockRejectsOrder() throws Exception {
        Product product = seedProduct(1);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        order.setCurrentStatus(com.loqal.orders.dto.OrderStatus.ORDER_PAYMENT_COMPLETE);
        order.setFinalAmountMinor(3998L);
        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setQuantity(5); // more than available
        item.setPriceAtPurchaseMinor(1999L);
        item.setOrderId(order.getId());
        order.setItems(List.of(item));
        Order saved = orderRepository.save(order).block(Duration.ofSeconds(10));

        StockReservationRequest request =
                new StockReservationRequest(order.getId(), List.of(new com.loqal.contracts.events.ProductOrderRequest(product.getId(), 5)));
        kafkaTemplate.send("order-creation-requested", objectMapper.writeValueAsString(request)).get(10, java.util.concurrent.TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Order updated = orderRepository.findById(saved.getId()).block(Duration.ofSeconds(5));
            assert updated != null;
            assert updated.getCurrentStatus() == com.loqal.orders.dto.OrderStatus.ORDER_REJECTED;
            // stock unchanged
            Product p = productRepository.findById(product.getId()).block(Duration.ofSeconds(5));
            assert p != null && p.getQuantity() == 1;
        });
    }
}
