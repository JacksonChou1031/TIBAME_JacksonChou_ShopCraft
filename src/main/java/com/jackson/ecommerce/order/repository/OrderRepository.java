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
import java.util.List;
import java.util.Optional;
import java.time.Instant;

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
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_PAYMENT')
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

    public void insertStatusHistory(long orderId, OrderStatus status, long changedBy) {
        jdbcTemplate.update("""
                INSERT INTO order_status_history (order_id, status, changed_by)
                VALUES (?, ?, ?)
                """, orderId, status.name(), changedBy);
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

    public List<OrderRow> findByBuyer(long buyerId) {
        return findRows("o.buyer_id = ?", buyerId);
    }

    public List<OrderRow> findBySeller(long sellerId) {
        return findRows("o.seller_id = ?", sellerId);
    }

    public List<OrderRow> findAll() {
        return findRows("1 = 1");
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        return count == null ? 0 : count;
    }

    public Optional<OrderRow> findById(long orderId) {
        return findRows("o.order_id = ?", orderId).stream().findFirst();
    }

    public Optional<OrderRow> findByBuyerAndId(long buyerId, long orderId) {
        return findRows("o.buyer_id = ? AND o.order_id = ?", buyerId, orderId).stream().findFirst();
    }

    public Optional<OrderRow> findBySellerAndId(long sellerId, long orderId) {
        return findRows("o.seller_id = ? AND o.order_id = ?", sellerId, orderId).stream().findFirst();
    }

    public List<OrderItemRow> findItems(long orderId) {
        return jdbcTemplate.query("""
                        SELECT order_item_id, product_id, seller_id, product_name, unit_price, quantity, line_amount
                          FROM order_items
                         WHERE order_id = ?
                         ORDER BY order_item_id
                        """,
                (rs, rowNum) -> new OrderItemRow(rs.getLong("order_item_id"), rs.getLong("product_id"),
                        rs.getLong("seller_id"), rs.getString("product_name"), rs.getBigDecimal("unit_price"),
                        rs.getInt("quantity"), rs.getBigDecimal("line_amount")), orderId);
    }

    public List<StatusHistoryRow> findStatusHistory(long orderId) {
        return jdbcTemplate.query("""
                        SELECT status, changed_by, changed_at
                          FROM order_status_history
                         WHERE order_id = ?
                         ORDER BY changed_at, status_history_id
                        """,
                (rs, rowNum) -> new StatusHistoryRow(OrderStatus.valueOf(rs.getString("status")),
                        rs.getLong("changed_by"), timestamp(rs.getTimestamp("changed_at"))), orderId);
    }

    public int updateStatus(long orderId, OrderStatus expected, OrderStatus next) {
        return jdbcTemplate.update("""
                UPDATE orders
                   SET status = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE order_id = ? AND status = ?
                """, next.name(), orderId, expected.name());
    }

    public void insertShipment(long orderId, String trackingNumber) {
        jdbcTemplate.update("""
                INSERT INTO order_shipments (order_id, tracking_number)
                VALUES (?, ?)
                """, orderId, trackingNumber);
    }

    public int restoreStock(long productId, int quantity) {
        return jdbcTemplate.update("""
                UPDATE products
                   SET stock = stock + ?, updated_at = CURRENT_TIMESTAMP
                 WHERE product_id = ?
                """, quantity, productId);
    }

    private List<OrderRow> findRows(String condition, Object... args) {
        String sql = """
                SELECT o.order_id, o.buyer_id, o.seller_id, o.status, o.subtotal_amount,
                       o.shipping_fee, o.total_amount, o.shipping_method, o.recipient_name,
                       o.recipient_phone, o.store_name, o.store_code, o.delivery_address,
                       o.created_at, o.updated_at, s.tracking_number
                  FROM orders o
                  LEFT JOIN order_shipments s ON s.order_id = o.order_id
                 WHERE %s
                 ORDER BY o.created_at DESC, o.order_id DESC
                """.formatted(condition);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrderRow(
                rs.getLong("order_id"), rs.getLong("buyer_id"), rs.getLong("seller_id"),
                OrderStatus.valueOf(rs.getString("status")), rs.getBigDecimal("subtotal_amount"),
                rs.getBigDecimal("shipping_fee"), rs.getBigDecimal("total_amount"),
                ShippingMethod.valueOf(rs.getString("shipping_method")), rs.getString("recipient_name"),
                rs.getString("recipient_phone"), rs.getString("store_name"), rs.getString("store_code"),
                rs.getString("delivery_address"), timestamp(rs.getTimestamp("created_at")),
                timestamp(rs.getTimestamp("updated_at")), rs.getString("tracking_number")), args);
    }

    private Instant timestamp(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
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

    public record OrderRow(long orderId, long buyerId, long sellerId, OrderStatus status,
                           BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total,
                           ShippingMethod shippingMethod, String recipientName, String recipientPhone,
                           String storeName, String storeCode, String deliveryAddress,
                           Instant createdAt, Instant updatedAt, String trackingNumber) {
    }

    public record OrderItemRow(long orderItemId, long productId, long sellerId, String productName,
                               BigDecimal unitPrice, int quantity, BigDecimal lineAmount) {
    }

    public record StatusHistoryRow(OrderStatus status, long changedBy, Instant changedAt) {
    }
}
