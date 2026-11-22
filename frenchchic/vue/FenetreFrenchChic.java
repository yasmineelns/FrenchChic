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

        add(conteneur);
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

    public void afficherEcranConfirmation(ResultatConfirmation resultat) {
        ecranConfirmation.afficher(resultat);
        cardLayout.show(conteneur, "confirmation");
    }
}