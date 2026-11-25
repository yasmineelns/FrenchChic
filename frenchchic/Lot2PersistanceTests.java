import frenchchic.controleur.SessionPasserCommande;
import frenchchic.metier.Commande;
import frenchchic.metier.InfosLivraison;
import frenchchic.metier.LignePanier;
import frenchchic.metier.Produit;
import frenchchic.metier.persistance.ConnexionBD;
import frenchchic.metier.persistance.DepotCommandes;
import frenchchic.metier.persistance.DepotProduits;
import frenchchic.metier.persistance.PersistanceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests du lot 2 — persistance JDBC ({@link DepotProduits}, {@link DepotCommandes}).
 *
 * <p>Ils s'exécutent sur une base de TEST séparée, vidée au début de chaque
 * test (les commandes ne peuvent pas être supprimées de la vraie base).
 * Par sécurité, ils refusent de tourner si le nom de la base ne contient pas
 * « test ».
 *
 * <p>Préparation (une fois) :
 * <pre>
 *   psql -U postgres -c "CREATE DATABASE frenchchic_test"
 *   psql -U postgres -d frenchchic_test -f sql/schema.sql
 * </pre>
 * Lancement :
 * <pre>
 *   java -Dfrenchchic.url=jdbc:postgresql://localhost:5432/frenchchic_test -cp "out;lib/*" Lot2PersistanceTests
 * </pre>
 */
public class Lot2PersistanceTests {

    private static int passed = 0;
    private static int failed = 0;

    private static final DepotProduits produits = new DepotProduits();
    private static final DepotCommandes commandes = new DepotCommandes();

    interface Test {
        void run() throws Exception;
    }

    private static void tester(String nom, Test test) {
        try {
            viderBase();
            test.run();
            System.out.println("[PASS] " + nom);
            passed++;
        } catch (Throwable e) {
            System.out.println("[FAIL] " + nom);
            System.out.println("       Erreur : " + e);
            failed++;
        }
    }

    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void verifierDouble(double attendu, double obtenu, String message) {
        if (Math.abs(attendu - obtenu) > 0.000001) {
            throw new AssertionError(message + " | attendu : " + attendu + ", obtenu : " + obtenu);
        }
    }

    /**
     * TRUNCATE ne déclenche pas les triggers « BEFORE DELETE » : c'est voulu, base de test uniquement.
     * CASCADE vide aussi les éventuelles autres tables liées à produit.
     */
    private static void viderBase() throws SQLException {
        try (Connection c = ConnexionBD.ouvrir(); Statement st = c.createStatement()) {
            st.execute("TRUNCATE ligne_commande, commande, produit CASCADE");
        }
        produits.enregistrer(new Produit("ROBE-001", "Robe d'été fleurie", 49.90, 12));
        produits.enregistrer(new Produit("SAC-001", "Sac en cuir camel", 89.00, 5));
        produits.enregistrer(new Produit("FOUL-001", "Foulard en soie", 29.90, 15));
    }

    private static int compter(String table) throws SQLException {
        try (Connection c = ConnexionBD.ouvrir();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static InfosLivraison livraison() {
        return new InfosLivraison("Martin", "Léa", "0612345678", "lea.martin@example.com",
                "12 rue de la Paix, 06000 Nice");
    }

    private static Commande commande(String numero, Object... refQuantite) {
        List<LignePanier> lignes = new java.util.ArrayList<>();
        for (int i = 0; i < refQuantite.length; i += 2) {
            Produit p = produits.getProduit((String) refQuantite[i]);
            if (p == null) {          // produit absent du catalogue (test d'annulation)
                p = new Produit((String) refQuantite[i], "Produit fantôme", 10.0, 0);
            }
            lignes.add(new LignePanier(p, (Integer) refQuantite[i + 1]));
        }
        return Commande.creer(lignes, livraison(), numero);
    }

    public static void main(String[] args) throws SQLException {

        System.out.println("======================================");
        System.out.println("     TESTS LOT 2 - PERSISTANCE");
        System.out.println("======================================");

        String url;
        try (Connection c = ConnexionBD.ouvrir()) {
            url = c.getMetaData().getURL();
        } catch (SQLException | PersistanceException e) {
            System.out.println("Connexion impossible à la base de test : " + e.getMessage());
            System.exit(2);
            return;
        }
        if (!url.contains("test")) {
            System.out.println("REFUS : la base " + url + " n'est pas une base de test.");
            System.out.println("Lancez avec -Dfrenchchic.url=jdbc:postgresql://localhost:5432/frenchchic_test");
            System.exit(2);
        }
        System.out.println("Base : " + url);

        testerProduits();
        testerCommandes();
        testerIntegration();

        try {
            viderBase();
        } catch (SQLException e) {
            // sans importance : la base de test est vidée au prochain lancement
        }
        afficherBilan();
        System.exit(failed == 0 ? 0 : 1);
    }

    // ==========================================
    // 1. DEPOT PRODUITS
    // ==========================================

    private static void testerProduits() {

        System.out.println("\n--- DEPOT PRODUITS ---");

        tester("PER-01 : Catalogue lu en base, trié par libellé", () -> {
            List<Produit> liste = produits.getProduits();
            verifier(liste.size() == 3, "3 produits attendus, obtenu " + liste.size());
            verifier(liste.get(0).getReference().equals("FOUL-001"), "Premier attendu : Foulard");
            verifier(liste.get(2).getReference().equals("SAC-001"), "Dernier attendu : Sac");
        });

        tester("PER-02 : Lecture par référence, null si inconnue", () -> {
            Produit robe = produits.getProduit("ROBE-001");
            verifier(robe != null, "ROBE-001 introuvable");
            verifierDouble(49.90, robe.getPrix(), "Prix incorrect");
            verifier(robe.getQuantiteStock() == 12, "Stock incorrect");
            verifier(produits.getProduit("INCONNU") == null, "null attendu pour une référence inconnue");
        });

        tester("PER-03 : Deux lectures du même produit sont égales (equals)", () -> {
            Produit a = produits.getProduit("ROBE-001");
            Produit b = produits.getProduit("ROBE-001");
            verifier(a != b, "Deux objets distincts attendus");
            verifier(a.equals(b) && a.hashCode() == b.hashCode(), "Ils doivent être égaux");
        });

        tester("PER-04 : Enregistrer met à jour le stock et crée un nouveau produit", () -> {
            Produit robe = produits.getProduit("ROBE-001");
            robe.decrementerStock(3);
            produits.enregistrer(robe);
            verifier(produits.getProduit("ROBE-001").getQuantiteStock() == 9, "Stock 9 attendu");
            produits.enregistrer(new Produit("JEAN-001", "Jean slim brut", 59.00, 20));
            verifier(produits.getProduits().size() == 4, "4 produits attendus");
        });

        tester("PER-05 : Les accents sont conservés", () -> {
            verifier(produits.getProduit("ROBE-001").getLibelle().equals("Robe d'été fleurie"),
                    "Libellé obtenu : " + produits.getProduit("ROBE-001").getLibelle());
        });
    }

    // ==========================================
    // 2. DEPOT COMMANDES
    // ==========================================

    private static void testerCommandes() {

        System.out.println("\n--- DEPOT COMMANDES ---");

        tester("PER-06 : Dernier numéro d'ordre = 0 sans commande", () ->
                verifier(commandes.dernierNumeroOrdre() == 0, "0 attendu"));

        tester("PER-07 : Enregistrer puis relire une commande à l'identique", () -> {
            Commande c = commande("TR-20261122-000007", "ROBE-001", 2, "FOUL-001", 1);
            commandes.enregistrer(c);
            Commande lue = commandes.getCommande("TR-20261122-000007");
            verifier(lue != null, "Commande introuvable");
            verifier(lue.getLignes().size() == 2, "2 lignes attendues");
            verifier(lue.getLignes().get(0).getProduit().getReference().equals("ROBE-001"), "Ordre des lignes");
            verifier(lue.getLignes().get(0).getQuantite() == 2, "Quantité ligne 1");
            verifierDouble(2 * 49.90 + 29.90, lue.getTotal(), "Total");
            verifier(lue.getModePaiement().equals(Commande.PAIEMENT_A_LA_LIVRAISON), "Mode de paiement");
            InfosLivraison l = lue.getInfosLivraison();
            verifier(l.getNom().equals("Martin") && l.getPrenom().equals("Léa")
                    && l.getTelephone().equals("0612345678") && l.getEmail().equals("lea.martin@example.com")
                    && l.getAdresse().equals("12 rue de la Paix, 06000 Nice"), "Infos de livraison");
            verifier(commandes.dernierNumeroOrdre() == 7, "Dernier numéro d'ordre 7 attendu");
        });

        tester("PER-08 : Commande inconnue : null", () ->
                verifier(commandes.getCommande("TR-20000101-000099") == null, "null attendu"));

        tester("PER-09 : Un numéro déjà utilisé est refusé, l'original reste intact", () -> {
            commandes.enregistrer(commande("TR-20261122-000001", "ROBE-001", 1));
            try {
                commandes.enregistrer(commande("TR-20261122-000001", "SAC-001", 3));
                throw new AssertionError("PersistanceException attendue");
            } catch (PersistanceException e) {
                Commande lue = commandes.getCommande("TR-20261122-000001");
                verifier(lue.getLignes().size() == 1
                        && lue.getLignes().get(0).getProduit().getReference().equals("ROBE-001"),
                        "La commande d'origine a été modifiée");
            }
        });

        tester("PER-10 : Échec sur une ligne = rien n'est enregistré (transaction)", () -> {
            try {
                commandes.enregistrer(commande("TR-20261122-000003", "ROBE-001", 1, "FANTOME-999", 1));
                throw new AssertionError("PersistanceException attendue (produit inexistant)");
            } catch (PersistanceException e) {
                verifier(commandes.getCommande("TR-20261122-000003") == null, "La commande ne doit pas exister");
                verifier(compter("commande") == 0 && compter("ligne_commande") == 0, "Tables non vides");
                verifier(commandes.dernierNumeroOrdre() == 0, "Le numéro d'ordre ne doit pas avancer");
            }
        });

        tester("PER-11 : La base refuse de modifier ou supprimer une commande", () -> {
            commandes.enregistrer(commande("TR-20261122-000001", "ROBE-001", 1));
            String[] requetes = {
                "UPDATE commande SET total = 0",
                "DELETE FROM commande",
                "UPDATE ligne_commande SET quantite = 99",
                "DELETE FROM ligne_commande",
            };
            for (String sql : requetes) {
                try (Connection c = ConnexionBD.ouvrir(); Statement st = c.createStatement()) {
                    st.executeUpdate(sql);
                    throw new AssertionError("Requête acceptée alors qu'elle devait être refusée : " + sql);
                } catch (SQLException attendu) {
                    // refusé par le déclencheur : c'est le comportement voulu
                }
            }
            Commande lue = commandes.getCommande("TR-20261122-000001");
            verifierDouble(49.90, lue.getTotal(), "La commande a été modifiée");
        });

        tester("PER-12 : La commande garde le prix du jour de l'achat", () -> {
            commandes.enregistrer(commande("TR-20261122-000001", "ROBE-001", 1));
            produits.enregistrer(new Produit("ROBE-001", "Robe d'été fleurie", 59.90, 12));
            Commande lue = commandes.getCommande("TR-20261122-000001");
            verifierDouble(49.90, lue.getLignes().get(0).getProduit().getPrix(), "Prix historique perdu");
            verifierDouble(49.90, lue.getTotal(), "Total historique perdu");
        });
    }

    // ==========================================
    // 3. CONTROLEUR + BASE DE DONNEES
    // ==========================================

    private static void testerIntegration() {

        System.out.println("\n--- CONTROLEUR + BASE DE DONNEES ---");

        tester("PER-13 : Une commande confirmée met à jour la base", () -> {
            SessionPasserCommande s = new SessionPasserCommande(produits, commandes);
            s.ajouterAuPanier("ROBE-001", 2);
            s.ajouterAuPanier("ROBE-001", 1);
            s.ajouterAuPanier("SAC-001", 1);
            s.modifierQuantitePanier("SAC-001", 2);

            Map<String, String> champs = new HashMap<>();
            champs.put("nom", "Martin");
            champs.put("prenom", "Léa");
            champs.put("telephone", "0612345678");
            champs.put("email", "lea.martin@example.com");
            champs.put("adresse", "12 rue de la Paix, 06000 Nice");

            SessionPasserCommande.ResultatConfirmation r = s.confirmerCommande(champs);
            String numero = r.commande().getNumeroTransaction();
            verifier(numero.endsWith("-000001"), "Premier numéro attendu, obtenu " + numero);
            verifier(produits.getProduit("ROBE-001").getQuantiteStock() == 9, "Stock robe en base");
            verifier(produits.getProduit("SAC-001").getQuantiteStock() == 3, "Stock sac en base");
            Commande lue = commandes.getCommande(numero);
            verifier(lue != null && lue.getLignes().size() == 2, "Commande en base avec 2 lignes");
            verifierDouble(3 * 49.90 + 2 * 89.00, lue.getTotal(), "Total en base");
            verifier(compter("ligne_commande") == 2, "2 lignes en base");
        });
    }

    // ==========================================
    // BILAN
    // ==========================================

    private static void afficherBilan() {
        System.out.println("\n======================================");
        System.out.println("             BILAN DES TESTS");
        System.out.println("======================================");
        System.out.println("Tests réussis : " + passed);
        System.out.println("Tests échoués : " + failed);
        System.out.println("--------------------------------------");
        System.out.println(failed == 0
                ? "RESULTAT : TOUS LES TESTS SONT VALIDES"
                : "RESULTAT : CERTAINS TESTS ONT ECHOUE");
        System.out.println("======================================");
    }
}
