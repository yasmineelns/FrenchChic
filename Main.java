package frenchchic;

import javax.swing.SwingUtilities;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.metier.Produit;
import frenchchic.vue.EcranAccueil;

/**
 * Point d'entrée de l'application French Chic "Ventes & stock" (Sprint 1).
 * Crée le produit du jour, le contrôleur du CU "Passer commande en ligne",
 * puis affiche l'écran d'accueil.
 */
public class Main {

    public static void main(String[] args) {
        // Hypothèse du sprint 1 : le produit du jour est déjà défini au lancement.
        Produit produitDuJour = new Produit("Robe d'été fleurie", 49.90, 12);

        SessionPasserCommande session = new SessionPasserCommande(produitDuJour);
"""
        // Swing doit être manipulé depuis le thread d'événements (EDT).
        SwingUtilities.invokeLater(() -> {
            EcranAccueil ecran = new EcranAccueil(session);
            ecran.setVisible(true);
        });
        """
        
    }
}
