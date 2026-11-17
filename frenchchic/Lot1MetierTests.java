import frenchchic.metier.*;
import frenchchic.metier.persistance.DepotCommandes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Lot1MetierTests {

    private static int passed = 0;
    private static int failed = 0;

    // Faux dépôt : permet de tester le générateur sans base de données.
    static class FauxDepot extends DepotCommandes {

        private final int dernierNumero;

        public FauxDepot(int dernierNumero) {
            this.dernierNumero = dernierNumero;
        }

        @Override
        public int dernierNumeroOrdre() {
            return dernierNumero;
        }
    }

    // Méthode générale pour exécuter un test.
    private static void tester(String nom, Runnable test) {
        try {
            test.run();
            System.out.println("[PASS] " + nom);
            passed++;
        } catch (Throwable e) {
            System.out.println("[FAIL] " + nom);
            System.out.println("       Erreur : " + e.getMessage());
            failed++;
        }
    }

    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void verifierDouble(
            double attendu, double obtenu, String message) {

        if (Math.abs(attendu - obtenu) > 0.000001) {
            throw new AssertionError(
                message + " | attendu : " + attendu + ", obtenu : " + obtenu
            );
        }
    }

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("       TESTS LOT 1 - METIER");
        System.out.println("======================================\n");

        testerProduit();
        testerLignePanier();
        testerPanier();
        testerExceptionStock();
        testerInfosLivraison();
        testerCommande();
        testerGenerateurTransaction();

        afficherBilan();
    }

    // ==========================================
    // 1. TESTS PRODUIT
    // ==========================================

    private static void testerProduit() {

        System.out.println("\n--- TESTS PRODUIT ---");

        Produit produit = new Produit("Robe fleurie", 49.90, 12);

        tester("PROD-01 : Création du produit", () -> {
            verifier(produit.getLibelle().equals("Robe fleurie"),
                    "Libellé incorrect");

            verifierDouble(49.90, produit.getPrix(),
                    "Prix incorrect");

            verifier(produit.getQuantiteStock() == 12,
                    "Stock initial incorrect");
        });

        tester("PROD-02 : Vérification d'un stock disponible", () -> {
            try {
                produit.verifierStock(5);
            } catch (StockInsuffisantException e) {
                throw new AssertionError("Exception inattendue");
            }

            verifier(produit.getQuantiteStock() == 12,
                    "La vérification ne doit pas modifier le stock");
        });

        tester("PROD-03 : Stock insuffisant", () -> {
            try {
                produit.verifierStock(15);
                throw new AssertionError("Exception non déclenchée");

            } catch (StockInsuffisantException e) {

                verifier(e.getProduit() == produit,
                        "Mauvais produit dans l'exception");

                verifier(e.getStockDisponible() == 12,
                        "Stock disponible incorrect");
            }
        });

        tester("PROD-04 : Décrémentation du stock", () -> {
            try {
                int restant = produit.decrementerStock(3);

                verifier(restant == 9,
                        "Stock restant incorrect");

                verifier(produit.getQuantiteStock() == 9,
                        "Stock non actualisé");

            } catch (StockInsuffisantException e) {
                throw new AssertionError("Exception inattendue");
            }
        });

        tester("PROD-05 : Achat de la totalité du stock", () -> {
            try {
                produit.decrementerStock(9);

                verifier(produit.getQuantiteStock() == 0,
                        "Le stock devrait être égal à zéro");

            } catch (StockInsuffisantException e) {
                throw new AssertionError("Exception inattendue");
            }
        });

        tester("PROD-06 : Achat supérieur au stock", () -> {
            Produit p = new Produit("Sac", 30, 4);

            try {
                p.decrementerStock(5);
                throw new AssertionError("Exception non déclenchée");

            } catch (StockInsuffisantException e) {

                verifier(p.getQuantiteStock() == 4,
                        "Le stock a été modifié malgré l'exception");
            }
        });
    }

    // ==========================================
    // 2. TESTS LIGNE PANIER
    // ==========================================

    private static void testerLignePanier() {

        System.out.println("\n--- TESTS LIGNE PANIER ---");

        Produit produit = new Produit("Jean", 40, 20);

        LignePanier ligne = new LignePanier(produit, 3);

        tester("LIG-01 : Création d'une ligne", () -> {
            verifier(ligne.getProduit() == produit,
                    "Produit incorrect");

            verifier(ligne.getQuantite() == 3,
                    "Quantité incorrecte");
        });

        tester("LIG-02 : Calcul du sous-total", () -> {
            verifierDouble(120, ligne.getSousTotal(),
                    "Sous-total incorrect");
        });

        tester("LIG-03 : Modification de quantité", () -> {
            ligne.setQuantite(5);

            verifier(ligne.getQuantite() == 5,
                    "Quantité non modifiée");

            verifierDouble(200, ligne.getSousTotal(),
                    "Sous-total incorrect après modification");
        });
    }

    // ==========================================
    // 3. TESTS PANIER
    // ==========================================

    private static void testerPanier() {

        System.out.println("\n--- TESTS PANIER ---");

        Produit robe = new Produit("Robe", 50, 10);
        Produit sac = new Produit("Sac", 30, 10);

        Panier panier = new Panier();

        tester("PAN-01 : Création d'un panier vide", () -> {
            verifier(panier.getLignes().isEmpty(),
                    "Le panier devrait être vide");

            verifierDouble(0, panier.getTotal(),
                    "Le total devrait être nul");

            verifier(panier.getNombreArticles() == 0,
                    "Le nombre d'articles devrait être nul");
        });

        tester("PAN-02 : Ajouter un produit", () -> {
            panier.ajouterLigne(robe, 2);

            verifier(panier.getLignes().size() == 1,
                    "Une seule ligne devrait exister");

            verifier(panier.getNombreArticles() == 2,
                    "Nombre d'articles incorrect");
        });

        tester("PAN-03 : Ajouter deux fois le même produit", () -> {
            panier.ajouterLigne(robe, 3);

            verifier(panier.getLignes().size() == 1,
                    "Le produit ne devrait pas créer une deuxième ligne");

            verifier(panier.getLignes().get(0).getQuantite() == 5,
                    "Les quantités ne sont pas cumulées");
        });

        tester("PAN-04 : Ajouter un deuxième produit", () -> {
            panier.ajouterLigne(sac, 3);

            verifier(panier.getLignes().size() == 2,
                    "Deux lignes devraient exister");
        });

        tester("PAN-05 : Calcul du total", () -> {
            // 5 robes à 50 € + 3 sacs à 30 € = 340 €
            verifierDouble(340, panier.getTotal(),
                    "Total incorrect");
        });

        tester("PAN-06 : Nombre total d'articles", () -> {
            verifier(panier.getNombreArticles() == 8,
                    "Nombre total d'articles incorrect");
        });

        tester("PAN-07 : Modifier la quantité", () -> {
            panier.modifierQuantite(robe, 2);

            verifier(panier.getLignes().get(0).getQuantite() == 2,
                    "Quantité non modifiée");

            verifierDouble(190, panier.getTotal(),
                    "Total incorrect après modification");
        });

        tester("PAN-08 : Retirer un produit", () -> {
            panier.retirerProduit(sac);

            verifier(panier.getLignes().size() == 1,
                    "Le produit n'a pas été retiré");

            verifier(panier.getLignes().get(0).getProduit() == robe,
                    "Mauvais produit restant");
        });

        tester("PAN-09 : Vider le panier", () -> {
            panier.vider();

            verifier(panier.getLignes().isEmpty(),
                    "Le panier n'est pas vide");

            verifierDouble(0, panier.getTotal(),
                    "Le total devrait être nul");

            verifier(panier.getNombreArticles() == 0,
                    "Le nombre d'articles devrait être nul");
        });

        tester("PAN-10 : Le panier ne modifie pas le stock", () -> {
            Panier p = new Panier();
            Produit produit = new Produit("Chaussures", 60, 10);

            p.ajouterLigne(produit, 4);

            verifier(produit.getQuantiteStock() == 10,
                    "Le panier ne doit pas décrémenter le stock");
        });
    }

    // ==========================================
    // 4. TESTS EXCEPTION STOCK
    // ==========================================

    private static void testerExceptionStock() {

        System.out.println("\n--- TESTS EXCEPTION STOCK ---");

        Produit produit = new Produit("Veste", 80, 3);

        tester("EXC-01 : Informations de l'exception", () -> {
            try {
                produit.verifierStock(5);

                throw new AssertionError("Exception non déclenchée");

            } catch (StockInsuffisantException e) {

                verifier(e.getProduit() == produit,
                        "Produit incorrect");

                verifier(e.getStockDisponible() == 3,
                        "Stock disponible incorrect");

                verifier(e.getMessage().contains("Veste"),
                        "Message incorrect");
            }
        });
    }

    // ==========================================
    // 5. TESTS INFOS LIVRAISON
    // ==========================================

    private static void testerInfosLivraison() {

        System.out.println("\n--- TESTS INFOS LIVRAISON ---");

        InfosLivraison infos = new InfosLivraison(
                "Dupont",
                "Marie",
                "0612345678",
                "marie@gmail.com",
                "10 rue de Paris"
        );

        tester("LIV-01 : Informations de livraison", () -> {

            verifier(infos.getNom().equals("Dupont"),
                    "Nom incorrect");

            verifier(infos.getPrenom().equals("Marie"),
                    "Prénom incorrect");

            verifier(infos.getTelephone().equals("0612345678"),
                    "Téléphone incorrect");

            verifier(infos.getEmail().equals("marie@gmail.com"),
                    "Email incorrect");

            verifier(infos.getAdresse().equals("10 rue de Paris"),
                    "Adresse incorrecte");
        });
    }

    // ==========================================
    // 6. TESTS COMMANDE
    // ==========================================

    private static void testerCommande() {

        System.out.println("\n--- TESTS COMMANDE ---");

        Produit robe = new Produit("Robe", 50, 10);
        Produit sac = new Produit("Sac", 30, 10);

        Panier panier = new Panier();

        panier.ajouterLigne(robe, 2);
        panier.ajouterLigne(sac, 1);

        InfosLivraison infos = new InfosLivraison(
                "Dupont",
                "Marie",
                "0612345678",
                "marie@gmail.com",
                "Nice"
        );

        Commande commande = Commande.creer(
                panier.getLignes(),
                infos,
                "TR-20261118-000001"
        );

        tester("CMD-01 : Création de la commande", () -> {
            verifier(commande != null,
                    "Commande non créée");

            verifier(commande.getLignes().size() == 2,
                    "Nombre de lignes incorrect");
        });

        tester("CMD-02 : Calcul du total", () -> {
            // 2 * 50 + 1 * 30 = 130 €
            verifierDouble(130, commande.getTotal(),
                    "Total de commande incorrect");
        });

        tester("CMD-03 : Mode de paiement", () -> {
            verifier(
                commande.getModePaiement().equals("à la livraison"),
                "Mode de paiement incorrect"
            );
        });

        tester("CMD-04 : Numéro de transaction", () -> {
            verifier(
                commande.getNumeroTransaction()
                        .equals("TR-20261118-000001"),
                "Numéro de transaction incorrect"
            );
        });

        tester("CMD-05 : Informations de livraison", () -> {
            verifier(
                commande.getInfosLivraison() == infos,
                "Informations de livraison incorrectes"
            );
        });

        tester("CMD-06 : Vider le panier après commande", () -> {
            panier.vider();

            verifier(panier.getLignes().isEmpty(),
                    "Le panier devrait être vide");

            verifier(commande.getLignes().size() == 2,
                    "La commande a été affectée");

            verifierDouble(130, commande.getTotal(),
                    "Le total de commande a changé");
        });

        tester("CMD-07 : Liste des lignes en lecture seule", () -> {
            try {
                commande.getLignes().clear();

                throw new AssertionError(
                    "La liste devrait être non modifiable"
                );

            } catch (UnsupportedOperationException attendu) {
                // Comportement attendu.
            }
        });
    }

    // ==========================================
    // 7. TESTS GENERATEUR TRANSACTION
    // ==========================================

    private static void testerGenerateurTransaction() {

        System.out.println("\n--- TESTS GENERATEUR TRANSACTION ---");

        FauxDepot depot = new FauxDepot(15);

        GenerateurNumeroTransaction generateur =
                new GenerateurNumeroTransaction(depot);

        tester("TR-01 : Génération du numéro", () -> {

            String numero = generateur.generer();

            String date = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd")
            );

            String attendu = "TR-" + date + "-000016";

            verifier(numero.equals(attendu),
                    "Numéro incorrect : " + numero);
        });

        tester("TR-02 : Format du numéro", () -> {

            String numero = generateur.generer();

            verifier(
                numero.matches("TR-\\d{8}-\\d{6}"),
                "Format de transaction invalide : " + numero
            );
        });

        tester("TR-03 : Premier numéro de transaction", () -> {

            GenerateurNumeroTransaction nouveau =
                    new GenerateurNumeroTransaction(
                            new FauxDepot(0)
                    );

            String numero = nouveau.generer();

            String date = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd")
            );

            verifier(
                numero.equals("TR-" + date + "-000001"),
                "Premier numéro incorrect"
            );
        });
    }

    // ==========================================
    // 8. BILAN
    // ==========================================

    private static void afficherBilan() {

        System.out.println("\n======================================");
        System.out.println("             BILAN DES TESTS");
        System.out.println("======================================");

        System.out.println("Tests réussis : " + passed);
        System.out.println("Tests échoués : " + failed);

        System.out.println("--------------------------------------");

        if (failed == 0) {
            System.out.println("RESULTAT : TOUS LES TESTS SONT VALIDES");
        } else {
            System.out.println("RESULTAT : CERTAINS TESTS ONT ECHOUE");
        }

        System.out.println("======================================");
    }
}