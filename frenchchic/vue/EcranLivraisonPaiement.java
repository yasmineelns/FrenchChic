package frenchchic.vue;

import frenchchic.controleur.ChampLivraisonInvalideException;
import frenchchic.controleur.SessionPasserCommande;
import frenchchic.controleur.SessionPasserCommande.ResultatConfirmation;
import frenchchic.metier.StockInsuffisantException;
import java.awt.*;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

/**
 * Écran 3 — Livraison et paiement.
 * Saisie des informations de livraison, affichage du mode de paiement
 * à la livraison, récapitulatif du panier et signalement des champs invalides.
 */
public class EcranLivraisonPaiement extends JPanel {

    private final SessionPasserCommande session;
    private final FenetreFrenchChic fenetre;

    // Champs de saisie
    private final JTextField nomField     = new JTextField();
    private final JTextField prenomField  = new JTextField();
    private final JTextField telField     = new JTextField();
    private final JTextField emailField   = new JTextField();
    private final JTextField adresseField = new JTextField();

    // Labels d'erreur sous chaque champ
    private final JLabel errNom     = erreurLabel();
    private final JLabel errPrenom  = erreurLabel();
    private final JLabel errTel     = erreurLabel();
    private final JLabel errEmail   = erreurLabel();
    private final JLabel errAdresse = erreurLabel();

    // Récapitulatif
    private final JLabel recapLabel = new JLabel(" ");
    private final JLabel totalLabel = new JLabel(" ");

    // Boutons
    private final JButton retourBouton    = new JButton("← Retour au panier");
    private final JButton confirmerBouton = new JButton("Confirmer la commande");

    public EcranLivraisonPaiement(SessionPasserCommande session,
                                   FenetreFrenchChic fenetre) {
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

        corps.add(sectionLabel("INFORMATIONS DE LIVRAISON"));
        corps.add(Box.createVerticalStrut(12));
        corps.add(carteFormulaire());
        corps.add(Box.createVerticalStrut(20));

        corps.add(sectionLabel("MODE DE PAIEMENT"));
        corps.add(Box.createVerticalStrut(12));
        corps.add(cartePaiement());
        corps.add(Box.createVerticalStrut(20));

        corps.add(sectionLabel("RÉCAPITULATIF"));
        corps.add(Box.createVerticalStrut(12));
        corps.add(carteRecap());
        corps.add(Box.createVerticalStrut(24));

        styleBouton(retourBouton,    new Color(225, 222, 216), new Color(31, 42, 68));
        styleBouton(confirmerBouton, new Color(31, 42, 68),    Color.WHITE);
        retourBouton.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmerBouton.setAlignmentX(Component.LEFT_ALIGNMENT);

        corps.add(retourBouton);
        corps.add(Box.createVerticalStrut(10));
        corps.add(confirmerBouton);

        JScrollPane scroll = new JScrollPane(corps);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(245, 242, 236));
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel carteFormulaire() {
        JPanel carte = carte();

        JTextField[] fields  = {nomField, prenomField, telField,
                                 emailField, adresseField};
        JLabel[]     erreurs = {errNom, errPrenom, errTel,
                                 errEmail, errAdresse};
        String[]     labels  = {
            "Nom *",
            "Prénom *",
            "Téléphone * (10 chiffres)",
            "Email *",
            "Adresse *"
        };

        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            fields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            fields[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            fields[i].setFont(new Font("SansSerif", Font.PLAIN, 14));

            erreurs[i].setAlignmentX(Component.LEFT_ALIGNMENT);

            carte.add(label);
            carte.add(Box.createVerticalStrut(4));
            carte.add(fields[i]);
            carte.add(erreurs[i]);
            carte.add(Box.createVerticalStrut(10));
        }
        return carte;
    }

    private JPanel cartePaiement() {
        JPanel carte = carte();

        JLabel icone = new JLabel("🚚  Paiement à la livraison");
        icone.setFont(new Font("SansSerif", Font.BOLD, 15));
        icone.setForeground(new Color(31, 42, 68));
        icone.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detail = new JLabel(
                "Vous réglez le montant lors de la réception de votre commande.");
        detail.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detail.setForeground(new Color(90, 95, 100));
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        carte.add(icone);
        carte.add(Box.createVerticalStrut(6));
        carte.add(detail);
        return carte;
    }

    private JPanel carteRecap() {
        JPanel carte = carte();

        recapLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        recapLabel.setForeground(new Color(31, 42, 68));
        recapLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setForeground(new Color(39, 105, 75));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        carte.add(recapLabel);
        carte.add(Box.createVerticalStrut(8));
        carte.add(totalLabel);
        return carte;
    }

    // -------------------------------------------------------------------------

    private void ajouterListeners() {
        retourBouton.addActionListener(e -> fenetre.afficherEcranPanier());

        confirmerBouton.addActionListener(e -> {
            effacerErreurs();

            Map<String, String> champs = new LinkedHashMap<>();
            champs.put("nom",        nomField.getText().trim());
            champs.put("prenom",     prenomField.getText().trim());
            champs.put("telephone",  telField.getText().trim());
            champs.put("email",      emailField.getText().trim());
            champs.put("adresse",    adresseField.getText().trim());

            try {
                ResultatConfirmation resultat = session.confirmerCommande(champs);
                fenetre.afficherEcranConfirmation(resultat);

            } catch (ChampLivraisonInvalideException ex) {
                for (String erreur : ex.getErreurs()) {
                    String lower = erreur.toLowerCase();
                    if (lower.contains("nom"))
                        errNom.setText(erreur);
                    else if (lower.contains("prénom") || lower.contains("prenom"))
                        errPrenom.setText(erreur);
                    else if (lower.contains("téléphone") || lower.contains("telephone"))
                        errTel.setText(erreur);
                    else if (lower.contains("email"))
                        errEmail.setText(erreur);
                    else if (lower.contains("adresse"))
                        errAdresse.setText(erreur);
                }

            } catch (StockInsuffisantException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Stock insuffisant",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    /**
     * Recharge le récapitulatif depuis le panier courant.
     * À appeler depuis FenetreFrenchChic avant d'afficher cet écran.
     */
    public void rafraichir() {
        effacerErreurs();
        SessionPasserCommande.RecapPanier recap = session.consulterPanier();

        StringBuilder sb = new StringBuilder("<html>");
        for (var ligne : recap.lignes()) {
            sb.append(ligne.getProduit().getLibelle())
              .append(" × ").append(ligne.getQuantite())
              .append("<br>");
        }
        sb.append("</html>");
        recapLabel.setText(sb.toString());

        totalLabel.setText(
                NumberFormat.getCurrencyInstance(Locale.FRANCE)
                            .format(recap.total()));
    }

    // -------------------------------------------------------------------------
    // Helpers

    private void effacerErreurs() {
        for (JLabel l : new JLabel[]{
                errNom, errPrenom, errTel, errEmail, errAdresse}) {
            l.setText(" ");
        }
    }

    private static JLabel erreurLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("SansSerif", Font.ITALIC, 11));
        l.setForeground(new Color(180, 40, 40));
        return l;
    }

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

    private static void styleBouton(JButton b, Color bg, Color fg) {
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
    }
}