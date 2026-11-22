package frenchchic.metier.persistance;

import frenchchic.metier.Commande;
import frenchchic.metier.InfosLivraison;
import frenchchic.metier.LignePanier;
import frenchchic.metier.Produit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dépôt des commandes confirmées, implémenté en JDBC sur les tables
 * {@code commande} et {@code ligne_commande}.
 *
 * <p>Les commandes sont seulement ajoutées et relues : jamais supprimées ni
 * écrasées (exigence de robustesse : 10 ans). La base le garantit aussi par
 * des déclencheurs (voir {@code sql/schema.sql}).
 */
public class DepotCommandes {

    /**
     * @return le dernier numéro d'ordre enregistré (0 s'il n'y a aucune commande)
     */
    public int dernierNumeroOrdre() {
        String sql = "SELECT COALESCE(MAX(numero_ordre), 0) FROM commande";
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new PersistanceException("Lecture du dernier numéro d'ordre impossible", e);
        }
    }

    /**
     * Enregistre durablement une commande confirmée et ses lignes, dans une
     * seule transaction : soit tout est enregistré, soit rien.
     *
     * @param commande la commande à enregistrer
     * @throws PersistanceException si l'enregistrement échoue (par exemple
     *                              si le numéro de transaction existe déjà)
     */
    public void enregistrer(Commande commande) {
        String sqlCommande = "INSERT INTO commande (numero_transaction, numero_ordre, nom, prenom, "
                + "telephone, email, adresse, mode_paiement, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlLigne = "INSERT INTO ligne_commande (numero_transaction, numero_ligne, "
                + "reference_produit, libelle, prix_unitaire, quantite) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = ConnexionBD.ouvrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement psC = c.prepareStatement(sqlCommande);
                 PreparedStatement psL = c.prepareStatement(sqlLigne)) {

                InfosLivraison l = commande.getInfosLivraison();
                psC.setString(1, commande.getNumeroTransaction());
                psC.setInt(2, numeroOrdre(commande.getNumeroTransaction()));
                psC.setString(3, l.getNom());
                psC.setString(4, l.getPrenom());
                psC.setString(5, l.getTelephone());
                psC.setString(6, l.getEmail());
                psC.setString(7, l.getAdresse());
                psC.setString(8, commande.getModePaiement());
                psC.setBigDecimal(9, BigDecimal.valueOf(commande.getTotal()));
                psC.executeUpdate();

                int numeroLigne = 1;
                for (LignePanier ligne : commande.getLignes()) {
                    Produit p = ligne.getProduit();
                    psL.setString(1, commande.getNumeroTransaction());
                    psL.setInt(2, numeroLigne++);
                    psL.setString(3, p.getReference());
                    psL.setString(4, p.getLibelle());
                    psL.setBigDecimal(5, BigDecimal.valueOf(p.getPrix()));
                    psL.setInt(6, ligne.getQuantite());
                    psL.addBatch();
                }
                psL.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistanceException("Enregistrement de la commande "
                    + commande.getNumeroTransaction() + " impossible", e);
        }
    }

    /**
     * Relit une commande enregistrée. Le libellé et le prix des lignes sont
     * ceux du jour de l'achat ; le stock indiqué est le stock actuel.
     *
     * @param numeroTransaction le numéro de transaction (TR-AAAAMMJJ-NNNNNN)
     * @return la commande, ou {@code null} si elle n'existe pas
     */
    public Commande getCommande(String numeroTransaction) {
        String sqlCommande = "SELECT nom, prenom, telephone, email, adresse "
                + "FROM commande WHERE numero_transaction = ?";
        String sqlLignes = "SELECT l.reference_produit, l.libelle, l.prix_unitaire, l.quantite, "
                + "COALESCE(p.quantite_stock, 0) AS stock "
                + "FROM ligne_commande l LEFT JOIN produit p ON p.reference = l.reference_produit "
                + "WHERE l.numero_transaction = ? ORDER BY l.numero_ligne";

        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement psC = c.prepareStatement(sqlCommande);
             PreparedStatement psL = c.prepareStatement(sqlLignes)) {

            psC.setString(1, numeroTransaction);
            InfosLivraison infos;
            try (ResultSet rs = psC.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                infos = new InfosLivraison(rs.getString("nom"), rs.getString("prenom"),
                        rs.getString("telephone"), rs.getString("email"), rs.getString("adresse"));
            }

            psL.setString(1, numeroTransaction);
            List<LignePanier> lignes = new ArrayList<>();
            try (ResultSet rs = psL.executeQuery()) {
                while (rs.next()) {
                    Produit p = new Produit(rs.getString("reference_produit"), rs.getString("libelle"),
                            rs.getBigDecimal("prix_unitaire").doubleValue(), rs.getInt("stock"));
                    lignes.add(new LignePanier(p, rs.getInt("quantite")));
                }
            }
            return Commande.creer(lignes, infos, numeroTransaction);
        } catch (SQLException e) {
            throw new PersistanceException("Lecture de la commande " + numeroTransaction + " impossible", e);
        }
    }

    /** Extrait le numéro d'ordre (NNNNNN) de TR-AAAAMMJJ-NNNNNN. */
    static int numeroOrdre(String numeroTransaction) {
        int tiret = numeroTransaction.lastIndexOf('-');
        try {
            return Integer.parseInt(numeroTransaction.substring(tiret + 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Numéro de transaction invalide : " + numeroTransaction, e);
        }
    }
}
