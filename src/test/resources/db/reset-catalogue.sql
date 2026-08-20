-- Etat initial connu pour les tests d'integration.
DELETE FROM order_lines;
DELETE FROM orders;
DELETE FROM products;

INSERT INTO products (id, name, price) VALUES
                                           ('11111111-1111-1111-1111-111111111111', 'Clavier mecanique',  89.90),
                                           ('22222222-2222-2222-2222-222222222222', 'Souris ergonomique', 45.00);