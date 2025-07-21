package com.loqal.paymentservice.controller;

import com.loqal.paymentservice.entity.dto.OrderRequest;
import com.loqal.paymentservice.entity.dto.PaymentResponse;
import com.loqal.paymentservice.service.RazorpayService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class RazorpayController {

    @Autowired
    private RazorpayService razorpayService;

    /**
     * Creates a Razorpay order.
     *
     * @param orderRequest The request containing order details.
     * @return A response entity with the order details.
     * @throws RazorpayException if there is an error creating the order.
     */
    @PostMapping("/order")
    public ResponseEntity<PaymentResponse> createOrder(@RequestBody OrderRequest orderRequest) throws RazorpayException {
        PaymentResponse paymentResponse = razorpayService.createOrder(orderRequest);
        return ResponseEntity.ok(paymentResponse);
    }

    /**
     * Verifies the payment signature.
     *
     * @param payload A map containing the payment verification details from Razorpay.
     * @return A response entity with a success or failure message.
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody Map<String, String> payload) {
        boolean isSignatureValid = razorpayService.verifyPayment(
                payload.get("razorpay_order_id"),
                payload.get("razorpay_payment_id"),
                payload.get("razorpay_signature")
        );

        if (isSignatureValid) {
            // Here you would typically update your order status in your database
            return ResponseEntity.ok("Payment verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Payment verification failed");
        }
    }
}