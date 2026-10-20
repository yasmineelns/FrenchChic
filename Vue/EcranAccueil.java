package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande;
import frenchchic.metier.Produit;
import javax.swing.*;
import java.awt.*;

public class EcranAccueil extends JFrame {

    SessionPasserCommande session;
    JLabel infos = new JLabel();
    JSpinner quantite = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    JButton acheter = new JButton("Acheter");

    public EcranAccueil(SessionPasserCommande s) {
        session = s;
        setTitle("French Chic");
        setSize(350, 120);
        setLayout(new FlowLayout());
        add(infos);
        add(quantite);
        add(acheter);

        var p = session.consulterProduitDuJour();
        afficherProduit(p.libelle(), p.prix(), p.stock());

        // clic sur acheter
        acheter.addActionListener(e -> {
            int q = (int) quantite.getValue();
            int stock = session.acheter(q);
            afficherConfirmation(q, stock);
        });
    }

    public void afficherProduit(String libelle, double prix, int stock) {
        infos.setText(libelle + " - " + prix + " euros - stock : " + stock);
    }

    public void afficherConfirmation(int q, int stock) {
        JOptionPane.showMessageDialog(this, "Achat confirmé (" + q + "), il reste " + stock);
    }

    public static void main(String[] args) {
        Produit p = new Produit("Robe", 49.9, 10);
        new EcranAccueil(new SessionPasserCommande(p)).setVisible(true);
    }
}