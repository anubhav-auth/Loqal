package com.loqal.catalog.promotions.controller;

import com.loqal.catalog.promotions.PromotionService;
import com.loqal.catalog.promotions.entity.Coupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionControllerTest {

    @Mock
    private PromotionService promotionService;

    private PromotionController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID COUPON_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new PromotionController(promotionService);
    }

    private Jwt mockJwt() {
        Jwt jwt = org.mockito.Mockito.mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn(TENANT_ID.toString());
        return jwt;
    }

    private Coupon sampleCoupon() {
        Coupon c = new Coupon();
        c.setId(COUPON_ID);
        c.setTenantId(TENANT_ID);
        c.setCode("TEST10");
        c.setDiscountType(Coupon.TYPE_PERCENT);
        c.setValue(1000);
        c.setMinOrderValueMinor(50000);
        c.setMaxDiscountMinor(5000L);
        c.setValidFrom(LocalDateTime.now().minusDays(1));
        c.setValidUntil(LocalDateTime.now().plusDays(30));
        c.setActive(true);
        c.setTimesUsed(0);
        c.markNew();
        return c;
    }

    @Test
    void createCoupon_success_returns201() {
        Coupon coupon = sampleCoupon();
        when(promotionService.createCoupon(eq(TENANT_ID), any(Coupon.class)))
                .thenReturn(Mono.just(coupon));

        StepVerifier.create(controller.createCoupon(new Coupon(), mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().value()).isEqualTo(201);
                    assertThat(r.getBody().getCode()).isEqualTo("TEST10");
                })
                .verifyComplete();
    }

    @Test
    void createCoupon_duplicateCode_returns400() {
        when(promotionService.createCoupon(eq(TENANT_ID), any(Coupon.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Coupon code already exists")));

        StepVerifier.create(controller.createCoupon(new Coupon(), mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(400))
                .verifyComplete();
    }

    @Test
    void listCoupons_returnsFlux() {
        Coupon c1 = sampleCoupon();
        Coupon c2 = sampleCoupon();
        c2.setId(UUID.randomUUID());
        c2.setCode("SAVE20");
        when(promotionService.listCoupons(TENANT_ID)).thenReturn(Flux.just(c1, c2));

        StepVerifier.create(controller.listCoupons(mockJwt()))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void deactivateCoupon_success_returns200() {
        Coupon coupon = sampleCoupon();
        coupon.setActive(false);
        when(promotionService.deactivate(eq(TENANT_ID), eq(COUPON_ID)))
                .thenReturn(Mono.just(coupon));

        StepVerifier.create(controller.deactivateCoupon(COUPON_ID, mockJwt()))
                .assertNext(r -> {
                    assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(r.getBody().isActive()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void deactivateCoupon_notFound_returns404() {
        UUID unknownId = UUID.randomUUID();
        when(promotionService.deactivate(eq(TENANT_ID), eq(unknownId)))
                .thenReturn(Mono.error(new IllegalArgumentException("Coupon not found")));

        StepVerifier.create(controller.deactivateCoupon(unknownId, mockJwt()))
                .assertNext(r -> assertThat(r.getStatusCode().value()).isEqualTo(404))
                .verifyComplete();
    }
}
