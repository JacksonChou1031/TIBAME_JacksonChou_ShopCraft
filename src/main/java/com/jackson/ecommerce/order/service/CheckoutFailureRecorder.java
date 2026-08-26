package com.jackson.ecommerce.order.service;

import com.jackson.ecommerce.order.api.CheckoutRequest;
import com.jackson.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutFailureRecorder {
    private final OrderRepository orderRepository;

    public CheckoutFailureRecorder(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(long memberId, String idempotencyKey, CheckoutRequest request, String reason) {
        orderRepository.insertPayment(null, memberId, idempotencyKey, request.mockAccountName().trim(),
                request.mockAccountNumber().trim(), "FAILED", reason);
        orderRepository.insertCheckoutRequest(memberId, idempotencyKey, "FAILED", null);
    }
}
