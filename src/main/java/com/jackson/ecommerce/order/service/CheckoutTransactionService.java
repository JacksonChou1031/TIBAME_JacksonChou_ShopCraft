package com.jackson.ecommerce.order.service;

import com.jackson.ecommerce.cart.domain.CartItem;
import com.jackson.ecommerce.cart.repository.CartRepository;
import com.jackson.ecommerce.common.web.ConflictException;
import com.jackson.ecommerce.order.api.CheckoutRequest;
import com.jackson.ecommerce.order.api.CheckoutResponse;
import com.jackson.ecommerce.order.domain.ShippingMethod;
import com.jackson.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutTransactionService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public CheckoutTransactionService(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    // Stock safety comes from the conditional atomic UPDATE in OrderRepository.decreaseStock().
    // READ_COMMITTED avoids serializable deadlocks when different buyers checkout the same product.
    @Transactional
    public CheckoutResponse create(long memberId, String idempotencyKey, CheckoutRequest request,
                                   ShippingMethod shippingMethod) {
        if (orderRepository.findCheckoutRequest(memberId, idempotencyKey).isPresent()) {
            throw new ConflictException("Idempotency key has already been used");
        }
        CartRepository.CartRow cart = cartRepository.findCart(memberId)
                .orElseThrow(() -> new ConflictException("Cart is empty"));
        List<CartItem> items = cartRepository.findItems(memberId);
        if (items.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }
        if (items.stream().anyMatch(item -> !item.purchasable())) {
            throw new ConflictException("Cart contains an unavailable product");
        }
        long sellerId = items.get(0).sellerId();
        BigDecimal subtotal = items.stream()
                .map(item -> item.currentUnitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.add(shippingMethod.fee());
        long orderId = orderRepository.insertOrder(memberId, sellerId, subtotal, shippingMethod.fee(), total,
                shippingMethod, request.recipientName().trim(), request.recipientPhone().trim(),
                blankToNull(request.storeName()), blankToNull(request.storeCode()), blankToNull(request.deliveryAddress()));
        orderRepository.insertStatusHistory(orderId, com.jackson.ecommerce.order.domain.OrderStatus.PENDING_PAYMENT, memberId);
        if (orderRepository.updateStatus(orderId, com.jackson.ecommerce.order.domain.OrderStatus.PENDING_PAYMENT,
                com.jackson.ecommerce.order.domain.OrderStatus.PAID) != 1) {
            throw new ConflictException("Order could not be marked as paid");
        }
        orderRepository.insertStatusHistory(orderId, com.jackson.ecommerce.order.domain.OrderStatus.PAID, memberId);
        for (CartItem item : items) {
            if (orderRepository.decreaseStock(item.productId(), item.quantity()) != 1) {
                throw new ConflictException("Stock is no longer sufficient for product " + item.productId());
            }
            orderRepository.insertItem(orderId, item);
        }
        orderRepository.insertPayment(orderId, memberId, idempotencyKey, request.mockAccountName().trim(),
                request.mockAccountNumber().trim(), "SUCCESS", null);
        orderRepository.insertCheckoutRequest(memberId, idempotencyKey, "SUCCESS", orderId);
        cartRepository.deleteAllItems(cart.id());
        cartRepository.updateSeller(cart.id(), null);
        return response(orderRepository.findSummary(orderId), idempotencyKey, false, "Payment succeeded");
    }

    private CheckoutResponse response(OrderRepository.CheckoutSummary summary, String idempotencyKey,
                                      boolean replayed, String message) {
        return new CheckoutResponse(summary.orderId(), summary.status().name(), "SUCCESS",
                summary.shippingMethod().name(), summary.subtotal(), summary.shippingFee(), summary.total(),
                idempotencyKey, replayed, message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
