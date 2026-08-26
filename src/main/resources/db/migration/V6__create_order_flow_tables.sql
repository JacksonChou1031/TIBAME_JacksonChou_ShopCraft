CREATE TABLE order_status_history (
    status_history_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at DATETIME2(6) NOT NULL CONSTRAINT df_order_status_history_changed_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_order_status_history PRIMARY KEY (status_history_id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_member FOREIGN KEY (changed_by) REFERENCES members(member_id),
    CONSTRAINT ck_order_status_history_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PENDING_SHIPMENT', 'SHIPPED', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE order_shipments (
    order_id BIGINT NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    shipped_at DATETIME2(6) NOT NULL CONSTRAINT df_order_shipments_shipped_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_order_shipments PRIMARY KEY (order_id),
    CONSTRAINT uq_order_shipments_tracking_number UNIQUE (tracking_number),
    CONSTRAINT fk_order_shipments_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE INDEX ix_order_status_history_order ON order_status_history (order_id, changed_at, status_history_id);

INSERT INTO order_status_history (order_id, status, changed_by, changed_at)
SELECT o.order_id, o.status, o.buyer_id, o.created_at
  FROM orders o
 WHERE NOT EXISTS (
       SELECT 1 FROM order_status_history h WHERE h.order_id = o.order_id
  );
