package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

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
            int stockRestant = session.acheter(quantite);
            afficherConfirmation(quantite, stockRestant);
        });

        pack();
        setLocationRelativeTo(null);
    }

    /** Place les composants : en-tête, infos produit, sélecteur de quantité, bouton, confirmation. */
    private void construireInterface() {
        JLabel titre = new JLabel("FRENCH CHIC", SwingConstants.CENTER);
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 24f));
        titre.setOpaque(true);
        titre.setBackground(Color.GRAY);
        titre.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        libelleLabel.setFont(libelleLabel.getFont().deriveFont(Font.BOLD));
        prixLabel.setForeground(new Color(0, 128, 0));
        confirmationLabel.setForeground(new Color(0, 100, 0));

        JPanel selecteur = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        selecteur.add(new JLabel("Quantité :"));
        selecteur.add(moinsBouton);
        selecteur.add(quantiteLabel);
        selecteur.add(plusBouton);

        JPanel contenu = new JPanel();
        contenu.setLayout(new BoxLayout(contenu, BoxLayout.Y_AXIS));
        contenu.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        for (JComponent c : new JComponent[] {
                new JLabel("Produit du jour"), libelleLabel, prixLabel, stockLabel,
                selecteur, acheterBouton, confirmationLabel }) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            contenu.add(c);
            contenu.add(Box.createVerticalStrut(6));
        }

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

    /**
     * Affiche la confirmation d'achat et met à jour le stock sur le même écran.
     *
     * @param quantite     le nombre d'exemplaires achetés
     * @param stockRestant le stock restant après l'achat
     */
    public void afficherConfirmation(int quantite, int stockRestant) {
        stockLabel.setText("Stock restant : " + stockRestant);
        String exemplaires = quantite > 1 ? " exemplaires achetés" : " exemplaire acheté";
        confirmationLabel.setText("Achat confirmé : " + quantite + exemplaires);
    }

    /** Formate un prix à la française, ex. 49.9 -> "49,90 €". */
    private static String formaterPrix(double prix) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(prix);
    }
}
