package com.jackson.ecommerce.order.service;

import com.jackson.ecommerce.cart.domain.CartItem;
import com.jackson.ecommerce.cart.repository.CartRepository;
import com.jackson.ecommerce.common.web.BadRequestException;
import com.jackson.ecommerce.common.web.ConflictException;
import com.jackson.ecommerce.member.service.MemberService;
import com.jackson.ecommerce.order.api.CheckoutRequest;
import com.jackson.ecommerce.order.api.CheckoutResponse;
import com.jackson.ecommerce.order.domain.ShippingMethod;
import com.jackson.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CheckoutService {
    public static final String MOCK_SUCCESS = "MOCK_SUCCESS";
    public static final String MOCK_FAILURE = "MOCK_FAILURE";

    private final MemberService memberService;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CheckoutTransactionService transactionService;
    private final CheckoutFailureRecorder failureRecorder;
    private final ConcurrentHashMap<Long, Object> checkoutLocks = new ConcurrentHashMap<>();

    public CheckoutService(MemberService memberService, CartRepository cartRepository, OrderRepository orderRepository,
                           CheckoutTransactionService transactionService, CheckoutFailureRecorder failureRecorder) {
        this.memberService = memberService;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.transactionService = transactionService;
        this.failureRecorder = failureRecorder;
    }

    public CheckoutResult checkout(long memberId, String idempotencyKey, CheckoutRequest request) {
        memberService.requireActive(memberId);
        String key = normalizeKey(idempotencyKey);
        ShippingMethod shippingMethod = validateRequest(request);
        Object checkoutLock = checkoutLocks.computeIfAbsent(memberId, ignored -> new Object());
        synchronized (checkoutLock) {
            return checkoutLocked(memberId, key, request, shippingMethod);
        }
    }

    private CheckoutResult checkoutLocked(long memberId, String key, CheckoutRequest request,
                                          ShippingMethod shippingMethod) {
        OrderRepository.CheckoutRequestRow existing = orderRepository.findCheckoutRequest(memberId, key).orElse(null);
        if (existing != null) {
            if ("FAILED".equals(existing.status())) {
                throw new ConflictException("Idempotency key has already been used for a failed payment");
            }
            return replay(existing, key);
        }

        List<CartItem> items = cartRepository.findItems(memberId);
        if (items.isEmpty()) {
            throw new ConflictException("Cart is empty");
        }
        if (items.stream().anyMatch(item -> !item.purchasable())) {
            throw new ConflictException("Cart contains an unavailable product");
        }
        if (MOCK_FAILURE.equals(request.mockAccountNumber().trim())) {
            failureRecorder.record(memberId, key, request, "Mock payment was declined");
            throw new PaymentFailedException("Mock payment was declined");
        }
        if (!MOCK_SUCCESS.equals(request.mockAccountNumber().trim())) {
            throw new BadRequestException("mockAccountNumber must be MOCK_SUCCESS or MOCK_FAILURE");
        }
        try {
            CheckoutResponse response = transactionService.create(memberId, key, request, shippingMethod);
            return new CheckoutResult(response, false);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent request with the same key may win the unique-key race after our initial lookup.
            OrderRepository.CheckoutRequestRow concurrent = orderRepository.findCheckoutRequest(memberId, key)
                    .orElse(null);
            if (concurrent != null && "SUCCESS".equals(concurrent.status())) {
                return replay(concurrent, key);
            }
            throw exception;
        }
    }

    private ShippingMethod validateRequest(CheckoutRequest request) {
        ShippingMethod method = ShippingMethod.from(request.shippingMethod());
        if (method == null) {
            throw new BadRequestException("shippingMethod is required");
        }
        if (method == ShippingMethod.CONVENIENCE_STORE) {
            if (isBlank(request.storeName()) || isBlank(request.storeCode())) {
                throw new BadRequestException("Convenience store delivery requires storeName and storeCode");
            }
        } else if (isBlank(request.deliveryAddress())) {
            throw new BadRequestException("Home delivery requires deliveryAddress");
        }
        return method;
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 100) {
            throw new BadRequestException("Idempotency-Key header is required and must be at most 100 characters");
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CheckoutResult replay(OrderRepository.CheckoutRequestRow request, String key) {
        OrderRepository.CheckoutSummary summary = orderRepository.findSummary(request.orderId());
        return new CheckoutResult(new CheckoutResponse(summary.orderId(), summary.status().name(), "SUCCESS",
                summary.shippingMethod().name(), summary.subtotal(), summary.shippingFee(), summary.total(),
                key, true, "Checkout request was already completed"), true);
    }

    public record CheckoutResult(CheckoutResponse response, boolean replayed) {
    }
}
