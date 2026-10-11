-- =====================================================================
--  WAREHOUSE MANAGEMENT SYSTEM - sample master data
--  Loaded automatically on the first run, after the user accounts are
--  created. Sample transactions (receiving, releasing, transfers) are
--  added by SampleDataSeeder.java so that the stock is always consistent.
--  INSERT ... SELECT is used to look up foreign keys by name.
-- =====================================================================

-- ------------------------------ CATEGORIES ---------------------------
INSERT INTO CATEGORIES (category_name, description) VALUES ('Electronics', 'Computer peripherals, cables and gadgets');
INSERT INTO CATEGORIES (category_name, description) VALUES ('Office Supplies', 'Paper, writing materials and office tools');
INSERT INTO CATEGORIES (category_name, description) VALUES ('Hardware & Tools', 'Hand tools, power tools and construction hardware');
INSERT INTO CATEGORIES (category_name, description) VALUES ('Food & Beverages', 'Packaged food, drinks and grocery items');
INSERT INTO CATEGORIES (category_name, description) VALUES ('Cleaning Supplies', 'Detergents, disinfectants and janitorial items');
INSERT INTO CATEGORIES (category_name, description) VALUES ('Packaging Materials', 'Boxes, tapes and wrapping materials');
INSERT INTO CATEGORIES (category_name, description) VALUES ('Safety Equipment', 'Personal protective equipment such as gloves, helmets and vests');

-- ------------------------------ SUPPLIERS ----------------------------
INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address)
    VALUES ('Allied Electronics Distributors', 'Mark Villanueva', '0917-555-0101', 'sales@allied-electronics.example.com', 'Unit 5, Tech Hub Bldg., Ortigas Center, Pasig City');
INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address)
    VALUES ('PrimeOffice Supply Co.', 'Grace Tan', '0918-555-0102', 'orders@primeoffice.example.com', '123 Rizal Avenue, Sta. Cruz, Manila');
INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address)
    VALUES ('BuildRight Hardware Trading', 'Ramon Cruz', '0919-555-0103', 'info@buildright.example.com', '45 MacArthur Highway, Malolos, Bulacan');
INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address)
    VALUES ('FreshSource Food Corporation', 'Liza Mendoza', '0920-555-0104', 'supply@freshsource.example.com', '88 Food Terminal Complex, Taguig City');
INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address)
    VALUES ('CleanPro Industrial Supply', 'Dennis Aquino', '0921-555-0105', 'cleanpro@example.com', '17 Industrial Road, Canlubang, Laguna');
INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address)
    VALUES ('PackWell Packaging Solutions', 'Jenny Lim', '(02) 8555-0106', 'packwell@example.com', '9 Warehouse Street, Valenzuela City');

-- ------------------------------ CUSTOMERS ----------------------------
INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, address)
    VALUES ('Northpoint Retail Mart', 'Carlo Ramos', '0927-555-0201', 'purchasing@northpoint.example.com', 'North Avenue, Quezon City');
INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, address)
    VALUES ('Golden Harvest Grocery', 'Teresa Uy', '0928-555-0202', 'orders@goldenharvest.example.com', 'Rizal Street, San Fernando, Pampanga');
INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, address)
    VALUES ('Metro Office Solutions Inc.', 'Paolo Garcia', '0929-555-0203', 'admin@metrooffice.example.com', 'Ayala Avenue, Makati City');
INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, address)
    VALUES ('Sunrise Construction Corp.', 'Engr. Victor Salazar', '0930-555-0204', 'procurement@sunrise.example.com', 'Km. 30 Aguinaldo Highway, Dasmarinas, Cavite');
INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, address)
    VALUES ('Bayview Hotel & Suites', 'Angela Dizon', '0931-555-0205', 'housekeeping@bayview.example.com', 'Roxas Boulevard, Pasay City');

-- ------------------------------ WAREHOUSES ---------------------------
INSERT INTO WAREHOUSES (warehouse_code, warehouse_name, location, capacity, contact_no, manager_id)
    SELECT 'WH-001', 'Main Warehouse', 'Km. 14 MacArthur Highway, Valenzuela City', 2800, '(02) 8555-0301', u.user_id
    FROM USERS u WHERE u.username = 'manager';
INSERT INTO WAREHOUSES (warehouse_code, warehouse_name, location, capacity, contact_no, manager_id)
    SELECT 'WH-002', 'North Distribution Center', 'Jose Abad Santos Avenue, San Fernando, Pampanga', 2500, '0917-555-0302', u.user_id
    FROM USERS u WHERE u.username = 'rreyes';
INSERT INTO WAREHOUSES (warehouse_code, warehouse_name, location, capacity, contact_no)
    VALUES ('WH-003', 'South Distribution Center', 'National Highway, Calamba, Laguna', 3500, '0917-555-0303');

-- ------------------------------ PRODUCTS -----------------------------
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'ELE-1001', 'LED Monitor 24-inch', c.category_id, 'pcs', 6850.00, 10, 'Full HD IPS monitor, HDMI/VGA' FROM CATEGORIES c WHERE c.category_name = 'Electronics';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'ELE-1002', 'Wireless Mouse', c.category_id, 'pcs', 450.00, 30, '2.4 GHz optical mouse with USB receiver' FROM CATEGORIES c WHERE c.category_name = 'Electronics';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'ELE-1003', 'USB Keyboard', c.category_id, 'pcs', 520.00, 25, 'Full-size wired keyboard' FROM CATEGORIES c WHERE c.category_name = 'Electronics';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'ELE-1004', 'HDMI Cable 2m', c.category_id, 'pcs', 180.00, 40, 'High-speed HDMI cable' FROM CATEGORIES c WHERE c.category_name = 'Electronics';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'ELE-1005', 'Power Bank 10000mAh', c.category_id, 'pcs', 890.00, 20, 'Dual USB output power bank' FROM CATEGORIES c WHERE c.category_name = 'Electronics';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description, status)
    SELECT 'ELE-1006', 'VGA Cable 1.5m', c.category_id, 'pcs', 95.00, 0, 'Discontinued item', 'INACTIVE' FROM CATEGORIES c WHERE c.category_name = 'Electronics';

INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'OFF-2001', 'A4 Bond Paper 70gsm', c.category_id, 'ream', 235.00, 100, '500 sheets per ream' FROM CATEGORIES c WHERE c.category_name = 'Office Supplies';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'OFF-2002', 'Ballpen Black (Box of 50)', c.category_id, 'box', 310.00, 20, '0.5 mm fine tip' FROM CATEGORIES c WHERE c.category_name = 'Office Supplies';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'OFF-2003', 'Heavy-Duty Stapler', c.category_id, 'pcs', 385.00, 15, 'Staples up to 100 sheets' FROM CATEGORIES c WHERE c.category_name = 'Office Supplies';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'OFF-2004', 'Printer Ink Black 70ml', c.category_id, 'bottle', 295.00, 30, 'Refill ink for tank printers' FROM CATEGORIES c WHERE c.category_name = 'Office Supplies';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'OFF-2005', 'Long Folder (Pack of 100)', c.category_id, 'pack', 420.00, 15, 'Long-size manila folders' FROM CATEGORIES c WHERE c.category_name = 'Office Supplies';

INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'HRD-3001', 'Cordless Drill 18V', c.category_id, 'pcs', 3450.00, 8, 'With battery and charger' FROM CATEGORIES c WHERE c.category_name = 'Hardware & Tools';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'HRD-3002', 'Claw Hammer 16oz', c.category_id, 'pcs', 320.00, 15, 'Fiberglass handle' FROM CATEGORIES c WHERE c.category_name = 'Hardware & Tools';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'HRD-3003', 'Screwdriver Set 12-pc', c.category_id, 'set', 560.00, 12, 'Flat and Phillips heads' FROM CATEGORIES c WHERE c.category_name = 'Hardware & Tools';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'HRD-3004', 'Measuring Tape 5m', c.category_id, 'pcs', 145.00, 25, 'Steel tape with lock' FROM CATEGORIES c WHERE c.category_name = 'Hardware & Tools';

INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'FNB-4001', 'Bottled Water 500ml (Case of 24)', c.category_id, 'case', 216.00, 50, 'Purified drinking water' FROM CATEGORIES c WHERE c.category_name = 'Food & Beverages';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'FNB-4002', '3-in-1 Instant Coffee (Pack of 30)', c.category_id, 'pack', 265.00, 40, 'Original blend sachets' FROM CATEGORIES c WHERE c.category_name = 'Food & Beverages';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'FNB-4003', 'Canned Tuna 155g (Case of 48)', c.category_id, 'case', 1680.00, 15, 'Tuna flakes in oil' FROM CATEGORIES c WHERE c.category_name = 'Food & Beverages';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'FNB-4004', 'Rice 25kg Sack', c.category_id, 'sack', 1350.00, 30, 'Well-milled rice' FROM CATEGORIES c WHERE c.category_name = 'Food & Beverages';

INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'CLN-5001', 'Dishwashing Liquid 1L', c.category_id, 'bottle', 98.00, 40, 'Lemon scent' FROM CATEGORIES c WHERE c.category_name = 'Cleaning Supplies';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'CLN-5002', 'Bleach 1 Gallon', c.category_id, 'gallon', 165.00, 25, 'Chlorine bleach' FROM CATEGORIES c WHERE c.category_name = 'Cleaning Supplies';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'CLN-5003', 'Garbage Bags XL (Roll of 10)', c.category_id, 'roll', 75.00, 50, 'Heavy-duty black bags' FROM CATEGORIES c WHERE c.category_name = 'Cleaning Supplies';

INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'PKG-6001', 'Corrugated Box Medium', c.category_id, 'pcs', 38.00, 200, '16 x 12 x 10 inches' FROM CATEGORIES c WHERE c.category_name = 'Packaging Materials';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'PKG-6002', 'Packing Tape 2-inch', c.category_id, 'roll', 45.00, 100, 'Clear packaging tape' FROM CATEGORIES c WHERE c.category_name = 'Packaging Materials';
INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, reorder_level, description)
    SELECT 'PKG-6003', 'Stretch Film 18-inch', c.category_id, 'roll', 420.00, 20, 'Pallet wrapping film' FROM CATEGORIES c WHERE c.category_name = 'Packaging Materials';
