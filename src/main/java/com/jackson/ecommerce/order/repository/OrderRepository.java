package com.jackson.ecommerce.order.repository;

import com.jackson.ecommerce.order.domain.OrderStatus;
import com.jackson.ecommerce.order.domain.ShippingMethod;
import com.jackson.ecommerce.cart.domain.CartItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CheckoutRequestRow> findCheckoutRequest(long memberId, String idempotencyKey) {
        return jdbcTemplate.query("""
                        SELECT checkout_request_id, result_status, order_id
                          FROM checkout_requests
                         WHERE member_id = ? AND idempotency_key = ?
                        """,
                (rs, rowNum) -> new CheckoutRequestRow(rs.getLong("checkout_request_id"),
                        rs.getString("result_status"), rs.getObject("order_id", Long.class)),
                memberId, idempotencyKey).stream().findFirst();
    }

    public long insertOrder(long buyerId, long sellerId, BigDecimal subtotal, BigDecimal shippingFee,
                            BigDecimal total, ShippingMethod shippingMethod, String recipientName,
                            String recipientPhone, String storeName, String storeCode, String address) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO orders
                        (buyer_id, seller_id, subtotal_amount, shipping_fee, total_amount, shipping_method,
                         recipient_name, recipient_phone, store_name, store_code, delivery_address, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PAID')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, buyerId);
            statement.setLong(2, sellerId);
            statement.setBigDecimal(3, subtotal);
            statement.setBigDecimal(4, shippingFee);
            statement.setBigDecimal(5, total);
            statement.setString(6, shippingMethod.name());
            statement.setString(7, recipientName);
            statement.setString(8, recipientPhone);
            statement.setString(9, storeName);
            statement.setString(10, storeCode);
            statement.setString(11, address);
            return statement;
        }, keyHolder);
        return generatedId(keyHolder, "ORDER_ID", "order_id", "Order id was not returned after insert");
    }

    public void insertItem(long orderId, CartItem item) {
        BigDecimal lineAmount = item.currentUnitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        jdbcTemplate.update("""
                INSERT INTO order_items
                    (order_id, product_id, seller_id, product_name, unit_price, quantity, line_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, orderId, item.productId(), item.sellerId(), item.productName(), item.currentUnitPrice(),
                item.quantity(), lineAmount);
    }

    public int decreaseStock(long productId, int quantity) {
        return jdbcTemplate.update("""
                UPDATE products
                   SET stock = stock - ?, updated_at = CURRENT_TIMESTAMP
                 WHERE product_id = ? AND status = 'PUBLISHED' AND deleted_at IS NULL AND stock >= ?
                """, quantity, productId, quantity);
    }

    public void insertPayment(Long orderId, long memberId, String idempotencyKey, String accountName,
                              String accountNumber, String status, String failureReason) {
        jdbcTemplate.update("""
                INSERT INTO payment_attempts
                    (order_id, member_id, idempotency_key, mock_account_name, mock_account_number,
                     payment_status, failure_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, orderId, memberId, idempotencyKey, accountName, accountNumber, status, failureReason);
    }

    public void insertCheckoutRequest(long memberId, String idempotencyKey, String status, Long orderId) {
        jdbcTemplate.update("""
                INSERT INTO checkout_requests (member_id, idempotency_key, result_status, order_id)
                VALUES (?, ?, ?, ?)
                """, memberId, idempotencyKey, status, orderId);
    }

    public CheckoutSummary findSummary(long orderId) {
        return jdbcTemplate.queryForObject("""
                        SELECT order_id, status, shipping_method, subtotal_amount, shipping_fee, total_amount
                          FROM orders WHERE order_id = ?
                        """, (rs, rowNum) -> new CheckoutSummary(
                        rs.getLong("order_id"), OrderStatus.valueOf(rs.getString("status")),
                        ShippingMethod.valueOf(rs.getString("shipping_method")),
                        rs.getBigDecimal("subtotal_amount"), rs.getBigDecimal("shipping_fee"),
                        rs.getBigDecimal("total_amount")), orderId);
    }

    private long generatedId(KeyHolder keyHolder, String upperName, String lowerName, String errorMessage) {
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = keys.get(upperName);
        if (key == null) {
            key = keys.get(lowerName);
        }
        if (!(key instanceof Number id)) {
            throw new IllegalStateException(errorMessage);
        }
        return id.longValue();
    }

    public record CheckoutRequestRow(long id, String status, Long orderId) {
    }

    public record CheckoutSummary(long orderId, OrderStatus status, ShippingMethod shippingMethod,
                                  BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total) {
    }
}
