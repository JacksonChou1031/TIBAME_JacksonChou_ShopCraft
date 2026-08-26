CREATE TABLE orders (
    order_id BIGINT IDENTITY(1,1) NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    subtotal_amount DECIMAL(19,2) NOT NULL,
    shipping_fee DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    shipping_method VARCHAR(30) NOT NULL,
    recipient_name NVARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    store_name NVARCHAR(100) NULL,
    store_code VARCHAR(30) NULL,
    delivery_address NVARCHAR(300) NULL,
    status VARCHAR(30) NOT NULL CONSTRAINT df_orders_status DEFAULT 'PAID',
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_orders_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_orders_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES members(member_id),
    CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES members(member_id),
    CONSTRAINT ck_orders_amounts_non_negative CHECK (subtotal_amount >= 0 AND shipping_fee >= 0 AND total_amount >= 0),
    CONSTRAINT ck_orders_shipping_method CHECK (shipping_method IN ('CONVENIENCE_STORE', 'HOME_DELIVERY')),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PENDING_SHIPMENT', 'SHIPPED', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE order_items (
    order_item_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_name NVARCHAR(200) NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(19,2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (order_item_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(product_id),
    CONSTRAINT fk_order_items_seller FOREIGN KEY (seller_id) REFERENCES members(member_id),
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_amounts_non_negative CHECK (unit_price >= 0 AND line_amount >= 0)
);

CREATE TABLE payment_attempts (
    payment_attempt_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NULL,
    member_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    mock_account_name NVARCHAR(100) NOT NULL,
    mock_account_number VARCHAR(100) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    failure_reason NVARCHAR(255) NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_payment_attempts_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_payment_attempts PRIMARY KEY (payment_attempt_id),
    CONSTRAINT fk_payment_attempts_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_payment_attempts_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT ck_payment_attempts_status CHECK (payment_status IN ('SUCCESS', 'FAILED'))
);

CREATE TABLE checkout_requests (
    checkout_request_id BIGINT IDENTITY(1,1) NOT NULL,
    member_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    result_status VARCHAR(20) NOT NULL,
    order_id BIGINT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_checkout_requests_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_checkout_requests PRIMARY KEY (checkout_request_id),
    CONSTRAINT uq_checkout_requests_member_key UNIQUE (member_id, idempotency_key),
    CONSTRAINT fk_checkout_requests_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT fk_checkout_requests_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT ck_checkout_requests_status CHECK (result_status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX ix_orders_buyer ON orders (buyer_id, created_at DESC, order_id DESC);
CREATE INDEX ix_orders_seller ON orders (seller_id, created_at DESC, order_id DESC);
CREATE INDEX ix_order_items_order ON order_items (order_id, order_item_id);
CREATE INDEX ix_payment_attempts_member ON payment_attempts (member_id, created_at DESC);
