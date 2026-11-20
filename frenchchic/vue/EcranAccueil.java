package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.metier.Produit.StockInsuffisantException;
import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Écran 1 — Liste des produits du catalogue.
 * Affiche pour chaque produit : libellé, prix, stock, sélecteur de quantité.
 * Permet d'ajouter au panier, d'ouvrir le panier ou d'acheter directement.
 */
public class EcranAccueil extends JPanel {

    private final SessionPasserCommande session;
    private final FenetreFrenchChic fenetre;

    private final JLabel libelleLabel      = new JLabel();
    private final JLabel prixLabel         = new JLabel();
    private final JLabel stockLabel        = new JLabel();
    private final JLabel quantiteLabel     = new JLabel("1");
    private final JLabel confirmationLabel = new JLabel(" ");

    private final JButton moinsBouton       = new JButton("-");
    private final JButton plusBouton        = new JButton("+");
    private final JButton acheterBouton     = new JButton("Acheter maintenant");
    private final JButton ajoutPanierBouton = new JButton("Ajouter au panier");
    private final JButton ouvrirPanierBouton = new JButton("Ouvrir le panier");

    private int quantite = 1;

    public EcranAccueil(SessionPasserCommande session, FenetreFrenchChic fenetre) {
        this.session = session;
        this.fenetre = fenetre;

        construireInterface();

        // Affichage du produit — à remplacer par une liste quand SessionPasserCommande
        // exposera consulterProduits() renvoyant List<InfosProduit>
        SessionPasserCommande.InfosProduit p = session.consulterProduitDuJour();
        afficherProduit(p.libelle(), p.prix(), p.stock());

        moinsBouton.addActionListener(e -> changerQuantite(-1));
        plusBouton.addActionListener(e -> changerQuantite(+1));

        acheterBouton.addActionListener(e -> {
            try {
                int stockRestant = session.acheter(quantite);
                afficherConfirmation(quantite, stockRestant);
            } catch (StockInsuffisantException ex) {
                afficherErreur(ex.getMessage());
            }
        });

        ajoutPanierBouton.addActionListener(e -> {
            // TODO : session.ajouterAuPanier(quantite)
            fenetre.afficherEcranPanier();
        });

        ouvrirPanierBouton.addActionListener(e -> fenetre.afficherEcranPanier());
    }

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

        // --- Infos produit ---
        libelleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        libelleLabel.setForeground(new Color(31, 42, 68));

        prixLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        prixLabel.setForeground(new Color(39, 105, 75));

        stockLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        stockLabel.setForeground(new Color(90, 95, 100));

        confirmationLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmationLabel.setForeground(new Color(39, 105, 75));

        // --- Sélecteur de quantité ---
        JPanel selecteur = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        selecteur.setBackground(Color.WHITE);

        JLabel texteQuantite = new JLabel("Quantité :");
        texteQuantite.setFont(new Font("SansSerif", Font.PLAIN, 15));

        styleBoutonQuantite(moinsBouton);
        styleBoutonQuantite(plusBouton);

        quantiteLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        quantiteLabel.setForeground(new Color(31, 42, 68));
        quantiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        quantiteLabel.setPreferredSize(new Dimension(35, 42));

        selecteur.add(texteQuantite);
        selecteur.add(moinsBouton);
        selecteur.add(quantiteLabel);
        selecteur.add(plusBouton);

        // --- Boutons d'action ---
        styleBoutonAction(acheterBouton,      new Color(31, 42, 68), Color.WHITE);
        styleBoutonAction(ajoutPanierBouton,  new Color(31, 42, 68), Color.WHITE);
        styleBoutonAction(ouvrirPanierBouton, new Color(225, 222, 216), new Color(31, 42, 68));

        // --- Carte produit ---
        JLabel sousTitre = new JLabel("PRODUITS DU CATALOGUE");
        sousTitre.setFont(new Font("SansSerif", Font.BOLD, 12));
        sousTitre.setForeground(new Color(120, 115, 105));

        JPanel carteProduit = new JPanel();
        carteProduit.setLayout(new BoxLayout(carteProduit, BoxLayout.Y_AXIS));
        carteProduit.setBackground(Color.WHITE);
        carteProduit.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 222, 216)),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        JComponent[] composants = {
            sousTitre,
            libelleLabel,
            prixLabel,
            stockLabel,
            selecteur,
            acheterBouton,
            ajoutPanierBouton,
            ouvrirPanierBouton,
            confirmationLabel
        };

        for (JComponent c : composants) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            carteProduit.add(c);
            carteProduit.add(Box.createVerticalStrut(12));
        }

        JPanel contenu = new JPanel();
        contenu.setLayout(new BoxLayout(contenu, BoxLayout.Y_AXIS));
        contenu.setBackground(new Color(245, 242, 236));
        contenu.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        contenu.add(carteProduit);

        add(contenu, BorderLayout.CENTER);
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
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
    }

    private void changerQuantite(int delta) {
        quantite = Math.max(1, quantite + delta);
        quantiteLabel.setText(String.valueOf(quantite));
    }

    public void afficherProduit(String libelle, double prix, int stock) {
        libelleLabel.setText(libelle);
        prixLabel.setText(formaterPrix(prix));
        stockLabel.setText("Stock disponible : " + stock);
    }

    public void afficherErreur(String message) {
        confirmationLabel.setForeground(new Color(180, 40, 40));
        confirmationLabel.setText(message);
    }

    public void afficherConfirmation(int quantite, int stockRestant) {
        confirmationLabel.setForeground(new Color(39, 105, 75));
        stockLabel.setText("Stock restant : " + stockRestant);
        String exemplaires = quantite > 1 ? " exemplaires achetés" : " exemplaire acheté";
        confirmationLabel.setText("Achat confirmé : " + quantite + exemplaires);
    }

    private static String formaterPrix(double prix) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(prix);
    }
}