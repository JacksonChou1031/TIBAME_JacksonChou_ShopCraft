package com.jackson.ecommerce.order.service;

import com.jackson.ecommerce.common.web.ConflictException;
import com.jackson.ecommerce.common.web.ForbiddenException;
import com.jackson.ecommerce.common.web.NotFoundException;
import com.jackson.ecommerce.order.api.OrderResponse;
import com.jackson.ecommerce.order.api.ShipmentRequest;
import com.jackson.ecommerce.order.domain.OrderStatus;
import com.jackson.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderRepository.OrderRow> listForBuyer(long buyerId) {
        return orderRepository.findByBuyer(buyerId);
    }

    @Transactional(readOnly = true)
    public List<OrderRepository.OrderRow> listForSeller(long sellerId) {
        return orderRepository.findBySeller(sellerId);
    }

    @Transactional(readOnly = true)
    public List<OrderRepository.OrderRow> listForAdmin() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public OrderResponse getForBuyer(long buyerId, long orderId) {
        OrderRepository.OrderRow order = findById(orderId);
        if (order.buyerId() != buyerId) {
            throw new ForbiddenException("You can only view your own orders");
        }
        return detail(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForSeller(long sellerId, long orderId) {
        OrderRepository.OrderRow order = findById(orderId);
        if (order.sellerId() != sellerId) {
            throw new ForbiddenException("You can only view orders for your products");
        }
        return detail(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForAdmin(long orderId) {
        return detail(findById(orderId));
    }

    @Transactional
    public void prepareShipment(long sellerId, long orderId) {
        OrderRepository.OrderRow order = sellerOrder(sellerId, orderId);
        transition(order, OrderStatus.PAID, OrderStatus.PENDING_SHIPMENT, sellerId);
    }

    @Transactional
    public OrderResponse ship(long sellerId, long orderId, ShipmentRequest request) {
        OrderRepository.OrderRow order = sellerOrder(sellerId, orderId);
        transition(order, OrderStatus.PENDING_SHIPMENT, OrderStatus.SHIPPED, sellerId);
        orderRepository.insertShipment(orderId, request.trackingNumber().trim());
        return getForSeller(sellerId, orderId);
    }

    @Transactional
    public OrderResponse confirmReceived(long buyerId, long orderId) {
        OrderRepository.OrderRow order = buyerOrder(buyerId, orderId);
        transition(order, OrderStatus.SHIPPED, OrderStatus.COMPLETED, buyerId);
        return getForBuyer(buyerId, orderId);
    }

    @Transactional
    public OrderResponse cancel(long buyerId, long orderId) {
        OrderRepository.OrderRow order = buyerOrder(buyerId, orderId);
        if (order.status() != OrderStatus.PAID && order.status() != OrderStatus.PENDING_SHIPMENT) {
            throw new ConflictException("Only paid orders waiting for shipment can be cancelled");
        }
        transition(order, order.status(), OrderStatus.CANCELLED, buyerId);
        for (OrderRepository.OrderItemRow item : orderRepository.findItems(orderId)) {
            orderRepository.restoreStock(item.productId(), item.quantity());
        }
        return getForBuyer(buyerId, orderId);
    }

    private void transition(OrderRepository.OrderRow order, OrderStatus expected, OrderStatus next, long changedBy) {
        if (order.status() != expected) {
            throw new ConflictException("Illegal order status transition from " + order.status() + " to " + next);
        }
        if (orderRepository.updateStatus(order.orderId(), expected, next) != 1) {
            throw new ConflictException("Order status was changed by another request");
        }
        orderRepository.insertStatusHistory(order.orderId(), next, changedBy);
    }

    private OrderRepository.OrderRow sellerOrder(long sellerId, long orderId) {
        return orderRepository.findBySellerAndId(sellerId, orderId)
                .orElseGet(() -> {
                    OrderRepository.OrderRow order = findById(orderId);
                    throw new ForbiddenException("You can only manage orders for your products");
                });
    }

    private OrderRepository.OrderRow buyerOrder(long buyerId, long orderId) {
        return orderRepository.findByBuyerAndId(buyerId, orderId)
                .orElseGet(() -> {
                    findById(orderId);
                    throw new ForbiddenException("You can only manage your own orders");
                });
    }

    private OrderRepository.OrderRow findById(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order was not found"));
    }

    private OrderResponse detail(OrderRepository.OrderRow order) {
        return OrderResponse.from(order, orderRepository.findItems(order.orderId()),
                orderRepository.findStatusHistory(order.orderId()));
    }
}
