CREATE TABLE products (
                          id          UUID            PRIMARY KEY,
                          name        VARCHAR(255)    NOT NULL,
                          price       NUMERIC(19,2)   NOT NULL,
                          created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
                          CONSTRAINT chk_products_price CHECK (price >= 0)
);

CREATE TABLE orders (
                        id          UUID            PRIMARY KEY,
                        user_ref    UUID            NOT NULL,
                        status      VARCHAR(20)     NOT NULL,
                        created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
                        updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
                        CONSTRAINT chk_orders_status
                            CHECK (status IN ('DRAFT', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE TABLE order_lines (
                             id            UUID            PRIMARY KEY,
                             order_id      UUID            NOT NULL REFERENCES orders(id)   ON DELETE CASCADE,
                             product_id    UUID            NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
                             product_name  VARCHAR(255)    NOT NULL,
                             unit_price    NUMERIC(19,2)   NOT NULL,
                             quantity      INTEGER         NOT NULL,
                             CONSTRAINT chk_order_lines_unit_price CHECK (unit_price >= 0),
                             CONSTRAINT chk_order_lines_quantity   CHECK (quantity > 0)
);

CREATE INDEX idx_order_lines_order_id ON order_lines(order_id);
CREATE INDEX idx_order_lines_product_id ON order_lines(product_id);