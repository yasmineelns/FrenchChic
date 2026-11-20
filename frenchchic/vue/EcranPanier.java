package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import javax.swing.*;
import java.awt.*;

/**
 * Écran 2 — Panier.
 * Affiche les lignes du panier, les sous-totaux et le total.
 * Permet de modifier une quantité, retirer un produit, vider le panier,
 * continuer les achats ou valider le panier.
 */
public class EcranPanier extends JPanel {

    private final SessionPasserCommande session;
    private final FenetreFrenchChic fenetre;

    // Labels dynamiques
    private final JPanel lignesPanel = new JPanel();
    private final JLabel totalLabel = new JLabel("Total : 0,00 €");

    // Boutons
    private final JButton continuerBouton  = new JButton("Continuer les achats");
    private final JButton viderBouton      = new JButton("Vider le panier");
    private final JButton validerBouton    = new JButton("Valider le panier");

    public EcranPanier(SessionPasserCommande session, FenetreFrenchChic fenetre) {
        this.session = session;
        this.fenetre = fenetre;
        construireInterface();
        ajouterListeners();
    }

    private void construireInterface() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 242, 236));

        // En-tête
        JLabel titre = new JLabel("FRENCH CHIC", SwingConstants.CENTER);
        titre.setFont(new Font("SansSerif", Font.BOLD, 26));
        titre.setForeground(Color.WHITE);
        titre.setOpaque(true);
        titre.setBackground(new Color(31, 42, 68));
        titre.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(titre, BorderLayout.NORTH);

        // Corps : liste des lignes + total
        JPanel corps = new JPanel();
        corps.setLayout(new BoxLayout(corps, BoxLayout.Y_AXIS));
        corps.setBackground(new Color(245, 242, 236));
        corps.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel sousTitre = new JLabel("MON PANIER");
        sousTitre.setFont(new Font("SansSerif", Font.BOLD, 12));
        sousTitre.setForeground(new Color(120, 115, 105));
        corps.add(sousTitre);
        corps.add(Box.createVerticalStrut(12));

        lignesPanel.setLayout(new BoxLayout(lignesPanel, BoxLayout.Y_AXIS));
        lignesPanel.setBackground(Color.WHITE);
        corps.add(lignesPanel);
        corps.add(Box.createVerticalStrut(12));

        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalLabel.setForeground(new Color(31, 42, 68));
        corps.add(totalLabel);
        corps.add(Box.createVerticalStrut(20));

        // Boutons
        styleNavBouton(continuerBouton, new Color(225, 222, 216), new Color(31, 42, 68));
        styleNavBouton(viderBouton,     new Color(225, 222, 216), new Color(31, 42, 68));
        styleNavBouton(validerBouton,   new Color(31, 42, 68),    Color.WHITE);

        corps.add(continuerBouton);
        corps.add(Box.createVerticalStrut(8));
        corps.add(viderBouton);
        corps.add(Box.createVerticalStrut(8));
        corps.add(validerBouton);

        add(new JScrollPane(corps), BorderLayout.CENTER);
    }

    private void styleNavBouton(JButton b, Color bg, Color fg) {
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
    }

    private void ajouterListeners() {
        continuerBouton.addActionListener(e -> fenetre.afficherEcranAccueil());
        viderBouton.addActionListener(e -> {
            // TODO : session.viderPanier()
            rafraichir();
        });
        validerBouton.addActionListener(e -> fenetre.afficherEcranLivraison());
    }

    /**
     * Recharge l'affichage du panier depuis le contrôleur.
     * À appeler chaque fois qu'on navigue vers cet écran.
     */
    public void rafraichir() {
        lignesPanel.removeAll();
        // TODO : récupérer session.consulterPanier() et afficher les lignes
        totalLabel.setText("Total : 0,00 €"); // placeholder
        lignesPanel.revalidate();
        lignesPanel.repaint();
    }
}