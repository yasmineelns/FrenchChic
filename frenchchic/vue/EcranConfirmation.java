package frenchchic.vue;

import frenchchic.controleur.SessionPasserCommande.ResultatConfirmation;
import frenchchic.metier.LignePanier;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Écran 4 — Confirmation d'achat.
 * Affiche le numéro de transaction, le récapitulatif, le total à payer
 * et le stock restant de chaque produit acheté.
 */
public class EcranConfirmation extends JPanel {

    private final FenetreFrenchChic fenetre;

    // Zone dynamique rechargée à chaque confirmation
    private final JPanel contenuDynamique = new JPanel();

    public EcranConfirmation(FenetreFrenchChic fenetre) {
        this.fenetre = fenetre;
        construireInterface();
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

        // --- Corps scrollable ---
        contenuDynamique.setLayout(new BoxLayout(
                contenuDynamique, BoxLayout.Y_AXIS));
        contenuDynamique.setBackground(new Color(245, 242, 236));
        contenuDynamique.setBorder(
                BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JScrollPane scroll = new JScrollPane(contenuDynamique);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(245, 242, 236));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Remplit l'écran avec les données de la commande confirmée.
     * À appeler depuis FenetreFrenchChic après la confirmation.
     *
     * @param resultat résultat renvoyé par SessionPasserCommande.confirmerCommande()
     */
    public void afficher(ResultatConfirmation resultat) {
        contenuDynamique.removeAll();

        // --- Bandeau succès ---
        JLabel succes = new JLabel("✔  Achat confirmé !");
        succes.setFont(new Font("SansSerif", Font.BOLD, 20));
        succes.setForeground(new Color(39, 105, 75));
        succes.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenuDynamique.add(succes);
        contenuDynamique.add(Box.createVerticalStrut(20));

        // --- Numéro de transaction ---
        contenuDynamique.add(sectionLabel("NUMÉRO DE TRANSACTION"));
        contenuDynamique.add(Box.createVerticalStrut(10));

        JPanel carteNumero = carte();
        JLabel numeroLabel = new JLabel(
                resultat.commande().getNumeroTransaction());
        numeroLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        numeroLabel.setForeground(new Color(31, 42, 68));
        numeroLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        carteNumero.add(numeroLabel);
        contenuDynamique.add(carteNumero);
        contenuDynamique.add(Box.createVerticalStrut(20));

        // --- Récapitulatif des lignes + stocks restants ---
        contenuDynamique.add(sectionLabel("RÉCAPITULATIF"));
        contenuDynamique.add(Box.createVerticalStrut(10));

        JPanel carteLignes = carte();
        List<LignePanier> lignes = resultat.commande().getLignes();
        List<Integer> stocks     = resultat.stocksRestants();

        for (int i = 0; i < lignes.size(); i++) {
            LignePanier ligne = lignes.get(i);
            int stockRestant  = stocks.get(i);

            // Ligne : libellé × quantité — sous-total
            JLabel ligneLabel = new JLabel(String.format(
                    "%s  ×  %d  —  %.2f €",
                    ligne.getProduit().getLibelle(),
                    ligne.getQuantite(),
                    ligne.getSousTotal()).replace(".", ","));
            ligneLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            ligneLabel.setForeground(new Color(31, 42, 68));
            ligneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Stock restant
            JLabel stockLabel = new JLabel(
                    "   Stock restant : " + stockRestant);
            stockLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            stockLabel.setForeground(new Color(90, 95, 100));
            stockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            carteLignes.add(ligneLabel);
            carteLignes.add(stockLabel);
            carteLignes.add(Box.createVerticalStrut(10));
        }

        // Séparateur + total
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(225, 222, 216));
        carteLignes.add(sep);
        carteLignes.add(Box.createVerticalStrut(10));

        JLabel totalLabel = new JLabel(String.format(
                "Total payé à la livraison : %.2f €",
                resultat.commande().getTotal()).replace(".", ","));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setForeground(new Color(39, 105, 75));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        carteLignes.add(totalLabel);

        contenuDynamique.add(carteLignes);
        contenuDynamique.add(Box.createVerticalStrut(20));

        // --- Mode de paiement ---
        contenuDynamique.add(sectionLabel("MODE DE PAIEMENT"));
        contenuDynamique.add(Box.createVerticalStrut(10));

        JPanel cartePaiement = carte();
        JLabel paiementLabel = new JLabel("🚚  "
                + resultat.commande().getModePaiement());
        paiementLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        paiementLabel.setForeground(new Color(31, 42, 68));
        paiementLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cartePaiement.add(paiementLabel);
        contenuDynamique.add(cartePaiement);
        contenuDynamique.add(Box.createVerticalStrut(24));

        // --- Bouton retour catalogue ---
        JButton retourBouton = new JButton("Retour au catalogue");
        retourBouton.setFont(new Font("SansSerif", Font.BOLD, 15));
        retourBouton.setBackground(new Color(31, 42, 68));
        retourBouton.setForeground(Color.WHITE);
        retourBouton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        retourBouton.setAlignmentX(Component.LEFT_ALIGNMENT);
        retourBouton.setFocusPainted(false);
        retourBouton.setBorderPainted(false);
        retourBouton.setOpaque(true);
        retourBouton.addActionListener(e -> fenetre.afficherEcranAccueil());
        contenuDynamique.add(retourBouton);

        contenuDynamique.revalidate();
        contenuDynamique.repaint();
    }

    // -------------------------------------------------------------------------

    private static JLabel sectionLabel(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(120, 115, 105));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel carte() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 222, 216)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }
}