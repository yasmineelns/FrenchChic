package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.controleur.SessionPasserCommande.RecapPanier;
import frenchchic.metier.LignePanier;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

/**
 * Écran 2 — Panier.
 * Affiche les lignes du panier, les sous-totaux et le total.
 * Permet de modifier une quantité, retirer un produit, vider le panier,
 * continuer les achats ou valider le panier.
 */
public class EcranPanier extends JPanel {

    private final SessionPasserCommande session;
    private final FenetreFrenchChic fenetre;

    // Zone dynamique rechargée à chaque affichage
    private final JPanel lignesPanel = new JPanel();
    private final JLabel totalLabel  = new JLabel("Total : 0,00 €");
    private final JLabel videLabel   = new JLabel("Votre panier est vide.");

    // Boutons fixes
    private final JButton continuerBouton = new JButton("← Continuer les achats");
    private final JButton viderBouton     = new JButton("Vider le panier");
    private final JButton validerBouton   = new JButton("Valider le panier");

    public EcranPanier(SessionPasserCommande session, FenetreFrenchChic fenetre) {
        this.session = session;
        this.fenetre = fenetre;
        construireInterface();
        ajouterListeners();
    }

    // -------------------------------------------------------------------------

    private void construireInterface() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 242, 236));

        // --- En-tête ---
        JLabel titre = new JLabel("FRENCH CHIC", SwingConstants.CENTER);
        titre.setFont(new Font("SansSerif", Font.BOLD, 26));
        titre.setForeground(Color.WHITE);
        titre.setOpaque(true);
        titre.setBackground(new Color(31, 42, 68));
        titre.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(titre, BorderLayout.NORTH);

        // --- Corps scrollable ---
        JPanel corps = new JPanel();
        corps.setLayout(new BoxLayout(corps, BoxLayout.Y_AXIS));
        corps.setBackground(new Color(245, 242, 236));
        corps.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // Sous-titre
        JLabel sousTitre = new JLabel("MON PANIER");
        sousTitre.setFont(new Font("SansSerif", Font.BOLD, 12));
        sousTitre.setForeground(new Color(120, 115, 105));
        sousTitre.setAlignmentX(Component.LEFT_ALIGNMENT);
        corps.add(sousTitre);
        corps.add(Box.createVerticalStrut(12));

        // Message panier vide
        videLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        videLabel.setForeground(new Color(90, 95, 100));
        videLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        videLabel.setVisible(false);
        corps.add(videLabel);

        // Zone des lignes
        lignesPanel.setLayout(new BoxLayout(lignesPanel, BoxLayout.Y_AXIS));
        lignesPanel.setBackground(new Color(245, 242, 236));
        lignesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        corps.add(lignesPanel);
        corps.add(Box.createVerticalStrut(16));

        // Total
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalLabel.setForeground(new Color(31, 42, 68));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        corps.add(totalLabel);
        corps.add(Box.createVerticalStrut(24));

        // Boutons
        styleBouton(continuerBouton, new Color(225, 222, 216), new Color(31, 42, 68));
        styleBouton(viderBouton,     new Color(225, 222, 216), new Color(31, 42, 68));
        styleBouton(validerBouton,   new Color(31, 42, 68),    Color.WHITE);

        continuerBouton.setAlignmentX(Component.LEFT_ALIGNMENT);
        viderBouton.setAlignmentX(Component.LEFT_ALIGNMENT);
        validerBouton.setAlignmentX(Component.LEFT_ALIGNMENT);

        corps.add(continuerBouton);
        corps.add(Box.createVerticalStrut(10));
        corps.add(viderBouton);
        corps.add(Box.createVerticalStrut(10));
        corps.add(validerBouton);

        JScrollPane scroll = new JScrollPane(corps);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(245, 242, 236));
        add(scroll, BorderLayout.CENTER);
    }

    private void ajouterListeners() {
        continuerBouton.addActionListener(e -> fenetre.afficherEcranAccueil());

        viderBouton.addActionListener(e -> {
            session.viderPanier();
            rafraichir();
        });

        validerBouton.addActionListener(e -> fenetre.afficherEcranLivraison());
    }

    // -------------------------------------------------------------------------

    /**
     * Recharge l'affichage du panier depuis le contrôleur.
     * Appelé par FenetreFrenchChic à chaque navigation vers cet écran.
     */
    public void rafraichir() {
        lignesPanel.removeAll();

        RecapPanier recap = session.consulterPanier();
        List<LignePanier> lignes = recap.lignes();

        if (lignes.isEmpty()) {
            videLabel.setVisible(true);
            totalLabel.setVisible(false);
            viderBouton.setEnabled(false);
            validerBouton.setEnabled(false);
        } else {
            videLabel.setVisible(false);
            totalLabel.setVisible(true);
            viderBouton.setEnabled(true);
            validerBouton.setEnabled(true);

            for (LignePanier ligne : lignes) {
                lignesPanel.add(creerCarteLigne(ligne));
                lignesPanel.add(Box.createVerticalStrut(10));
            }

            totalLabel.setText("Total : "
                    + NumberFormat.getCurrencyInstance(Locale.FRANCE)
                                  .format(recap.total()));
        }

        lignesPanel.revalidate();
        lignesPanel.repaint();
    }

    /**
     * Crée une carte pour une ligne du panier avec
     * sélecteur de quantité et bouton supprimer.
     */
    private JPanel creerCarteLigne(LignePanier ligne) {

        JPanel carte = new JPanel();
        carte.setLayout(new BoxLayout(carte, BoxLayout.Y_AXIS));
        carte.setBackground(Color.WHITE);
        carte.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 222, 216)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        carte.setAlignmentX(Component.LEFT_ALIGNMENT);
        carte.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Libellé
        JLabel libelleLabel = new JLabel(ligne.getProduit().getLibelle());
        libelleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        libelleLabel.setForeground(new Color(31, 42, 68));
        libelleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Sous-total
        JLabel sousTotal = new JLabel(
                NumberFormat.getCurrencyInstance(Locale.FRANCE)
                            .format(ligne.getSousTotal()));
        sousTotal.setFont(new Font("SansSerif", Font.BOLD, 15));
        sousTotal.setForeground(new Color(39, 105, 75));
        sousTotal.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Ligne quantité + bouton supprimer
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setBackground(Color.WHITE);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton moinsBouton = new JButton("-");
        JLabel  quantiteLabel = new JLabel(
                String.valueOf(ligne.getQuantite()));
        JButton plusBouton  = new JButton("+");
        JButton supprimerBouton = new JButton("🗑 Retirer");

        styleBoutonQuantite(moinsBouton);
        styleBoutonQuantite(plusBouton);

        quantiteLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        quantiteLabel.setForeground(new Color(31, 42, 68));
        quantiteLabel.setPreferredSize(new Dimension(30, 42));
        quantiteLabel.setHorizontalAlignment(SwingConstants.CENTER);

        supprimerBouton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        supprimerBouton.setForeground(new Color(180, 40, 40));
        supprimerBouton.setBackground(Color.WHITE);
        supprimerBouton.setBorderPainted(false);
        supprimerBouton.setFocusPainted(false);
        supprimerBouton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        actions.add(moinsBouton);
        actions.add(quantiteLabel);
        actions.add(plusBouton);
        actions.add(Box.createHorizontalStrut(16));
        actions.add(supprimerBouton);

        // Listeners
        moinsBouton.addActionListener(e -> {
            int nouvelleQte = ligne.getQuantite() - 1;
            if (nouvelleQte < 1) return;
            session.modifierQuantitePanier(
                    ligne.getProduit().getLibelle(), nouvelleQte);
            rafraichir();
        });

        plusBouton.addActionListener(e -> {
            int nouvelleQte = ligne.getQuantite() + 1;
            session.modifierQuantitePanier(
                    ligne.getProduit().getLibelle(), nouvelleQte);
            rafraichir();
        });

        supprimerBouton.addActionListener(e -> {
            session.retirerDuPanier(ligne.getProduit().getLibelle());
            rafraichir();
        });

        carte.add(libelleLabel);
        carte.add(Box.createVerticalStrut(6));
        carte.add(sousTotal);
        carte.add(Box.createVerticalStrut(10));
        carte.add(actions);

        return carte;
    }

    // -------------------------------------------------------------------------
    // Helpers

    private static void styleBouton(JButton b, Color bg, Color fg) {
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
    }

    private static void styleBoutonQuantite(JButton b) {
        b.setFont(new Font("SansSerif", Font.BOLD, 20));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(31, 42, 68));
        b.setPreferredSize(new Dimension(38, 38));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}