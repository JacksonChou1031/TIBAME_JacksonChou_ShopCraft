package com.jackson.ecommerce.product.repository;

import com.jackson.ecommerce.product.domain.Product;
import com.jackson.ecommerce.product.domain.ProductImage;
import com.jackson.ecommerce.product.domain.ProductStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProductRepository {
    private static final String PRODUCT_COLUMNS = """
            SELECT product_id, seller_id, name, description, price, currency, stock,
                   category, status, deleted_at
              FROM products
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(long sellerId, String name, String description, BigDecimal price,
                       int stock, String category) {
        String sql = """
                INSERT INTO products (seller_id, name, description, price, stock, category)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, sellerId);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setBigDecimal(4, price);
            statement.setInt(5, stock);
            statement.setString(6, category);
            return statement;
        }, keyHolder);
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = keys.get("PRODUCT_ID");
        if (key == null) {
            key = keys.get("product_id");
        }
        if (!(key instanceof Number productId)) {
            throw new IllegalStateException("Product id was not returned after insert");
        }
        return productId.longValue();
    }

    public int update(long productId, long sellerId, String name, String description,
                      BigDecimal price, int stock, String category) {
        return jdbcTemplate.update("""
                        UPDATE products
                           SET name = ?, description = ?, price = ?, stock = ?, category = ?,
                               updated_at = CURRENT_TIMESTAMP
                         WHERE product_id = ? AND seller_id = ? AND deleted_at IS NULL
                        """,
                name, description, price, stock, category, productId, sellerId);
    }

    public int softDelete(long productId, long sellerId) {
        return jdbcTemplate.update("""
                        UPDATE products SET deleted_at = CURRENT_TIMESTAMP, status = 'UNPUBLISHED',
                               updated_at = CURRENT_TIMESTAMP
                         WHERE product_id = ? AND seller_id = ? AND deleted_at IS NULL
                        """, productId, sellerId);
    }

    public int softDeleteById(long productId) {
        return jdbcTemplate.update("""
                        UPDATE products SET deleted_at = CURRENT_TIMESTAMP, status = 'UNPUBLISHED',
                               updated_at = CURRENT_TIMESTAMP
                         WHERE product_id = ? AND deleted_at IS NULL
                        """, productId);
    }

    public Optional<Product> findById(long productId) {
        return queryProducts(PRODUCT_COLUMNS + " WHERE product_id = ?", productId).stream().findFirst();
    }

    public Optional<Product> findPublicById(long productId) {
        return queryProducts(PRODUCT_COLUMNS + " WHERE product_id = ? AND status = 'PUBLISHED' AND deleted_at IS NULL",
                productId).stream().findFirst();
    }

    public ProductPage findPublic(String keyword, String category, String sort, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE status = 'PUBLISHED' AND deleted_at IS NULL");
        List<Object> args = new ArrayList<>();
        addSearch(where, args, keyword, category);
        String orderBy = switch (sort) {
            case "price_asc" -> " ORDER BY price ASC, product_id DESC";
            case "price_desc" -> " ORDER BY price DESC, product_id DESC";
            default -> " ORDER BY created_at DESC, product_id DESC";
        };
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products" + where, Long.class, args.toArray());
        String sql = PRODUCT_COLUMNS + where + orderBy + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        args.add((page - 1) * size);
        args.add(size);
        return new ProductPage(queryProducts(sql, args.toArray()), total);
    }

    public ProductPage findBySeller(long sellerId, int page, int size) {
        String where = " WHERE seller_id = ?";
        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products" + where, Long.class, sellerId);
        List<Product> products = queryProducts(
                PRODUCT_COLUMNS + where + " ORDER BY created_at DESC, product_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                sellerId, (page - 1) * size, size);
        return new ProductPage(products, total);
    }

    public ProductPage findAll(int page, int size) {
        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        List<Product> products = queryProducts(
                PRODUCT_COLUMNS + " ORDER BY created_at DESC, product_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                (page - 1) * size, size);
        return new ProductPage(products, total);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        return count == null ? 0 : count;
    }

    public long insertImage(long productId, String storageKey, String originalFilename,
                            String mediaType, long fileSize, int sortOrder) {
        String sql = """
                INSERT INTO product_images
                    (product_id, storage_key, original_filename, media_type, file_size, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, productId);
            statement.setString(2, storageKey);
            statement.setString(3, originalFilename);
            statement.setString(4, mediaType);
            statement.setLong(5, fileSize);
            statement.setInt(6, sortOrder);
            return statement;
        }, keyHolder);
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = keys.get("IMAGE_ID");
        if (key == null) {
            key = keys.get("image_id");
        }
        if (!(key instanceof Number imageId)) {
            throw new IllegalStateException("Image id was not returned after insert");
        }
        return imageId.longValue();
    }

    public List<ProductImage> findImages(long productId) {
        return jdbcTemplate.query("""
                        SELECT image_id, product_id, storage_key, original_filename, media_type, file_size, sort_order
                          FROM product_images WHERE product_id = ? ORDER BY sort_order, image_id
                        """,
                (rs, rowNum) -> new ProductImage(
                        rs.getLong("image_id"), rs.getLong("product_id"), rs.getString("storage_key"),
                        rs.getString("original_filename"), rs.getString("media_type"),
                        rs.getLong("file_size"), rs.getInt("sort_order")),
                productId);
    }

    public Optional<ProductImage> findImage(long productId, long imageId) {
        return jdbcTemplate.query("""
                        SELECT image_id, product_id, storage_key, original_filename, media_type, file_size, sort_order
                          FROM product_images WHERE product_id = ? AND image_id = ?
                        """,
                (rs, rowNum) -> new ProductImage(
                        rs.getLong("image_id"), rs.getLong("product_id"), rs.getString("storage_key"),
                        rs.getString("original_filename"), rs.getString("media_type"),
                        rs.getLong("file_size"), rs.getInt("sort_order")),
                productId, imageId).stream().findFirst();
    }

    public int countImages(long productId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_images WHERE product_id = ?", Integer.class,
                productId);
    }

    public int deleteImage(long productId, long imageId) {
        return jdbcTemplate.update("DELETE FROM product_images WHERE product_id = ? AND image_id = ?", productId, imageId);
    }

    private List<Product> queryProducts(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Product(
                rs.getLong("product_id"),
                rs.getLong("seller_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getString("currency"),
                rs.getInt("stock"),
                rs.getString("category"),
                ProductStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("deleted_at") != null,
                List.of()), args).stream().map(product -> new Product(
                product.id(), product.sellerId(), product.name(), product.description(), product.price(),
                product.currency(), product.stock(), product.category(), product.status(), product.deleted(),
                findImages(product.id()))).toList();
    }

    private void addSearch(StringBuilder where, List<Object> args, String keyword, String category) {
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(name) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?))");
            String value = "%" + keyword.trim() + "%";
            args.add(value);
            args.add(value);
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND category = ?");
            args.add(category.trim());
        }
    }

    public record ProductPage(List<Product> items, long totalItems) {
    }
}
