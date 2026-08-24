package com.loqal.payments.config;

import com.loqal.payments.gateway.MockPaymentProvider;
import com.loqal.payments.gateway.PaymentProvider;
import com.loqal.payments.gateway.RazorpayGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Selects the active payment provider via {@code payments.provider}
 * (razorpay | mock). Default: razorpay.
 */
@Configuration
public class PaymentProviderConfig {

    @Bean
    public PaymentProvider paymentProvider(List<PaymentProvider> providers,
                                           @Value("${payments.provider:razorpay}") String selected) {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(selected))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown payments.provider '" + selected + "'. Available: "
                        + providers.stream().map(PaymentProvider::name).toList()));
    }
}
