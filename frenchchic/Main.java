package frenchchic;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.metier.persistance.DepotCommandes;
import frenchchic.metier.persistance.DepotProduits;
import frenchchic.metier.persistance.DepotReservations;
import frenchchic.metier.persistance.PersistanceException;
import frenchchic.vue.FenetreFrenchChic;

/**
 * Point d'entrée de l'application French Chic « Ventes & stock » (sprint 2).
 *
 * <p>Crée les dépôts JDBC (PostgreSQL), le contrôleur du CU « Passer commande
 * en ligne », puis la fenêtre principale qui gère la navigation entre les
 * quatre écrans.
 */
public class Main {

    public static void main(String[] args) {
        DepotProduits depotProduits = new DepotProduits();
        DepotCommandes depotCommandes = new DepotCommandes();

        // Vérifie tout de suite que la base répond, pour afficher un message
        // clair plutôt qu'une erreur au milieu de l'écran d'accueil.
        try {
            depotProduits.getProduits();
        } catch (PersistanceException e) {
            String cause = e.getCause() != null ? "\n\n" + e.getCause().getMessage() : "";
            System.err.println("Base de données inaccessible : " + e.getMessage() + cause);
            JOptionPane.showMessageDialog(null,
                    "Impossible de se connecter à la base de données.\n\n" + e.getMessage() + cause,
                    "French Chic", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SessionPasserCommande session = new SessionPasserCommande(
                depotProduits, depotCommandes, new DepotReservations());

        // Swing doit être manipulé depuis le thread d'événements (EDT).
        SwingUtilities.invokeLater(() -> new FenetreFrenchChic(session).setVisible(true));
    }
}
