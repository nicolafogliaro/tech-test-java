-- products
INSERT INTO products (id, name, description, price, stock, created_at, updated_at)
VALUES
(10, 'Gaming Laptop', 'High performance laptop', 1500.00, 5, NOW(), NOW()),
(11, 'Office Chair', 'Ergonomic chair', 300.00, 20, NOW(), NOW());

-- orders
INSERT INTO orders (id, customer_id, description, status, total_amount, created_at, updated_at)
VALUES
(1, 100, 'Order for gaming laptop', 'CONFIRMED', 1500.00, '2023-10-01 10:00:00', '2023-10-01 10:00:00'),
(2, 100, 'Order for office supplies', 'CONFIRMED', 600.00, '2023-10-02 14:00:00', '2023-10-02 14:00:00'),
(3, 101, 'Miscellaneous items', 'PENDING', 200.00, '2023-10-10 08:00:00', '2023-10-10 08:00:00');

-- order_items
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price, created_at, updated_at)
VALUES
(1, 1, 10, 1, 1500.00, NOW(), NOW()),
(2, 2, 11, 2, 300.00, NOW(), NOW());
