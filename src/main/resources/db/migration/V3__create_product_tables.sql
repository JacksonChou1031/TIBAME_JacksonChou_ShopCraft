CREATE TABLE products (
    product_id BIGINT IDENTITY(1,1) NOT NULL,
    seller_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL CONSTRAINT df_products_currency DEFAULT 'TWD',
    stock INT NOT NULL CONSTRAINT df_products_stock DEFAULT 0,
    category NVARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_products_status DEFAULT 'PUBLISHED',
    deleted_at DATETIME2(6) NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_products_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_products_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_products PRIMARY KEY (product_id),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES members(member_id),
    CONSTRAINT ck_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_products_stock_non_negative CHECK (stock >= 0),
    CONSTRAINT ck_products_currency_twd CHECK (currency = 'TWD'),
    CONSTRAINT ck_products_status CHECK (status IN ('PUBLISHED', 'UNPUBLISHED'))
);

CREATE INDEX ix_products_public_listing ON products (status, deleted_at, created_at DESC);
CREATE INDEX ix_products_seller ON products (seller_id, deleted_at, created_at DESC);

CREATE TABLE product_images (
    image_id BIGINT IDENTITY(1,1) NOT NULL,
    product_id BIGINT NOT NULL,
    storage_key VARCHAR(100) NOT NULL,
    original_filename NVARCHAR(255) NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    sort_order INT NOT NULL CONSTRAINT df_product_images_sort_order DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_product_images_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_product_images PRIMARY KEY (image_id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(product_id),
    CONSTRAINT uq_product_images_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_product_images_size_positive CHECK (file_size > 0),
    CONSTRAINT ck_product_images_type CHECK (media_type IN ('image/jpeg', 'image/png', 'image/webp'))
);

CREATE INDEX ix_product_images_product ON product_images (product_id, sort_order, image_id);
