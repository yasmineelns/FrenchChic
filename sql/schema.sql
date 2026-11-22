-- =====================================================================
-- French Chic — schéma PostgreSQL (lot 2 : persistance)
--
-- À exécuter une fois sur une base vide (relançable sans risque) :
--   psql -U postgres -d frenchchic -f sql/schema.sql
-- =====================================================================

-- Ce fichier est en UTF-8. Sans cette ligne, psql sous Windows le lit en
-- WIN1252 et les accents sont enregistrés de travers (« Ã© » au lieu de « é »).
SET client_encoding = 'UTF8';

-- Catalogue des produits
CREATE TABLE IF NOT EXISTS produit (
    reference       VARCHAR(20)   PRIMARY KEY,
    libelle         VARCHAR(100)  NOT NULL,
    prix            NUMERIC(10,2) NOT NULL CHECK (prix >= 0),
    quantite_stock  INTEGER       NOT NULL CHECK (quantite_stock >= 0)
);

-- Commandes confirmées : jamais modifiées ni supprimées (conservation 10 ans)
CREATE TABLE IF NOT EXISTS commande (
    numero_transaction  VARCHAR(30)   PRIMARY KEY,          -- TR-AAAAMMJJ-NNNNNN
    numero_ordre        INTEGER       NOT NULL UNIQUE,      -- NNNNNN
    date_commande       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nom                 VARCHAR(100)  NOT NULL,
    prenom              VARCHAR(100)  NOT NULL,
    telephone           VARCHAR(20)   NOT NULL,
    email               VARCHAR(150)  NOT NULL,
    adresse             VARCHAR(300)  NOT NULL,
    mode_paiement       VARCHAR(30)   NOT NULL,
    total               NUMERIC(10,2) NOT NULL CHECK (total >= 0)
);

-- Lignes d'une commande. Le libellé et le prix sont recopiés au moment de
-- l'achat : la commande reste juste même si le catalogue change ensuite.
CREATE TABLE IF NOT EXISTS ligne_commande (
    numero_transaction  VARCHAR(30)   NOT NULL REFERENCES commande (numero_transaction),
    numero_ligne        INTEGER       NOT NULL,
    reference_produit   VARCHAR(20)   NOT NULL REFERENCES produit (reference),
    libelle             VARCHAR(100)  NOT NULL,
    prix_unitaire       NUMERIC(10,2) NOT NULL CHECK (prix_unitaire >= 0),
    quantite            INTEGER       NOT NULL CHECK (quantite > 0),
    PRIMARY KEY (numero_transaction, numero_ligne)
);

-- La base elle-même interdit de modifier ou de supprimer une commande.
CREATE OR REPLACE FUNCTION interdire_modification_commande() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Les commandes confirmées ne peuvent être ni modifiées ni supprimées';
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER commande_immuable
    BEFORE UPDATE OR DELETE ON commande
    FOR EACH ROW EXECUTE FUNCTION interdire_modification_commande();

CREATE OR REPLACE TRIGGER ligne_commande_immuable
    BEFORE UPDATE OR DELETE ON ligne_commande
    FOR EACH ROW EXECUTE FUNCTION interdire_modification_commande();

-- Catalogue de départ (ne touche pas au stock des produits déjà présents,
-- mais corrige leur libellé s'il a été mal enregistré)
INSERT INTO produit (reference, libelle, prix, quantite_stock) VALUES
    ('ROBE-001',  'Robe d''été fleurie',   49.90, 12),
    ('JEAN-001',  'Jean slim brut',         59.00, 20),
    ('SAC-001',   'Sac en cuir camel',      89.00,  5),
    ('VESTE-001', 'Veste en lin beige',     79.90,  8),
    ('CHAUS-001', 'Ballerines vernies',     45.00, 10),
    ('FOUL-001',  'Foulard en soie',        29.90, 15)
ON CONFLICT (reference) DO UPDATE SET libelle = EXCLUDED.libelle;
