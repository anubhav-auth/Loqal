package com.loqal.app.config;

import com.loqal.contracts.dto.ErrorResponse;
import com.loqal.contracts.exception.InsufficientStockException;
import com.loqal.contracts.exception.InvalidCouponException;
import com.loqal.contracts.exception.InvalidOrderStatusException;
import com.loqal.catalog.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(mock(RequestPath.class));
        when(request.getPath().value()).thenReturn("/test/path");
        when(request.getMethod()).thenReturn(HttpMethod.GET);
    }

    @Test
    void handleValidation_returns400_withFieldErrorMessage() {
        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);

        when(fieldError.getDefaultMessage()).thenReturn("must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("must not be blank", response.getBody().message());
        assertEquals("/test/path", response.getBody().path());
    }

    @Test
    void handleValidation_emptyFieldErrors_fallsBackToDefaultMessage() {
        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(ex.getFieldError()).thenReturn(null);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().message());
    }

    @Test
    void handleInvalidCoupon_returns422() {
        InvalidCouponException ex = new InvalidCouponException("Coupon EXPIRED is no longer valid");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCoupon(ex, exchange);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(422, response.getBody().status());
        assertEquals("Unprocessable Entity", response.getBody().error());
        assertEquals("Coupon EXPIRED is no longer valid", response.getBody().message());
    }

    @Test
    void handleInsufficientStock_returns422() {
        InsufficientStockException ex = new InsufficientStockException("Only 3 items left in stock");

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientStock(ex, exchange);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(422, response.getBody().status());
        assertEquals("Only 3 items left in stock", response.getBody().message());
    }

    @Test
    void handleInvalidStatus_returns409() {
        InvalidOrderStatusException ex = new InvalidOrderStatusException("Cannot cancel delivered order");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidStatus(ex, exchange);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Cannot cancel delivered order", response.getBody().message());
    }

    @Test
    void handleProductNotFound_returns404() {
        UUID productId = UUID.randomUUID();
        ProductNotFoundException ex = new ProductNotFoundException(productId);

        ResponseEntity<ErrorResponse> response = handler.handleProductNotFound(ex, exchange);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Product not found: " + productId, response.getBody().message());
    }

    @Test
    void handleResponseStatus_mapsStatusCode() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(ex, exchange);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Unauthorized", response.getBody().message());
    }

    @Test
    void handleResponseStatus_nullReason_usesDefaultMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, null);

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(ex, exchange);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Request failed", response.getBody().message());
    }

    @Test
    void handleUnexpected_returns500() {
        Exception ex = new RuntimeException("something broke");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex, exchange);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }

    @Test
    void allResponsesContainTimestamp() {
        InvalidCouponException ex = new InvalidCouponException("bad coupon");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCoupon(ex, exchange);

        assertNotNull(response.getBody().timestamp());
        // timestamp should be parseable as ISO-8601
        assertDoesNotThrow(() -> java.time.Instant.parse(response.getBody().timestamp()));
    }
}
