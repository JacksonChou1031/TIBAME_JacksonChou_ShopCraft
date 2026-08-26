package com.jackson.ecommerce.cart.repository;

import com.jackson.ecommerce.cart.domain.CartItem;
import com.jackson.ecommerce.product.domain.ProductStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CartRepository {
    private final JdbcTemplate jdbcTemplate;

    public CartRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CartRow> findCart(long memberId) {
        return jdbcTemplate.query("""
                        SELECT cart_id, member_id, seller_id
                          FROM carts WHERE member_id = ?
                        """,
                (rs, rowNum) -> new CartRow(rs.getLong("cart_id"), rs.getLong("member_id"),
                        rs.getObject("seller_id", Long.class)), memberId).stream().findFirst();
    }

    public long insertCart(long memberId, long sellerId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO carts (member_id, seller_id) VALUES (?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, memberId);
            statement.setLong(2, sellerId);
            return statement;
        }, keyHolder);
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = keys.get("CART_ID");
        if (key == null) {
            key = keys.get("cart_id");
        }
        if (!(key instanceof Number cartId)) {
            throw new IllegalStateException("Cart id was not returned after insert");
        }
        return cartId.longValue();
    }

    public int updateSeller(long cartId, Long sellerId) {
        return jdbcTemplate.update("""
                        UPDATE carts SET seller_id = ?, updated_at = CURRENT_TIMESTAMP WHERE cart_id = ?
                        """, sellerId, cartId);
    }

    public List<CartItem> findItems(long memberId) {
        return jdbcTemplate.query("""
                        SELECT ci.cart_item_id, ci.product_id, p.seller_id, p.name, p.price, p.currency,
                               ci.quantity, p.stock, p.status, p.deleted_at
                          FROM carts c
                          JOIN cart_items ci ON ci.cart_id = c.cart_id
                          JOIN products p ON p.product_id = ci.product_id
                         WHERE c.member_id = ?
                         ORDER BY ci.created_at, ci.cart_item_id
                        """,
                (rs, rowNum) -> new CartItem(
                        rs.getLong("cart_item_id"), rs.getLong("product_id"), rs.getLong("seller_id"),
                        rs.getString("name"), rs.getBigDecimal("price"), rs.getString("currency"),
                        rs.getInt("quantity"), rs.getInt("stock"),
                        ProductStatus.valueOf(rs.getString("status")), rs.getTimestamp("deleted_at") != null),
                memberId);
    }

    public Optional<ItemRow> findItem(long cartId, long productId) {
        return jdbcTemplate.query("""
                        SELECT cart_item_id, quantity FROM cart_items
                         WHERE cart_id = ? AND product_id = ?
                        """, (rs, rowNum) -> new ItemRow(rs.getLong("cart_item_id"), rs.getInt("quantity")),
                cartId, productId).stream().findFirst();
    }

    public void insertItem(long cartId, long productId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)
                """, cartId, productId, quantity);
    }

    public int updateItem(long cartId, long productId, int quantity) {
        return jdbcTemplate.update("""
                UPDATE cart_items SET quantity = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE cart_id = ? AND product_id = ?
                """, quantity, cartId, productId);
    }

    public int deleteItem(long cartId, long productId) {
        return jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?", cartId, productId);
    }

    public int deleteAllItems(long cartId) {
        return jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
    }

    public int countItems(long cartId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", Integer.class, cartId);
    }

    public record CartRow(long id, long memberId, Long sellerId) {
    }

    public record ItemRow(long id, int quantity) {
    }
}
