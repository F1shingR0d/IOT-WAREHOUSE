-- =====================================================================
--  WAREHOUSE MANAGEMENT SYSTEM - Apache Derby database schema
--  13 tables with primary keys, foreign keys, UNIQUE, NOT NULL,
--  DEFAULT and CHECK constraints.
--  This script is executed automatically on the first run of the program
--  (see DatabaseInitializer.java). Tables are created in dependency order.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. USERS : system accounts (passwords are stored as PBKDF2 hashes)
-- ---------------------------------------------------------------------
CREATE TABLE USERS (
    user_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    status         VARCHAR(10)  DEFAULT 'ACTIVE' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- 2. CATEGORIES : product classification
-- ---------------------------------------------------------------------
CREATE TABLE CATEGORIES (
    category_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    category_name  VARCHAR(100) NOT NULL,
    description    VARCHAR(255),
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (category_id),
    CONSTRAINT uq_categories_name UNIQUE (category_name)
);

-- ---------------------------------------------------------------------
-- 3. PRODUCTS : items stored in the warehouses (CATEGORIES 1 : M PRODUCTS)
-- ---------------------------------------------------------------------
CREATE TABLE PRODUCTS (
    product_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    sku            VARCHAR(30)   NOT NULL,
    product_name   VARCHAR(100)  NOT NULL,
    category_id    INTEGER       NOT NULL,
    unit           VARCHAR(20)   DEFAULT 'pcs' NOT NULL,
    unit_cost      DECIMAL(10,2) DEFAULT 0 NOT NULL,
    reorder_level  INTEGER       DEFAULT 10 NOT NULL,
    description    VARCHAR(255),
    status         VARCHAR(10)   DEFAULT 'ACTIVE' NOT NULL,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (product_id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES CATEGORIES (category_id),
    CONSTRAINT ck_products_cost CHECK (unit_cost >= 0),
    CONSTRAINT ck_products_reorder CHECK (reorder_level >= 0),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- 4. WAREHOUSES : storage locations
--    manager_id is UNIQUE and optional -> one-to-one with USERS
--    (a user can be in charge of at most one warehouse)
-- ---------------------------------------------------------------------
CREATE TABLE WAREHOUSES (
    warehouse_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    warehouse_code VARCHAR(20)  NOT NULL,
    warehouse_name VARCHAR(100) NOT NULL,
    location       VARCHAR(255) NOT NULL,
    capacity       INTEGER      NOT NULL,
    contact_no     VARCHAR(20),
    manager_id     INTEGER,
    status         VARCHAR(10)  DEFAULT 'ACTIVE' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_warehouses PRIMARY KEY (warehouse_id),
    CONSTRAINT uq_warehouses_code UNIQUE (warehouse_code),
    CONSTRAINT uq_warehouses_name UNIQUE (warehouse_name),
    CONSTRAINT uq_warehouses_manager UNIQUE (manager_id),
    CONSTRAINT fk_warehouses_manager FOREIGN KEY (manager_id) REFERENCES USERS (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_warehouses_capacity CHECK (capacity > 0),
    CONSTRAINT ck_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- 5. SUPPLIERS : where received stock comes from
-- ---------------------------------------------------------------------
CREATE TABLE SUPPLIERS (
    supplier_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    supplier_name  VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    contact_no     VARCHAR(20)  NOT NULL,
    email          VARCHAR(100),
    address        VARCHAR(255),
    status         VARCHAR(10)  DEFAULT 'ACTIVE' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_suppliers PRIMARY KEY (supplier_id),
    CONSTRAINT uq_suppliers_name UNIQUE (supplier_name),
    CONSTRAINT ck_suppliers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- 6. CUSTOMERS : where released stock goes to
-- ---------------------------------------------------------------------
CREATE TABLE CUSTOMERS (
    customer_id    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    customer_name  VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    contact_no     VARCHAR(20)  NOT NULL,
    email          VARCHAR(100),
    address        VARCHAR(255),
    status         VARCHAR(10)  DEFAULT 'ACTIVE' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    CONSTRAINT uq_customers_name UNIQUE (customer_name),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ---------------------------------------------------------------------
-- 7. WAREHOUSE_STOCK : quantity of each product in each warehouse.
--    Associative table: WAREHOUSES M : N PRODUCTS.
--    Composite primary key (warehouse_id, product_id).
-- ---------------------------------------------------------------------
CREATE TABLE WAREHOUSE_STOCK (
    warehouse_id   INTEGER   NOT NULL,
    product_id     INTEGER   NOT NULL,
    quantity       INTEGER   DEFAULT 0 NOT NULL,
    last_updated   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_warehouse_stock PRIMARY KEY (warehouse_id, product_id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES WAREHOUSES (warehouse_id),
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT ck_stock_quantity CHECK (quantity >= 0)
);

-- ---------------------------------------------------------------------
-- 8. RECEIVINGS : stock IN from a supplier (document header)
-- ---------------------------------------------------------------------
CREATE TABLE RECEIVINGS (
    receiving_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    reference_no   VARCHAR(20)  NOT NULL,
    receiving_date DATE         NOT NULL,
    warehouse_id   INTEGER      NOT NULL,
    supplier_id    INTEGER      NOT NULL,
    user_id        INTEGER      NOT NULL,
    remarks        VARCHAR(255),
    status         VARCHAR(10)  DEFAULT 'POSTED' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_receivings PRIMARY KEY (receiving_id),
    CONSTRAINT uq_receivings_ref UNIQUE (reference_no),
    CONSTRAINT fk_receivings_warehouse FOREIGN KEY (warehouse_id) REFERENCES WAREHOUSES (warehouse_id),
    CONSTRAINT fk_receivings_supplier FOREIGN KEY (supplier_id) REFERENCES SUPPLIERS (supplier_id),
    CONSTRAINT fk_receivings_user FOREIGN KEY (user_id) REFERENCES USERS (user_id),
    CONSTRAINT ck_receivings_status CHECK (status IN ('POSTED', 'VOIDED'))
);

-- ---------------------------------------------------------------------
-- 9. RECEIVING_ITEMS : products of a receiving (RECEIVINGS M : N PRODUCTS)
-- ---------------------------------------------------------------------
CREATE TABLE RECEIVING_ITEMS (
    item_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    receiving_id   INTEGER       NOT NULL,
    product_id     INTEGER       NOT NULL,
    quantity       INTEGER       NOT NULL,
    unit_cost      DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_receiving_items PRIMARY KEY (item_id),
    CONSTRAINT uq_receiving_items UNIQUE (receiving_id, product_id),
    CONSTRAINT fk_ritems_receiving FOREIGN KEY (receiving_id) REFERENCES RECEIVINGS (receiving_id) ON DELETE CASCADE,
    CONSTRAINT fk_ritems_product FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT ck_ritems_quantity CHECK (quantity > 0),
    CONSTRAINT ck_ritems_cost CHECK (unit_cost >= 0)
);

-- ---------------------------------------------------------------------
-- 10. RELEASES : stock OUT to a customer (document header)
-- ---------------------------------------------------------------------
CREATE TABLE RELEASES (
    release_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    reference_no   VARCHAR(20)  NOT NULL,
    release_date   DATE         NOT NULL,
    warehouse_id   INTEGER      NOT NULL,
    customer_id    INTEGER      NOT NULL,
    user_id        INTEGER      NOT NULL,
    remarks        VARCHAR(255),
    status         VARCHAR(10)  DEFAULT 'POSTED' NOT NULL,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_releases PRIMARY KEY (release_id),
    CONSTRAINT uq_releases_ref UNIQUE (reference_no),
    CONSTRAINT fk_releases_warehouse FOREIGN KEY (warehouse_id) REFERENCES WAREHOUSES (warehouse_id),
    CONSTRAINT fk_releases_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id),
    CONSTRAINT fk_releases_user FOREIGN KEY (user_id) REFERENCES USERS (user_id),
    CONSTRAINT ck_releases_status CHECK (status IN ('POSTED', 'VOIDED'))
);

-- ---------------------------------------------------------------------
-- 11. RELEASE_ITEMS : products of a release (RELEASES M : N PRODUCTS)
-- ---------------------------------------------------------------------
CREATE TABLE RELEASE_ITEMS (
    item_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    release_id     INTEGER NOT NULL,
    product_id     INTEGER NOT NULL,
    quantity       INTEGER NOT NULL,
    CONSTRAINT pk_release_items PRIMARY KEY (item_id),
    CONSTRAINT uq_release_items UNIQUE (release_id, product_id),
    CONSTRAINT fk_litems_release FOREIGN KEY (release_id) REFERENCES RELEASES (release_id) ON DELETE CASCADE,
    CONSTRAINT fk_litems_product FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT ck_litems_quantity CHECK (quantity > 0)
);

-- ---------------------------------------------------------------------
-- 12. STOCK_TRANSFERS : stock moved between two different warehouses
--     (two foreign keys to the same WAREHOUSES table)
-- ---------------------------------------------------------------------
CREATE TABLE STOCK_TRANSFERS (
    transfer_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    reference_no      VARCHAR(20)  NOT NULL,
    transfer_date     DATE         NOT NULL,
    from_warehouse_id INTEGER      NOT NULL,
    to_warehouse_id   INTEGER      NOT NULL,
    user_id           INTEGER      NOT NULL,
    remarks           VARCHAR(255),
    status            VARCHAR(10)  DEFAULT 'POSTED' NOT NULL,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_stock_transfers PRIMARY KEY (transfer_id),
    CONSTRAINT uq_transfers_ref UNIQUE (reference_no),
    CONSTRAINT fk_transfers_from FOREIGN KEY (from_warehouse_id) REFERENCES WAREHOUSES (warehouse_id),
    CONSTRAINT fk_transfers_to FOREIGN KEY (to_warehouse_id) REFERENCES WAREHOUSES (warehouse_id),
    CONSTRAINT fk_transfers_user FOREIGN KEY (user_id) REFERENCES USERS (user_id),
    CONSTRAINT ck_transfers_warehouses CHECK (from_warehouse_id <> to_warehouse_id),
    CONSTRAINT ck_transfers_status CHECK (status IN ('POSTED', 'VOIDED'))
);

-- ---------------------------------------------------------------------
-- 13. TRANSFER_ITEMS : products of a transfer (STOCK_TRANSFERS M : N PRODUCTS)
-- ---------------------------------------------------------------------
CREATE TABLE TRANSFER_ITEMS (
    item_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    transfer_id    INTEGER NOT NULL,
    product_id     INTEGER NOT NULL,
    quantity       INTEGER NOT NULL,
    CONSTRAINT pk_transfer_items PRIMARY KEY (item_id),
    CONSTRAINT uq_transfer_items UNIQUE (transfer_id, product_id),
    CONSTRAINT fk_titems_transfer FOREIGN KEY (transfer_id) REFERENCES STOCK_TRANSFERS (transfer_id) ON DELETE CASCADE,
    CONSTRAINT fk_titems_product FOREIGN KEY (product_id) REFERENCES PRODUCTS (product_id),
    CONSTRAINT ck_titems_quantity CHECK (quantity > 0)
);

-- ---------------------------------------------------------------------
-- Extra indexes for date searches (Derby indexes PK/UNIQUE/FK columns automatically)
-- ---------------------------------------------------------------------
CREATE INDEX idx_receivings_date ON RECEIVINGS (receiving_date);
CREATE INDEX idx_releases_date ON RELEASES (release_date);
CREATE INDEX idx_transfers_date ON STOCK_TRANSFERS (transfer_date);
