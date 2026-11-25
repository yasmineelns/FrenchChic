import frenchchic.controleur.ChampLivraisonInvalideException;
import frenchchic.controleur.PanierExpireException;
import frenchchic.controleur.SessionPasserCommande;
import frenchchic.controleur.SessionPasserCommande.InfosProduit;
import frenchchic.controleur.SessionPasserCommande.RecapPanier;
import frenchchic.controleur.SessionPasserCommande.ResultatConfirmation;
import frenchchic.metier.Commande;
import frenchchic.metier.Produit;
import frenchchic.metier.StockInsuffisantException;
import frenchchic.metier.persistance.DepotCommandes;
import frenchchic.metier.persistance.DepotProduits;
import frenchchic.metier.persistance.DepotReservations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests du lot 2 — contrôleur {@link SessionPasserCommande}.
 *
 * <p>Sans base de données : les dépôts sont remplacés par des faux dépôts en
 * mémoire. Comme le vrai dépôt JDBC, le faux dépôt des produits renvoie un
 * NOUVEL objet Produit à chaque lecture.
 *
 * <p>Lancement : {@code java -cp out Lot2ControleurTests}
 */
public class Lot2ControleurTests {

    private static int passed = 0;
    private static int failed = 0;

    // ==========================================
    // Faux dépôts (en mémoire)
    // ==========================================

    /** Catalogue en mémoire ; chaque lecture renvoie une copie, comme JDBC. */
    static class FauxDepotProduits extends DepotProduits {

        final Map<String, Produit> stockes = new LinkedHashMap<>();
        int nbEnregistrements = 0;

        FauxDepotProduits(Produit... produits) {
            for (Produit p : produits) {
                stockes.put(p.getReference(), p);
            }
        }

        private static Produit copie(Produit p) {
            return new Produit(p.getReference(), p.getLibelle(), p.getPrix(), p.getQuantiteStock());
        }

        @Override
        public List<Produit> getProduits() {
            List<Produit> liste = new ArrayList<>();
            for (Produit p : stockes.values()) {
                liste.add(copie(p));
            }
            return liste;
        }

        @Override
        public Produit getProduit(String reference) {
            Produit p = stockes.get(reference);
            return p == null ? null : copie(p);
        }

        @Override
        public void enregistrer(Produit produit) {
            stockes.put(produit.getReference(), copie(produit));
            nbEnregistrements++;
        }

        int stock(String reference) {
            return stockes.get(reference).getQuantiteStock();
        }
    }

    static class FauxDepotCommandes extends DepotCommandes {

        final Map<String, Commande> commandes = new LinkedHashMap<>();
        int dernier = 0;

        @Override
        public int dernierNumeroOrdre() {
            return dernier;
        }

        @Override
        public void enregistrer(Commande commande) {
            String n = commande.getNumeroTransaction();
            dernier = Integer.parseInt(n.substring(n.lastIndexOf('-') + 1));
            commandes.put(n, commande);
        }

        @Override
        public Commande getCommande(String numeroTransaction) {
            return commandes.get(numeroTransaction);
        }
    }

    /** Réservations en mémoire, avec les mêmes règles que le dépôt JDBC. */
    static class FauxDepotReservations extends DepotReservations {

        record Resa(int quantite, Instant expire) { }

        final FauxDepotProduits produits;
        final Map<String, Map<String, Resa>> parSession = new HashMap<>();

        FauxDepotReservations(FauxDepotProduits produits) {
            this.produits = produits;
        }

        private int reserveParAutres(String idSession, String reference, Instant maintenant) {
            int total = 0;
            for (Map.Entry<String, Map<String, Resa>> e : parSession.entrySet()) {
                Resa r = e.getValue().get(reference);
                if (!e.getKey().equals(idSession) && r != null && r.expire().isAfter(maintenant)) {
                    total += r.quantite();
                }
            }
            return total;
        }

        @Override
        public boolean reserver(String idSession, String reference, int quantite,
                                Instant expireLe, Instant maintenant) {
            Produit p = produits.stockes.get(reference);
            if (p == null || quantite > p.getQuantiteStock() - reserveParAutres(idSession, reference, maintenant)) {
                return false;
            }
            parSession.computeIfAbsent(idSession, k -> new HashMap<>()).put(reference, new Resa(quantite, expireLe));
            return true;
        }

        @Override
        public int disponiblePour(String idSession, String reference, Instant maintenant) {
            Produit p = produits.stockes.get(reference);
            return p == null ? 0 : Math.max(0, p.getQuantiteStock() - reserveParAutres(idSession, reference, maintenant));
        }

        @Override
        public Map<String, Integer> reservationsActives(Instant maintenant) {
            Map<String, Integer> total = new HashMap<>();
            for (Map<String, Resa> resas : parSession.values()) {
                resas.forEach((ref, r) -> {
                    if (r.expire().isAfter(maintenant)) {
                        total.merge(ref, r.quantite(), Integer::sum);
                    }
                });
            }
            return total;
        }

        @Override
        public void liberer(String idSession, String reference) {
            Map<String, Resa> resas = parSession.get(idSession);
            if (resas != null) {
                resas.remove(reference);
            }
        }

        @Override
        public void libererTout(String idSession) {
            parSession.remove(idSession);
        }

        int nbReservations() {
            return parSession.values().stream().mapToInt(Map::size).sum();
        }
    }

    /** Horloge que le test fait avancer à volonté (pas besoin d'attendre 15 minutes). */
    static class HorlogeReglable extends Clock {

        private Instant maintenant = Instant.parse("2026-11-24T10:00:00Z");

        void avancer(Duration d) {
            maintenant = maintenant.plus(d);
        }

        @Override
        public Instant instant() {
            return maintenant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /** Session avec un dépôt de réservations neuf et une durée de 15 minutes. */
    private static SessionPasserCommande session(FauxDepotProduits produits, FauxDepotCommandes commandes) {
        return session(produits, commandes, new FauxDepotReservations(produits), new HorlogeReglable());
    }

    private static SessionPasserCommande session(FauxDepotProduits produits, FauxDepotCommandes commandes,
                                                 FauxDepotReservations reservations, Clock horloge) {
        return new SessionPasserCommande(produits, commandes, reservations,
                SessionPasserCommande.DUREE_RESERVATION, horloge);
    }

    // ==========================================
    // Outils
    // ==========================================

    interface Test {
        void run() throws Exception;
    }

    private static void tester(String nom, Test test) {
        try {
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

    private static FauxDepotProduits catalogue() {
        return new FauxDepotProduits(
                new Produit("ROBE-001", "Robe d'été fleurie", 49.90, 12),
                new Produit("SAC-001", "Sac en cuir camel", 89.00, 5),
                new Produit("FOUL-001", "Foulard en soie", 29.90, 15));
    }

    private static Map<String, String> livraisonValide() {
        Map<String, String> champs = new HashMap<>();
        champs.put("nom", "Martin");
        champs.put("prenom", "Léa");
        champs.put("telephone", "0612345678");
        champs.put("email", "lea.martin@example.com");
        champs.put("adresse", "12 rue de la Paix, 06000 Nice");
        return champs;
    }

    private static String numeroAttendu(int ordre) {
        return "TR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("-%06d", ordre);
    }

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("     TESTS LOT 2 - CONTROLEUR");
        System.out.println("======================================");

        testerConsultation();
        testerPanier();
        testerConfirmation();
        testerReservation();

        afficherBilan();
        System.exit(failed == 0 ? 0 : 1);
    }

    // ==========================================
    // 1. CONSULTER LES PRODUITS
    // ==========================================

    private static void testerConsultation() {

        System.out.println("\n--- CONSULTER LES PRODUITS ---");

        tester("CTL-01 : Liste des produits avec référence, libellé, prix et stock", () -> {
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            List<InfosProduit> infos = s.consulterProduits();
            verifier(infos.size() == 3, "3 produits attendus, obtenu " + infos.size());
            InfosProduit robe = infos.get(0);
            verifier(robe.reference().equals("ROBE-001"), "Référence incorrecte");
            verifier(robe.libelle().equals("Robe d'été fleurie"), "Libellé incorrect");
            verifierDouble(49.90, robe.prix(), "Prix incorrect");
            verifier(robe.stock() == 12, "Stock incorrect");
        });
    }

    // ==========================================
    // 2. PANIER
    // ==========================================

    private static void testerPanier() {

        System.out.println("\n--- PANIER ---");

        tester("CTL-02 : Ajout au panier et nombre d'articles", () -> {
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            verifier(s.ajouterAuPanier("ROBE-001", 2) == 2, "2 articles attendus");
            verifier(s.ajouterAuPanier("SAC-001", 1) == 3, "3 articles attendus");
            RecapPanier r = s.consulterPanier();
            verifier(r.lignes().size() == 2, "2 lignes attendues");
            verifierDouble(2 * 49.90 + 89.00, r.total(), "Total du panier incorrect");
        });

        tester("CTL-03 : Deux ajouts du même produit = une seule ligne", () -> {
            // Le dépôt renvoie un nouvel objet à chaque fois : le panier doit
            // reconnaître le même produit grâce à sa référence.
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            s.ajouterAuPanier("ROBE-001", 1);
            s.ajouterAuPanier("ROBE-001", 2);
            RecapPanier r = s.consulterPanier();
            verifier(r.lignes().size() == 1, "1 ligne attendue, obtenu " + r.lignes().size());
            verifier(r.lignes().get(0).getQuantite() == 3, "Quantité 3 attendue");
        });

        tester("CTL-04 : Stock insuffisant à l'ajout (alternatif A)", () -> {
            FauxDepotProduits depot = catalogue();
            SessionPasserCommande s = session(depot, new FauxDepotCommandes());
            try {
                s.ajouterAuPanier("SAC-001", 6);
                throw new AssertionError("StockInsuffisantException attendue");
            } catch (StockInsuffisantException e) {
                verifier(s.consulterPanier().lignes().isEmpty(), "Le panier doit rester vide");
                verifier(depot.stock("SAC-001") == 5, "Le stock ne doit pas changer");
            }
        });

        tester("CTL-04b : Deux ajouts qui dépassent ensemble le stock sont refusés", () -> {
            // 3 sacs puis encore 3 : chaque ajout seul tient dans le stock (5),
            // mais pas le total du panier (6).
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            s.ajouterAuPanier("SAC-001", 3);
            try {
                s.ajouterAuPanier("SAC-001", 3);
                throw new AssertionError("StockInsuffisantException attendue (3 + 3 > 5)");
            } catch (StockInsuffisantException e) {
                verifier(s.consulterPanier().nombreArticles() == 3, "Le panier doit rester à 3 sacs");
            }
        });

        tester("CTL-05 : Produit inconnu", () -> {
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            try {
                s.ajouterAuPanier("INCONNU", 1);
                throw new AssertionError("IllegalArgumentException attendue");
            } catch (IllegalArgumentException e) {
                verifier(e.getMessage().contains("INCONNU"), "Le message doit citer la référence");
            }
        });

        tester("CTL-06 : Modifier la quantité d'une ligne", () -> {
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            s.ajouterAuPanier("ROBE-001", 1);
            s.modifierQuantitePanier("ROBE-001", 4);
            RecapPanier r = s.consulterPanier();
            verifier(r.lignes().get(0).getQuantite() == 4, "Quantité 4 attendue");
            verifier(r.nombreArticles() == 4, "4 articles attendus");
        });

        tester("CTL-07 : Retirer un produit du panier", () -> {
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            s.ajouterAuPanier("ROBE-001", 1);
            s.ajouterAuPanier("SAC-001", 1);
            s.retirerDuPanier("ROBE-001");
            RecapPanier r = s.consulterPanier();
            verifier(r.lignes().size() == 1, "1 ligne attendue, obtenu " + r.lignes().size());
            verifier(r.lignes().get(0).getProduit().getReference().equals("SAC-001"), "Il doit rester le sac");
        });

        tester("CTL-08 : Vider le panier ne touche pas au stock", () -> {
            FauxDepotProduits depot = catalogue();
            SessionPasserCommande s = session(depot, new FauxDepotCommandes());
            s.ajouterAuPanier("ROBE-001", 3);
            s.viderPanier();
            verifier(s.consulterPanier().lignes().isEmpty(), "Le panier doit être vide");
            verifier(depot.stock("ROBE-001") == 12, "Le stock ne doit pas changer");
            verifier(depot.nbEnregistrements == 0, "Aucun produit ne doit être enregistré");
        });
    }

    // ==========================================
    // 3. CONFIRMER LA COMMANDE
    // ==========================================

    private static void testerConfirmation() {

        System.out.println("\n--- CONFIRMER LA COMMANDE ---");

        tester("CTL-09 : Confirmation : commande enregistrée, stocks mis à jour, panier vidé", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotCommandes commandes = new FauxDepotCommandes();
            SessionPasserCommande s = session(produits, commandes);
            s.ajouterAuPanier("ROBE-001", 2);
            s.ajouterAuPanier("FOUL-001", 1);

            ResultatConfirmation r = s.confirmerCommande(livraisonValide());

            Commande c = r.commande();
            verifier(c != null, "La commande doit être renvoyée");
            verifier(c.getNumeroTransaction().equals(numeroAttendu(1)),
                    "Numéro attendu " + numeroAttendu(1) + ", obtenu " + c.getNumeroTransaction());
            verifier(commandes.commandes.containsKey(c.getNumeroTransaction()), "Commande non enregistrée");
            verifierDouble(2 * 49.90 + 29.90, c.getTotal(), "Total incorrect");
            verifier(c.getModePaiement().equals(Commande.PAIEMENT_A_LA_LIVRAISON), "Paiement incorrect");
            verifier(c.getInfosLivraison().getEmail().equals("lea.martin@example.com"), "Email incorrect");
            verifier(r.stocksRestants().equals(List.of(10, 14)), "Stocks restants : " + r.stocksRestants());
            verifier(produits.stock("ROBE-001") == 10, "Stock de la robe non enregistré");
            verifier(produits.stock("FOUL-001") == 14, "Stock du foulard non enregistré");
            verifier(s.consulterPanier().lignes().isEmpty(), "Le panier doit être vidé");
        });

        tester("CTL-10 : Les numéros de transaction se suivent", () -> {
            FauxDepotCommandes commandes = new FauxDepotCommandes();
            SessionPasserCommande s = session(catalogue(), commandes);
            s.ajouterAuPanier("ROBE-001", 1);
            String n1 = s.confirmerCommande(livraisonValide()).commande().getNumeroTransaction();
            s.ajouterAuPanier("SAC-001", 1);
            String n2 = s.confirmerCommande(livraisonValide()).commande().getNumeroTransaction();
            verifier(n1.equals(numeroAttendu(1)) && n2.equals(numeroAttendu(2)),
                    "Numéros obtenus : " + n1 + ", " + n2);
        });

        tester("CTL-11 : Livraison invalide (alternatif D) : rien n'est enregistré", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotCommandes commandes = new FauxDepotCommandes();
            SessionPasserCommande s = session(produits, commandes);
            s.ajouterAuPanier("ROBE-001", 1);
            Map<String, String> champs = livraisonValide();
            champs.put("email", "pas-un-email");
            champs.put("telephone", "06 12");
            champs.remove("nom");
            try {
                s.confirmerCommande(champs);
                throw new AssertionError("ChampLivraisonInvalideException attendue");
            } catch (ChampLivraisonInvalideException e) {
                verifier(e.getErreurs().size() == 3, "3 erreurs attendues : " + e.getErreurs());
                verifier(commandes.commandes.isEmpty(), "Aucune commande ne doit être enregistrée");
                verifier(produits.stock("ROBE-001") == 12, "Le stock ne doit pas changer");
                verifier(s.consulterPanier().lignes().size() == 1, "Le panier doit être conservé");
            }
        });

        tester("CTL-12 : Stock devenu insuffisant avant la confirmation (alternatif B)", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotCommandes commandes = new FauxDepotCommandes();
            SessionPasserCommande s = session(produits, commandes);
            s.ajouterAuPanier("SAC-001", 4);

            // Un autre client achète 3 sacs entre l'ajout au panier et la confirmation.
            produits.enregistrer(new Produit("SAC-001", "Sac en cuir camel", 89.00, 2));

            try {
                s.confirmerCommande(livraisonValide());
                throw new AssertionError("StockInsuffisantException attendue : il ne reste que 2 sacs "
                        + "(stock enregistré après la commande : " + produits.stock("SAC-001") + ")");
            } catch (StockInsuffisantException e) {
                verifier(commandes.commandes.isEmpty(), "Aucune commande ne doit être enregistrée");
                verifier(produits.stock("SAC-001") == 2, "Le stock ne doit pas changer");
            }
        });
    }

    // ==========================================
    // 4. RESERVATION DU PANIER (CHRONO DE 15 MIN)
    // ==========================================

    private static void testerReservation() {

        System.out.println("\n--- RESERVATION DU PANIER ---");

        tester("CTL-13 : Les articles réservés par un client ne peuvent pas être pris par un autre", () -> {
            FauxDepotProduits produits = catalogue();                       // 5 sacs
            FauxDepotReservations resas = new FauxDepotReservations(produits);
            HorlogeReglable horloge = new HorlogeReglable();
            SessionPasserCommande a = session(produits, new FauxDepotCommandes(), resas, horloge);
            SessionPasserCommande b = session(produits, new FauxDepotCommandes(), resas, horloge);
            a.ajouterAuPanier("SAC-001", 4);
            try {
                b.ajouterAuPanier("SAC-001", 2);
                throw new AssertionError("StockInsuffisantException attendue : 4 sacs sur 5 sont réservés");
            } catch (StockInsuffisantException e) {
                verifier(e.getMessage().contains("Seulement 1"), "Message attendu « Seulement 1 » : " + e.getMessage());
            }
            verifier(b.ajouterAuPanier("SAC-001", 1) == 1, "Le dernier sac libre doit pouvoir être réservé");
            verifier(produits.stock("SAC-001") == 5, "Réserver ne doit pas modifier le stock");
        });

        tester("CTL-14 : Le catalogue affiche le stock moins les réservations", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotReservations resas = new FauxDepotReservations(produits);
            HorlogeReglable horloge = new HorlogeReglable();
            SessionPasserCommande a = session(produits, new FauxDepotCommandes(), resas, horloge);
            SessionPasserCommande b = session(produits, new FauxDepotCommandes(), resas, horloge);
            a.ajouterAuPanier("SAC-001", 3);
            InfosProduit sac = b.consulterProduits().stream()
                    .filter(p -> p.reference().equals("SAC-001")).findFirst().orElseThrow();
            verifier(sac.stock() == 2, "2 sacs libres attendus, obtenu " + sac.stock());
        });

        tester("CTL-15 : Augmenter la quantité dans le panier au-delà du stock libre est refusé", () -> {
            SessionPasserCommande s = session(catalogue(), new FauxDepotCommandes());
            s.ajouterAuPanier("SAC-001", 1);
            try {
                s.modifierQuantitePanier("SAC-001", 6);
                throw new AssertionError("StockInsuffisantException attendue");
            } catch (StockInsuffisantException e) {
                verifier(s.consulterPanier().lignes().get(0).getQuantite() == 1, "La quantité doit rester à 1");
            }
            s.modifierQuantitePanier("SAC-001", 5);
            verifier(s.consulterPanier().nombreArticles() == 5, "5 sacs doivent être acceptés");
        });

        tester("CTL-16 : Retirer ou vider le panier remet les articles en vente", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotReservations resas = new FauxDepotReservations(produits);
            HorlogeReglable horloge = new HorlogeReglable();
            SessionPasserCommande a = session(produits, new FauxDepotCommandes(), resas, horloge);
            SessionPasserCommande b = session(produits, new FauxDepotCommandes(), resas, horloge);
            a.ajouterAuPanier("SAC-001", 5);
            a.ajouterAuPanier("ROBE-001", 2);
            a.retirerDuPanier("SAC-001");
            verifier(b.ajouterAuPanier("SAC-001", 5) == 5, "Les 5 sacs doivent être libres après le retrait");
            a.viderPanier();
            verifier(a.secondesRestantes() == -1, "Plus de chrono quand le panier est vide");
            verifier(b.ajouterAuPanier("ROBE-001", 12) == 17, "Les 12 robes doivent être libres après le vidage");
        });

        tester("CTL-17 : Le chrono démarre au premier article et ne repart pas à zéro", () -> {
            HorlogeReglable horloge = new HorlogeReglable();
            FauxDepotProduits produits = catalogue();
            SessionPasserCommande s = session(produits, new FauxDepotCommandes(),
                    new FauxDepotReservations(produits), horloge);
            verifier(s.secondesRestantes() == -1, "Pas de chrono avec un panier vide");
            s.ajouterAuPanier("ROBE-001", 1);
            verifier(s.secondesRestantes() == 15 * 60, "15:00 attendu, obtenu " + s.secondesRestantes());
            horloge.avancer(Duration.ofMinutes(5));
            s.ajouterAuPanier("FOUL-001", 1);
            verifier(s.secondesRestantes() == 10 * 60, "10:00 attendu, obtenu " + s.secondesRestantes());
        });

        tester("CTL-18 : Après 15 minutes, le panier expire et les articles sont remis en vente", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotReservations resas = new FauxDepotReservations(produits);
            HorlogeReglable horloge = new HorlogeReglable();
            SessionPasserCommande a = session(produits, new FauxDepotCommandes(), resas, horloge);
            SessionPasserCommande b = session(produits, new FauxDepotCommandes(), resas, horloge);
            a.ajouterAuPanier("SAC-001", 5);
            horloge.avancer(Duration.ofMinutes(15));
            verifier(a.panierExpire(), "Le panier doit être expiré");
            verifier(b.ajouterAuPanier("SAC-001", 5) == 5, "Les sacs doivent être de nouveau en vente");
        });

        tester("CTL-19 : Confirmer un panier expiré est refusé, rien n'est enregistré", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotCommandes commandes = new FauxDepotCommandes();
            FauxDepotReservations resas = new FauxDepotReservations(produits);
            HorlogeReglable horloge = new HorlogeReglable();
            SessionPasserCommande s = session(produits, commandes, resas, horloge);
            s.ajouterAuPanier("ROBE-001", 2);
            horloge.avancer(Duration.ofMinutes(16));
            try {
                s.confirmerCommande(livraisonValide());
                throw new AssertionError("PanierExpireException attendue");
            } catch (PanierExpireException e) {
                verifier(commandes.commandes.isEmpty(), "Aucune commande ne doit être enregistrée");
                verifier(produits.stock("ROBE-001") == 12, "Le stock ne doit pas changer");
                verifier(s.consulterPanier().lignes().isEmpty(), "Le panier doit être vidé");
                verifier(resas.nbReservations() == 0, "Les réservations doivent être supprimées");
            }
        });

        tester("CTL-20 : Une commande confirmée supprime ses réservations", () -> {
            FauxDepotProduits produits = catalogue();
            FauxDepotReservations resas = new FauxDepotReservations(produits);
            SessionPasserCommande s = session(produits, new FauxDepotCommandes(), resas, new HorlogeReglable());
            s.ajouterAuPanier("SAC-001", 2);
            s.confirmerCommande(livraisonValide());
            verifier(resas.nbReservations() == 0, "Réservations restantes : " + resas.nbReservations());
            verifier(produits.stock("SAC-001") == 3, "Stock 3 attendu après la vente");
            verifier(s.secondesRestantes() == -1, "Plus de chrono après la commande");
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
