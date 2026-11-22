package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.controleur.SessionPasserCommande.InfosProduit;
import frenchchic.metier.StockInsuffisantException;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

/**
 * Écran 1 — Liste des produits du catalogue.
 * Affiche pour chaque produit : libellé, prix, stock, sélecteur de quantité.
 * Permet d'ajouter au panier ou d'ouvrir le panier.
 */
public class EcranAccueil extends JPanel {

    private final SessionPasserCommande session;
    private final FenetreFrenchChic fenetre;

    private final JButton ouvrirPanierBouton = new JButton("Ouvrir le panier");

    public EcranAccueil(SessionPasserCommande session, FenetreFrenchChic fenetre) {
        this.session = session;
        this.fenetre = fenetre;
        construireInterface();
    }

    private void construireInterface() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 242, 236));

        // --- En-tête ---
        JPanel entete = new JPanel(new BorderLayout());
        entete.setBackground(new Color(31, 42, 68));

        JLabel titre = new JLabel("FRENCH CHIC", SwingConstants.CENTER);
        titre.setFont(new Font("SansSerif", Font.BOLD, 26));
        titre.setForeground(Color.WHITE);
        titre.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        entete.add(titre, BorderLayout.CENTER);

        styleBoutonAction(ouvrirPanierBouton,
                new Color(225, 222, 216), new Color(31, 42, 68));
        ouvrirPanierBouton.setPreferredSize(new Dimension(160, 40));
        ouvrirPanierBouton.setMaximumSize(new Dimension(160, 40));

        JPanel panierWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panierWrapper.setBackground(new Color(31, 42, 68));
        panierWrapper.add(ouvrirPanierBouton);
        entete.add(panierWrapper, BorderLayout.EAST);

        ouvrirPanierBouton.addActionListener(e -> fenetre.afficherEcranPanier());

        add(entete, BorderLayout.NORTH);

        // --- Corps : une carte par produit ---
        JPanel catalogue = new JPanel();
        catalogue.setLayout(new BoxLayout(catalogue, BoxLayout.Y_AXIS));
        catalogue.setBackground(new Color(245, 242, 236));
        catalogue.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel sousTitre = new JLabel("PRODUITS DU CATALOGUE");
        sousTitre.setFont(new Font("SansSerif", Font.BOLD, 12));
        sousTitre.setForeground(new Color(120, 115, 105));
        sousTitre.setAlignmentX(Component.LEFT_ALIGNMENT);
        catalogue.add(sousTitre);
        catalogue.add(Box.createVerticalStrut(16));

        List<InfosProduit> produits = session.consulterProduits();
        for (InfosProduit p : produits) {
            catalogue.add(creerCarteProduit(p));
            catalogue.add(Box.createVerticalStrut(16));
        }

        JScrollPane scroll = new JScrollPane(catalogue);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(245, 242, 236));
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel creerCarteProduit(InfosProduit p) {

        JLabel libelleLabel = new JLabel(p.libelle());
        libelleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        libelleLabel.setForeground(new Color(31, 42, 68));

        JLabel prixLabel = new JLabel(formaterPrix(p.prix()));
        prixLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        prixLabel.setForeground(new Color(39, 105, 75));

        JLabel stockLabel = new JLabel("Stock disponible : " + p.stock());
        stockLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        stockLabel.setForeground(new Color(90, 95, 100));

        JLabel confirmationLabel = new JLabel(" ");
        confirmationLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Sélecteur de quantité local à cette carte
        int[] quantite = {1};

        JLabel quantiteLabel = new JLabel("1");
        quantiteLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        quantiteLabel.setForeground(new Color(31, 42, 68));
        quantiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        quantiteLabel.setPreferredSize(new Dimension(35, 42));

        JButton moinsBouton = new JButton("-");
        JButton plusBouton  = new JButton("+");
        styleBoutonQuantite(moinsBouton);
        styleBoutonQuantite(plusBouton);

        moinsBouton.addActionListener(e -> {
            quantite[0] = Math.max(1, quantite[0] - 1);
            quantiteLabel.setText(String.valueOf(quantite[0]));
        });
        plusBouton.addActionListener(e -> {
            quantite[0]++;
            quantiteLabel.setText(String.valueOf(quantite[0]));
        });

        JPanel selecteur = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        selecteur.setBackground(Color.WHITE);
        JLabel texteQuantite = new JLabel("Quantité :");
        texteQuantite.setFont(new Font("SansSerif", Font.PLAIN, 14));
        selecteur.add(texteQuantite);
        selecteur.add(moinsBouton);
        selecteur.add(quantiteLabel);
        selecteur.add(plusBouton);

        JButton ajoutPanierBouton = new JButton("Ajouter au panier");
        styleBoutonAction(ajoutPanierBouton, new Color(31, 42, 68), Color.WHITE);

        ajoutPanierBouton.addActionListener(e -> {
            try {
                int nombreArticles = session.ajouterAuPanier(
                        p.reference(), quantite[0]);
                confirmationLabel.setForeground(new Color(39, 105, 75));
                confirmationLabel.setText(quantite[0]
                        + " article(s) ajouté(s) — "
                        + nombreArticles + " article(s) au panier");
                quantite[0] = 1;
                quantiteLabel.setText("1");
            } catch (StockInsuffisantException ex) {
                confirmationLabel.setForeground(new Color(180, 40, 40));
                confirmationLabel.setText(ex.getMessage());
            }
        });

        // Assemblage de la carte
        JPanel carte = new JPanel();
        carte.setLayout(new BoxLayout(carte, BoxLayout.Y_AXIS));
        carte.setBackground(Color.WHITE);
        carte.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 222, 216)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        carte.setAlignmentX(Component.LEFT_ALIGNMENT);
        carte.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        for (JComponent c : new JComponent[]{
                libelleLabel, prixLabel, stockLabel,
                selecteur, ajoutPanierBouton, confirmationLabel}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            carte.add(c);
            carte.add(Box.createVerticalStrut(10));
        }

        return carte;
    }

    private void styleBoutonQuantite(JButton b) {
        b.setFont(new Font("SansSerif", Font.BOLD, 22));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(31, 42, 68));
        b.setPreferredSize(new Dimension(42, 42));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleBoutonAction(JButton b, Color bg, Color fg) {
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
    }

    private static String formaterPrix(double prix) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(prix);
    }
}