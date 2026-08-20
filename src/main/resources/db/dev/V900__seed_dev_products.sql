-- Catalogue de developpement UNIQUEMENT (profil dev).
-- Sans produits en base, aucune commande n'est creable : le prix vient
-- desormais du catalogue et plus de la requete client.
-- Numero de version haut (V900) pour ne jamais entrer en collision avec
-- les migrations de schema V1, V2, V3...
INSERT INTO products (id, name, price) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Clavier mecanique',  89.90),
    ('22222222-2222-2222-2222-222222222222', 'Souris ergonomique', 45.00),
    ('33333333-3333-3333-3333-333333333333', 'Ecran 27 pouces',   249.99),
    ('44444444-4444-4444-4444-444444444444', 'Casque audio',      129.50);
