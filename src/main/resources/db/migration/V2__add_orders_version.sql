-- Verrouillage optimiste sur la machine a etats.
-- Sans cette colonne, deux POST /confirm concurrents lisent tous les deux
-- le statut DRAFT, passent tous les deux le controle de transition, et le
-- second ecrase le premier sans que personne ne le sache (TOCTOU).
--
-- Pattern expand : la colonne est ajoutee avec un DEFAULT, donc les lignes
-- existantes sont valorisees sans downtime et l'ancienne version du code
-- continue de fonctionner pendant le deploiement.
ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
