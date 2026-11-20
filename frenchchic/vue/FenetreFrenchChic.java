package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre unique de l'application. Gère la navigation entre les écrans 1 à 4.
 */
public class FenetreFrenchChic extends JFrame {

    private final SessionPasserCommande session;
    private final JPanel conteneur;
    private final CardLayout cardLayout;

    public FenetreFrenchChic(SessionPasserCommande session) {
        this.session = session;
        this.cardLayout = new CardLayout();
        this.conteneur = new JPanel(cardLayout);

        setTitle("French Chic");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(conteneur);

        // Ajout des écrans (EcranAccueil est déjà prêt, les autres à créer)
        conteneur.add(new EcranAccueil(session, this), "accueil");
        // conteneur.add(new EcranPanier(session, this), "panier");   // sprint 2
        // conteneur.add(new EcranLivraisonPaiement(session, this), "livraison");
        // conteneur.add(new EcranConfirmation(session, this), "confirmation");

        setPreferredSize(new Dimension(520, 560));
        pack();
        setMinimumSize(new Dimension(480, 520));
        setLocationRelativeTo(null);
    }

    public void afficherEcranAccueil()    { cardLayout.show(conteneur, "accueil"); }
    public void afficherEcranPanier()     { cardLayout.show(conteneur, "panier"); }
    public void afficherEcranLivraison()  { cardLayout.show(conteneur, "livraison"); }
    public void afficherEcranConfirmation() { cardLayout.show(conteneur, "confirmation"); }
}