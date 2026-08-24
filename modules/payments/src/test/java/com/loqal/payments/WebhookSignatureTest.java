package com.loqal.payments;

import com.loqal.payments.gateway.RazorpayGateway;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureTest {

    private static final String SECRET = "test-secret";

    private String hmac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    @Test
    void validSignaturePasses() throws Exception {
        RazorpayGateway gateway = new RazorpayGateway("key", "secret", SECRET);
        String payload = "{\"event\":\"payment.captured\"}";
        assertTrue(gateway.verifyWebhookSignature(payload, hmac(payload)));
    }

    @Test
    void tamperedPayloadFails() throws Exception {
        RazorpayGateway gateway = new RazorpayGateway("key", "secret", SECRET);
        String payload = "{\"event\":\"payment.captured\"}";
        assertFalse(gateway.verifyWebhookSignature(payload + "x", hmac(payload)));
    }

    @Test
    void wrongSecretFails() throws Exception {
        RazorpayGateway gateway = new RazorpayGateway("key", "secret", "other-secret");
        String payload = "{}";
        assertFalse(gateway.verifyWebhookSignature(payload, hmac(payload)));
    }

    @Test
    void nullInputsFail() {
        RazorpayGateway gateway = new RazorpayGateway("key", "secret", SECRET);
        assertFalse(gateway.verifyWebhookSignature(null, "sig"));
        assertFalse(gateway.verifyWebhookSignature("{}", null));
    }
}
