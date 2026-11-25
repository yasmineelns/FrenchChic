package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.controleur.SessionPasserCommande.ResultatConfirmation;
import java.awt.*;
import javax.swing.*;

public class FenetreFrenchChic extends JFrame {

    private final SessionPasserCommande session;
    private final JPanel conteneur;
    private final CardLayout cardLayout;

    // Références aux écrans pour pouvoir appeler rafraichir() et afficher()
    private final EcranAccueil           ecranAccueil;
    private final EcranPanier            ecranPanier;
    private final EcranLivraisonPaiement ecranLivraison;
    private final EcranConfirmation      ecranConfirmation;

    /** Bandeau « Panier réservé encore mm:ss », mis à jour chaque seconde. */
    private final JLabel chronoLabel = new JLabel(" ", SwingConstants.CENTER);

    public FenetreFrenchChic(SessionPasserCommande session) {
        this.session    = session;
        this.cardLayout = new CardLayout();
        this.conteneur  = new JPanel(cardLayout);

        setTitle("French Chic");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Création des écrans
        ecranAccueil      = new EcranAccueil(session, this);
        ecranPanier       = new EcranPanier(session, this);
        ecranLivraison    = new EcranLivraisonPaiement(session, this);
        ecranConfirmation = new EcranConfirmation(this);

        conteneur.add(ecranAccueil,      "accueil");
        conteneur.add(ecranPanier,       "panier");
        conteneur.add(ecranLivraison,    "livraison");
        conteneur.add(ecranConfirmation, "confirmation");

        add(conteneur, BorderLayout.CENTER);

        // Chrono de réservation du panier, visible sur tous les écrans
        chronoLabel.setOpaque(true);
        chronoLabel.setBackground(new Color(255, 243, 205));
        chronoLabel.setForeground(new Color(120, 80, 0));
        chronoLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        chronoLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        chronoLabel.setVisible(false);
        add(chronoLabel, BorderLayout.SOUTH);
        new javax.swing.Timer(1000, e -> mettreAJourChrono()).start();
        setPreferredSize(new Dimension(520, 620));
        pack();
        setMinimumSize(new Dimension(480, 560));
        setLocationRelativeTo(null);
    }

    public void afficherEcranAccueil() {
        ecranAccueil.rafraichir();   // stocks à jour après une commande
        cardLayout.show(conteneur, "accueil");
    }

    public void afficherEcranPanier() {
        ecranPanier.rafraichir();
        cardLayout.show(conteneur, "panier");
    }

    public void afficherEcranLivraison() {
        ecranLivraison.rafraichir();
        cardLayout.show(conteneur, "livraison");
    }

    /**
     * Appelé chaque seconde : affiche le temps de réservation restant et,
     * à zéro, vide le panier, remet les articles en vente et prévient le client.
     */
    private void mettreAJourChrono() {
        if (session.panierExpire()) {
            session.expirerPanier();
            chronoLabel.setVisible(false);
            JOptionPane.showMessageDialog(this,
                    "Votre panier a expiré : il était réservé "
                            + session.getDureeReservation().toMinutes() + " minutes.\n"
                            + "Les articles ont été remis en vente.",
                    "Panier expiré", JOptionPane.INFORMATION_MESSAGE);
            afficherEcranAccueil();
            return;
        }
        long s = session.secondesRestantes();
        chronoLabel.setVisible(s >= 0);
        if (s >= 0) {
            chronoLabel.setText(String.format("⏱ Panier réservé encore %d:%02d", s / 60, s % 60));
        }
    }

    /** Affiche un message d'expiration (confirmation refusée) puis le catalogue. */
    public void signalerPanierExpire(String message) {
        chronoLabel.setVisible(false);
        JOptionPane.showMessageDialog(this, message, "Panier expiré", JOptionPane.INFORMATION_MESSAGE);
        afficherEcranAccueil();
    }

    public void afficherEcranConfirmation(ResultatConfirmation resultat) {
        ecranConfirmation.afficher(resultat);
        cardLayout.show(conteneur, "confirmation");
    }
}