package frenchchic.metier.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Dépôt des réservations de stock, implémenté en JDBC sur la table
 * {@code reservation}.
 *
 * <p>Un panier (identifié par {@code idSession}) réserve ses articles jusqu'à
 * une heure d'expiration. Le stock disponible pour un client est :
 * stock du produit − réservations encore valides des AUTRES paniers.
 *
 * <p>L'heure courante ({@code maintenant}) est passée en paramètre : le
 * contrôleur utilise sa propre horloge, ce qui permet de tester l'expiration
 * sans attendre 15 minutes.
 */
public class DepotReservations {

    /**
     * Réserve {@code quantite} exemplaires du produit pour ce panier (remplace
     * la réservation précédente du même produit), si le stock le permet.
     *
     * <p>Le produit est verrouillé ({@code SELECT … FOR UPDATE}) pendant la
     * vérification : deux paniers ne peuvent pas réserver les mêmes articles
     * en même temps.
     *
     * @return {@code true} si la réservation est faite, {@code false} si le
     *         stock disponible est insuffisant (rien n'est alors modifié)
     */
    public boolean reserver(String idSession, String reference, int quantite,
                            Instant expireLe, Instant maintenant) {
        String verrou = "SELECT quantite_stock FROM produit WHERE reference = ? FOR UPDATE";
        String autres = "SELECT COALESCE(SUM(quantite), 0) FROM reservation "
                + "WHERE reference_produit = ? AND id_session <> ? AND expire_le > ?";
        String ecrire = "INSERT INTO reservation (id_session, reference_produit, quantite, expire_le) "
                + "VALUES (?, ?, ?, ?) ON CONFLICT (id_session, reference_produit) "
                + "DO UPDATE SET quantite = EXCLUDED.quantite, expire_le = EXCLUDED.expire_le";

        try (Connection c = ConnexionBD.ouvrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement psV = c.prepareStatement(verrou);
                 PreparedStatement psA = c.prepareStatement(autres);
                 PreparedStatement psE = c.prepareStatement(ecrire)) {

                psV.setString(1, reference);
                int stock;
                try (ResultSet rs = psV.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return false;
                    }
                    stock = rs.getInt(1);
                }

                psA.setString(1, reference);
                psA.setString(2, idSession);
                psA.setTimestamp(3, Timestamp.from(maintenant));
                int reserveParAutres;
                try (ResultSet rs = psA.executeQuery()) {
                    rs.next();
                    reserveParAutres = rs.getInt(1);
                }

                if (quantite > stock - reserveParAutres) {
                    c.rollback();
                    return false;
                }

                psE.setString(1, idSession);
                psE.setString(2, reference);
                psE.setInt(3, quantite);
                psE.setTimestamp(4, Timestamp.from(expireLe));
                psE.executeUpdate();
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistanceException("Réservation du produit " + reference + " impossible", e);
        }
    }

    /**
     * @return le stock que ce panier peut encore réserver pour ce produit :
     *         stock − réservations valides des autres paniers (0 au minimum)
     */
    public int disponiblePour(String idSession, String reference, Instant maintenant) {
        String sql = "SELECT p.quantite_stock - COALESCE((SELECT SUM(r.quantite) FROM reservation r "
                + "WHERE r.reference_produit = p.reference AND r.id_session <> ? AND r.expire_le > ?), 0) "
                + "FROM produit p WHERE p.reference = ?";
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, idSession);
            ps.setTimestamp(2, Timestamp.from(maintenant));
            ps.setString(3, reference);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Math.max(0, rs.getInt(1)) : 0;
            }
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du stock disponible de " + reference + " impossible", e);
        }
    }

    /**
     * @return pour chaque produit, la quantité réservée par des paniers encore
     *         valides (tous paniers confondus)
     */
    public Map<String, Integer> reservationsActives(Instant maintenant) {
        String sql = "SELECT reference_produit, SUM(quantite) FROM reservation "
                + "WHERE expire_le > ? GROUP BY reference_produit";
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(maintenant));
            Map<String, Integer> resultat = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultat.put(rs.getString(1), rs.getInt(2));
                }
            }
            return resultat;
        } catch (SQLException e) {
            throw new PersistanceException("Lecture des réservations impossible", e);
        }
    }

    /** Supprime la réservation d'un produit pour ce panier. */
    public void liberer(String idSession, String reference) {
        executer("DELETE FROM reservation WHERE id_session = ? AND reference_produit = ?",
                idSession, reference);
    }

    /** Supprime toutes les réservations de ce panier. */
    public void libererTout(String idSession) {
        executer("DELETE FROM reservation WHERE id_session = ?", idSession);
    }

    private static void executer(String sql, String... valeurs) {
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < valeurs.length; i++) {
                ps.setString(i + 1, valeurs[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistanceException("Libération des réservations impossible", e);
        }
    }
}
