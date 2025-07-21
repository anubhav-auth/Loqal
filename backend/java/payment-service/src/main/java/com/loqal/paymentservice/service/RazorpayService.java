package com.loqal.paymentservice.service;

import com.loqal.paymentservice.entity.dto.OrderRequest;
import com.loqal.paymentservice.entity.dto.PaymentResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient razorpayClient;

    /**
     * Creates a Razorpay order.
     * @param orderRequest The request containing order details.
     * @return A PaymentResponse with order details for the client.
     * @throws RazorpayException if there is an error creating the order.
     */
    public PaymentResponse createOrder(OrderRequest orderRequest) throws RazorpayException {
        if (razorpayClient == null) {
            razorpayClient = new RazorpayClient(keyId, keySecret);
        }

        JSONObject orderRequestJson = new JSONObject();
        orderRequestJson.put("amount", orderRequest.getAmount() * 100); // amount in the smallest currency unit
        orderRequestJson.put("currency", orderRequest.getCurrency());
        orderRequestJson.put("receipt", orderRequest.getReceipt());

        Order order = razorpayClient.orders.create(orderRequestJson);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setOrderId(order.get("id"));
        paymentResponse.setRazorpayKeyId(keyId);
        paymentResponse.setAmount(order.get("amount"));
        paymentResponse.setCurrency(order.get("currency"));

        return paymentResponse;
    }

    /**
     * Verifies the payment signature.
     * @param orderId The Razorpay order ID.
     * @param paymentId The Razorpay payment ID.
     * @param signature The signature from Razorpay.
     * @return true if the signature is valid, false otherwise.
     */
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            // Log the exception
            e.printStackTrace();
            return false;
        }
    }
}
