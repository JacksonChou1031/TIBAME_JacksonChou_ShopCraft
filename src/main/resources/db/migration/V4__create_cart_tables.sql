CREATE TABLE carts (
    cart_id BIGINT IDENTITY(1,1) NOT NULL,
    member_id BIGINT NOT NULL,
    seller_id BIGINT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_carts_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_carts_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_carts PRIMARY KEY (cart_id),
    CONSTRAINT uq_carts_member UNIQUE (member_id),
    CONSTRAINT fk_carts_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT fk_carts_seller FOREIGN KEY (seller_id) REFERENCES members(member_id)
);

CREATE TABLE cart_items (
    cart_item_id BIGINT IDENTITY(1,1) NOT NULL,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_cart_items_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_cart_items_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_cart_items PRIMARY KEY (cart_item_id),
    CONSTRAINT uq_cart_items_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(cart_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(product_id),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_cart_items_cart ON cart_items (cart_id, product_id);
