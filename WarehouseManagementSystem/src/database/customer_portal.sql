-- =====================================================================
--  WAREHOUSE MANAGEMENT SYSTEM - Customer Portal tables (tables 14-16)
--  Customers register and log in, then request stock from a warehouse.
--  A request does not change the stock; staff release it later, which
--  creates a normal RELEASES document.
--  Executed automatically after schema.sql on a new database, and once on
--  an existing database that does not have these tables yet
--  (see DatabaseInitializer.java).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 14. CUSTOMER_ACCOUNTS : customer login accounts
--     customer_id is UNIQUE -> one-to-one with CUSTOMERS
--     (a customer has at most one login account).
--     New registrations start as PENDING until staff approve them.
-- ---------------------------------------------------------------------
CREATE TABLE CUSTOMER_ACCOUNTS (
    account_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    customer_id    INTEGER      NOT NULL,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    status         VARCHAR(10)  DEFAULT 'PENDING' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login     TIMESTAMP,
    CONSTRAINT pk_customer_accounts PRIMARY KEY (account_id),
    CONSTRAINT uq_caccounts_customer UNIQUE (customer_id),
    CONSTRAINT uq_caccounts_username UNIQUE (username),
    CONSTRAINT fk_caccounts_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id) ON DELETE CASCADE,
    CONSTRAINT ck_caccounts_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- 15. STOCK_REQUESTS : stock requested by a customer from a warehouse
--     PENDING -> APPROVED -> FULFILLED (released), or REJECTED / CANCELLED.
--     release_id links a fulfilled request to the release that delivered
--     it (UNIQUE: one release fulfils at most one request).
-- ---------------------------------------------------------------------
CREATE TABLE STOCK_REQUESTS (
    request_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    reference_no   VARCHAR(20)  NOT NULL,
    request_date   DATE         NOT NULL,
    needed_by      DATE,
    customer_id    INTEGER      NOT NULL,
    warehouse_id   INTEGER      NOT NULL,
    remarks        VARCHAR(255),
    status         VARCHAR(10)  DEFAULT 'PENDING' NOT NULL,
    reviewed_by    INTEGER,
    reviewed_at    TIMESTAMP,
    review_note    VARCHAR(255),
    release_id     INTEGER,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_stock_requests PRIMARY KEY (request_id),
    CONSTRAINT uq_requests_ref UNIQUE (reference_no),
    CONSTRAINT uq_requests_release UNIQUE (release_id),
    CONSTRAINT fk_requests_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id),
    CONSTRAINT fk_requests_warehouse FOREIGN KEY (warehouse_id) REFERENCES WAREHOUSES (warehouse_id),
    CONSTRAINT fk_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES USERS (user_id) ON DELETE SET NULL,
    CONSTRAINT fk_requests_release FOREIGN KEY (release_id) REFERENCES RELEASES (release_id),
    CONSTRAINT ck_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'FULFILLED')),
    CONSTRAINT ck_requests_needed_by CHECK (needed_by IS NULL OR needed_by >= request_date)
);

-- ---------------------------------------------------------------------
-- 16. STOCK_REQUEST_ITEMS : products of a request (STOCK_REQUESTS M : N PRODUCTS)
-- ---------------------------------------------------------------------
CREATE TABLE STOCK_REQUEST_ITEMS (
    item_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    request_id     INTEGER NOT NULL,
    product_id     INTEGER NOT NULL,
    quantity       INTEGER NOT NULL,
    CONSTRAINT pk_request_items PRIMARY KEY (item_id),
    CONSTRAINT uq_request_items UNIQUE (request_id, product_id),
    CONSTRAINT fk_qitems_request FOREIGN KEY (request_id) REFERENCES STOCK_REQUESTS (request_id) ON DELETE CASCADE,
    CONSTRAINT fk_qitems_product FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT ck_qitems_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_requests_date ON STOCK_REQUESTS (request_date);
