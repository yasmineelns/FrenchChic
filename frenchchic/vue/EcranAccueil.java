package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import frenchchic.metier.Produit.StockInsuffisantException;

/**
 * Écran d'accueil du CU "Passer commande en ligne".
 * Affiche le produit du jour (libellé, prix, stock) et permet au client
 * de choisir une quantité avec les boutons "-" et "+", puis d'acheter.
 * Après l'achat, la confirmation et le stock restant s'affichent sur le même écran.
 */
public class EcranAccueil extends JFrame {

    /** Contrôleur du cas d'utilisation, à qui l'écran délègue les actions. */
    private final SessionPasserCommande session;

    private final JLabel libelleLabel = new JLabel();
    private final JLabel prixLabel = new JLabel();
    private final JLabel stockLabel = new JLabel();
    private final JLabel quantiteLabel = new JLabel("1");
    private final JLabel confirmationLabel = new JLabel(" ");

    private final JButton moinsBouton = new JButton("-");
    private final JButton plusBouton = new JButton("+");
    private final JButton acheterBouton = new JButton("Acheter");

    /** Quantité choisie par le client (1 par défaut, jamais en dessous). */
    private int quantite = 1;

    /**
     * Construit l'écran et affiche immédiatement le produit du jour
     * obtenu auprès du contrôleur.
     *
     * @param session le contrôleur du CU "Passer commande en ligne"
     */
    public EcranAccueil(SessionPasserCommande session) {
        this.session = session;

        setTitle("French Chic");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        construireInterface();

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

        setPreferredSize(new Dimension(520, 560));

        pack();

        setMinimumSize(new Dimension(480, 520));

        setLocationRelativeTo(null);
    }

    /** Place les composants : en-tête, infos produit, sélecteur de quantité, bouton, confirmation. */
    private void construireInterface() {

        JLabel titre = new JLabel("FRENCH CHIC", SwingConstants.CENTER);
        titre.setFont(new Font("SansSerif", Font.BOLD, 26));
        titre.setForeground(Color.WHITE);
        titre.setOpaque(true);
        titre.setBackground(new Color(31, 42, 68));
        titre.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        libelleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        libelleLabel.setForeground(new Color(31, 42, 68));
        prixLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        prixLabel.setForeground(new Color(39, 105, 75));
        stockLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        stockLabel.setForeground(new Color(90, 95, 100));
        confirmationLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmationLabel.setForeground(new Color(39, 105, 75));

        JPanel selecteur = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        selecteur.setBackground(Color.WHITE);
        JLabel texteQuantite = new JLabel("Quantité :");
        texteQuantite.setFont(new Font("SansSerif", Font.PLAIN, 15));
        selecteur.add(texteQuantite);
        // Bouton moins
        moinsBouton.setText("-");
        moinsBouton.setFont(new Font("SansSerif", Font.BOLD, 22));
        moinsBouton.setForeground(new Color(31, 42, 68));
        moinsBouton.setBackground(new Color(31, 42, 68));
        moinsBouton.setForeground(Color.WHITE);
        moinsBouton.setPreferredSize(new Dimension(42, 42));
        moinsBouton.setFocusPainted(false);
        moinsBouton.setBorderPainted(false);
        moinsBouton.setContentAreaFilled(true);
        moinsBouton.setOpaque(true);
        moinsBouton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Quantité
        quantiteLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        quantiteLabel.setForeground(new Color(31, 42, 68));
        quantiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        quantiteLabel.setPreferredSize(new Dimension(35, 42));
        // Bouton plus
        plusBouton.setText("+");
        plusBouton.setFont(new Font("DejaVu Sans", Font.BOLD, 22));
        plusBouton.setForeground(Color.WHITE);
        plusBouton.setBackground(new Color(31, 42, 68));
        plusBouton.setPreferredSize(new Dimension(42, 42));
        plusBouton.setBorder(BorderFactory.createEmptyBorder());
        plusBouton.setBorderPainted(false);
        plusBouton.setFocusPainted(false);
        plusBouton.setContentAreaFilled(true);
        plusBouton.setOpaque(true);
        plusBouton.setFont(new Font("SansSerif", Font.BOLD, 22));
        plusBouton.setForeground(Color.WHITE);
        plusBouton.setBackground(new Color(31, 42, 68));
        plusBouton.setPreferredSize(new Dimension(42, 42));
        plusBouton.setFocusPainted(false);
        plusBouton.setBorderPainted(false);
        plusBouton.setContentAreaFilled(true);
        plusBouton.setOpaque(true);
        plusBouton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selecteur.add(moinsBouton);
        selecteur.add(quantiteLabel);
        acheterBouton.setText("Acheter maintenant");

        acheterBouton.setFont(new Font("SansSerif", Font.BOLD, 16));
        acheterBouton.setForeground(Color.WHITE);
        acheterBouton.setBackground(new Color(31, 42, 68));
        acheterBouton.setPreferredSize(
            new Dimension(280, 48)
        );
        acheterBouton.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 48)
        );
        acheterBouton.setFocusPainted(false);
        acheterBouton.setBorder(
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        );
        acheterBouton.setAlignmentX(Component.LEFT_ALIGNMENT);
        selecteur.add(plusBouton);


        JPanel contenu = new JPanel();
        contenu.setLayout(new BoxLayout(contenu, BoxLayout.Y_AXIS));
        contenu.setBackground(new Color(245, 242, 236));
        contenu.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        JPanel carteProduit = new JPanel();
        carteProduit.setLayout(new BoxLayout(carteProduit, BoxLayout.Y_AXIS));
        carteProduit.setBackground(Color.WHITE);
        carteProduit.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(225, 222, 216)
                ),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
            )
        );
        JLabel sousTitre = new JLabel("PRODUIT DU JOUR");

        sousTitre.setFont(
            new Font("SansSerif", Font.BOLD, 12)
        );

        sousTitre.setForeground(
            new Color(120, 115, 105)
        );

        JComponent[] composants = {
            sousTitre,
            libelleLabel,
            prixLabel,
            stockLabel,
            selecteur,
            acheterBouton,
            confirmationLabel
        };

        for (JComponent c : composants) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);

            carteProduit.add(c);

            carteProduit.add(
                Box.createVerticalStrut(12)
            );
        }
        contenu.add(carteProduit);

        setLayout(new BorderLayout());
        add(titre, BorderLayout.NORTH);
        add(contenu, BorderLayout.CENTER);
    }

    /** Modifie la quantité choisie via "-" / "+" sans descendre sous 1. */
    private void changerQuantite(int delta) {
        quantite = Math.max(1, quantite + delta);
        quantiteLabel.setText(String.valueOf(quantite));
    }

    /**
     * Affiche les informations du produit du jour.
     *
     * @param libelle le nom du produit
     * @param prix    le prix unitaire
     * @param stock   la quantité disponible en stock
     */
    public void afficherProduit(String libelle, double prix, int stock) {
        libelleLabel.setText(libelle);
        prixLabel.setText(formaterPrix(prix));
        stockLabel.setText("Stock disponible : " + stock);
    }
    /** Affiche un message d'erreur en rouge à la place de la confirmation. */
    public void afficherErreur(String message) {
        confirmationLabel.setForeground(new Color(180, 40, 40));
        confirmationLabel.setText(message);
}
    

    /**
     * Affiche la confirmation d'achat et met à jour le stock sur le même écran.
     *
     * @param quantite     le nombre d'exemplaires achetés
     * @param stockRestant le stock restant après l'achat
     */
    public void afficherConfirmation(int quantite, int stockRestant) {
    confirmationLabel.setForeground(new Color(39, 105, 75));
    stockLabel.setText("Stock restant : " + stockRestant);
    String exemplaires = quantite > 1 ? " exemplaires achetés" : " exemplaire acheté";
    confirmationLabel.setText("Achat confirmé : " + quantite + exemplaires);
}

    /** Formate un prix à la française, ex. 49.9 -> "49,90 €". */
    private static String formaterPrix(double prix) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(prix);
    }
}
