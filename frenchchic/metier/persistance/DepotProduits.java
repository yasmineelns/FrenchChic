package frenchchic.metier.persistance;

import frenchchic.metier.Produit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dépôt des produits, implémenté en JDBC sur la table {@code produit}.
 *
 * <p>Chaque méthode ouvre et referme sa propre connexion. Toutes les requêtes
 * utilisent des {@link PreparedStatement} (pas de concaténation de valeurs
 * dans le SQL, donc pas d'injection SQL).
 */
public class DepotProduits {

    private static final String COLONNES = "reference, libelle, prix, quantite_stock";

    /**
     * @return tous les produits du catalogue, triés par libellé
     */
    public List<Produit> getProduits() {
        String sql = "SELECT " + COLONNES + " FROM produit ORDER BY libelle";
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Produit> produits = new ArrayList<>();
            while (rs.next()) {
                produits.add(lire(rs));
            }
            return produits;
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du catalogue impossible", e);
        }
    }

    /**
     * @param reference la référence du produit
     * @return le produit, ou {@code null} s'il n'existe pas
     */
    public Produit getProduit(String reference) {
        String sql = "SELECT " + COLONNES + " FROM produit WHERE reference = ?";
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du produit " + reference + " impossible", e);
        }
    }

    /**
     * Crée le produit s'il n'existe pas, sinon met à jour son libellé, son prix
     * et son stock (utilisé après la décrémentation du stock à la confirmation).
     *
     * @param produit le produit à enregistrer
     */
    public void enregistrer(Produit produit) {
        String sql = "INSERT INTO produit (" + COLONNES + ") VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (reference) DO UPDATE SET libelle = EXCLUDED.libelle, "
                + "prix = EXCLUDED.prix, quantite_stock = EXCLUDED.quantite_stock";
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, produit.getReference());
            ps.setString(2, produit.getLibelle());
            ps.setBigDecimal(3, BigDecimal.valueOf(produit.getPrix()));
            ps.setInt(4, produit.getQuantiteStock());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Enregistrement du produit "
                    + produit.getReference() + " impossible", e);
        }
    }

    private static Produit lire(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getString("reference"),
                rs.getString("libelle"),
                rs.getBigDecimal("prix").doubleValue(),
                rs.getInt("quantite_stock"));
    }
}
