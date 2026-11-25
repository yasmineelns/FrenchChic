package frenchchic.controleur;

import frenchchic.metier.Commande;
import frenchchic.metier.InfosLivraison;
import frenchchic.metier.LignePanier;
import frenchchic.metier.Panier;
import frenchchic.metier.Produit;
import frenchchic.metier.StockInsuffisantException;
import frenchchic.metier.GenerateurNumeroTransaction;
import frenchchic.metier.persistance.DepotCommandes;
import frenchchic.metier.persistance.DepotProduits;
import frenchchic.metier.persistance.DepotReservations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class SessionPasserCommande {

    // -------------------------------------------------------------------------
    // Dépendances injectées (dépôts) — accès limité à cette classe
    // -------------------------------------------------------------------------

    /** Dépôt des produits : lecture de la liste et mise à jour des stocks. */
    private final DepotProduits depotProduits;

    /** Dépôt des commandes : enregistrement durable et calcul du n° d'ordre. */
    private final DepotCommandes depotCommandes;

    // -------------------------------------------------------------------------
    // Collaborateurs métier
    // -------------------------------------------------------------------------

    /** Générateur du numéro de transaction (format TR-AAAAMMJJ-NNNNNN). */
    private final GenerateurNumeroTransaction generateurNumeroTransaction;

    /** Validateur des champs de livraison (obligatoires, email, téléphone…). */
    private final ValidateurLivraison validateurLivraison;

    /** Panier temporaire du client pour la session courante. */
    private final Panier panier = new Panier();

    // -------------------------------------------------------------------------
    // Réservation du stock par le panier (RG4 modifiée)
    // -------------------------------------------------------------------------

    /** Durée de réservation par défaut d'un panier. */
    public static final Duration DUREE_RESERVATION = Duration.ofMinutes(15);

    /** Dépôt des réservations : le stock réservé par les paniers en cours. */
    private final DepotReservations depotReservations;

    /** Identifiant de ce panier dans la table des réservations. */
    private final String idSession = UUID.randomUUID().toString();

    /** Durée pendant laquelle le panier réserve ses articles. */
    private final Duration dureeReservation;

    /** Horloge (remplaçable dans les tests pour simuler le temps qui passe). */
    private final Clock horloge;

    /** Fin de la réservation ; {@code null} tant que le panier est vide. */
    private Instant expiration;

    // -------------------------------------------------------------------------
    // Records de transfert vers la vue (Data Transfer Objects)
    // -------------------------------------------------------------------------

    /**
     * Informations d'un produit renvoyées à l'écran 1 (liste des produits).
     *
     * @param reference référence du produit (à renvoyer à la session)
     * @param libelle   libellé du produit
     * @param prix      prix unitaire en euros
     * @param stock     quantité disponible en stock
     */
    public record InfosProduit(String reference, String libelle, double prix, int stock) { }

    /**
     * Résumé du panier renvoyé à l'écran 2 (panier).
     *
     * @param lignes          lignes du panier (produit, quantité, sous-total)
     * @param total           total du panier
     * @param nombreArticles  nombre total d'articles (pour le bouton « Panier »)
     */
    public record RecapPanier(
            List<LignePanier> lignes,
            double total,
            int nombreArticles) { }

    /**
     * Résultat d'une commande confirmée, renvoyé à l'écran 4 (confirmation).
     *
     * @param commande        la commande enregistrée
     * @param stocksRestants  stock restant pour chaque produit acheté,
     *                        dans le même ordre que les lignes de la commande
     */
    public record ResultatConfirmation(
            Commande commande,
            List<Integer> stocksRestants) { }

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------

    /**
     * Crée la session avec une réservation de 15 minutes. Pour une
     * démonstration, la durée peut être changée au lancement avec
     * {@code -Dfrenchchic.reservation.minutes=1}.
     *
     * @param depotProduits     dépôt des produits (lecture + mise à jour stock)
     * @param depotCommandes    dépôt des commandes (enregistrement durable)
     * @param depotReservations dépôt des réservations de stock des paniers
     */
    public SessionPasserCommande(DepotProduits depotProduits,
                                  DepotCommandes depotCommandes,
                                  DepotReservations depotReservations) {
        this(depotProduits, depotCommandes, depotReservations,
                Duration.ofMinutes(Long.getLong("frenchchic.reservation.minutes",
                        DUREE_RESERVATION.toMinutes())),
                Clock.systemUTC());
    }

    /**
     * Crée la session en injectant les dépôts, la durée de réservation et
     * l'horloge (utilisé par les tests).
     */
    public SessionPasserCommande(DepotProduits depotProduits,
                                  DepotCommandes depotCommandes,
                                  DepotReservations depotReservations,
                                  Duration dureeReservation,
                                  Clock horloge) {
        this.depotProduits = depotProduits;
        this.depotCommandes = depotCommandes;
        this.depotReservations = depotReservations;
        this.dureeReservation = dureeReservation;
        this.horloge = horloge;
        this.generateurNumeroTransaction =
                new GenerateurNumeroTransaction(depotCommandes);
        this.validateurLivraison = new ValidateurLivraison();
    }

    // =========================================================================
    // Sd ConsulterProduits
    // =========================================================================

    /**
     * Retourne la liste des produits du catalogue avec leur libellé, prix et
     * stock disponible (Écran 1 : Liste des produits).
     *
     * <p>Correspond au Sd ConsulterProduits : la session interroge le dépôt
     * des produits, puis boucle sur chaque produit pour en extraire libellé,
     * prix et stock.
     *
     * <p>Le stock affiché est le stock encore libre : stock du produit moins
     * les articles réservés par les paniers en cours (celui-ci compris).
     *
     * @return liste d'InfosProduit à afficher dans l'écran 1
     */
    public List<InfosProduit> consulterProduits() {
        gererExpiration();
        List<Produit> produits = depotProduits.getProduits();
        Map<String, Integer> reserves = depotReservations.reservationsActives(horloge.instant());
        List<InfosProduit> result = new ArrayList<>();
        for (Produit p : produits) {
            int libre = p.getQuantiteStock() - reserves.getOrDefault(p.getReference(), 0);
            result.add(new InfosProduit(
                    p.getReference(),
                    p.getLibelle(),
                    p.getPrix(),
                    Math.max(0, libre)));
        }
        return result;
    }

    // =========================================================================
    // Réservation et chrono du panier
    // =========================================================================

    /**
     * @return le nombre de secondes avant l'expiration du panier, ou -1 si le
     *         panier ne réserve rien (panier vide)
     */
    public long secondesRestantes() {
        if (expiration == null) {
            return -1;
        }
        return Math.max(0, Duration.between(horloge.instant(), expiration).getSeconds());
    }

    /** @return {@code true} si le délai de réservation du panier est dépassé */
    public boolean panierExpire() {
        return expiration != null && !horloge.instant().isBefore(expiration);
    }

    /**
     * Fin du délai : remet les articles en vente (supprime les réservations)
     * et vide le panier.
     */
    public void expirerPanier() {
        depotReservations.libererTout(idSession);
        panier.vider();
        expiration = null;
    }

    /** @return la durée de réservation d'un panier */
    public Duration getDureeReservation() {
        return dureeReservation;
    }

    private void gererExpiration() {
        if (panierExpire()) {
            expirerPanier();
        }
    }

    private int quantiteAuPanier(Produit produit) {
        for (LignePanier ligne : panier.getLignes()) {
            if (ligne.getProduit().equals(produit)) {
                return ligne.getQuantite();
            }
        }
        return 0;
    }

    /**
     * Réserve {@code quantiteTotale} exemplaires du produit pour ce panier,
     * ou lève StockInsuffisantException si les autres paniers et le stock ne
     * le permettent pas.
     */
    private void reserver(Produit produit, int quantiteTotale, int dejaAuPanier)
            throws StockInsuffisantException {
        Instant maintenant = horloge.instant();
        Instant fin = expiration != null ? expiration : maintenant.plus(dureeReservation);
        if (!depotReservations.reserver(idSession, produit.getReference(),
                quantiteTotale, fin, maintenant)) {
            int encorePossible = depotReservations.disponiblePour(
                    idSession, produit.getReference(), maintenant) - dejaAuPanier;
            throw new StockInsuffisantException(produit, Math.max(0, encorePossible));
        }
        expiration = fin;   // le chrono démarre au premier article
    }

    // =========================================================================
    // Sd AjouterAuPanier  (déroulement normal + alternatif A)
    // =========================================================================

    /**
     * Ajoute la quantité demandée du produit identifié par sa référence au
     * panier, après vérification du stock disponible.
     *
     * <p>Correspond au Sd AjouterAuPanier :
     * <ol>
     *   <li>Récupère le Produit depuis le dépôt.</li>
     *   <li>Réserve la quantité totale du produit dans le panier (RG4 modifiée :
     *       le panier réserve ses articles pendant la durée de réservation ;
     *       le chrono démarre au premier article).</li>
     *   <li>En cas de succès, ajoute la ligne au Panier et retourne le nombre
     *       d'articles mis à jour (bouton « Panier (n) »).</li>
     * </ol>
     *
     * <p><b>Déroulement alternatif A</b> : si le stock libre (stock moins les
     * réservations des autres paniers) est insuffisant,
     * {@link StockInsuffisantException} est levée — le produit n'est pas ajouté
     * au panier, le stock reste inchangé.
     *
     * @param reference référence du produit choisi par le client
     * @param quantite  quantité demandée (≥ 1)
     * @return le nombre total d'articles dans le panier après l'ajout
     * @throws StockInsuffisantException si la quantité dépasse le stock
     *                                   disponible (déroulement alternatif A)
     * @throws IllegalArgumentException  si aucun produit ne correspond à la
     *                                   référence donnée
     */
    public int ajouterAuPanier(String reference, int quantite)
            throws StockInsuffisantException {
        gererExpiration();
        Produit produit = depotProduits.getProduit(reference);
        if (produit == null) {
            throw new IllegalArgumentException(
                    "Produit introuvable : " + reference);
        }
        // Calcule la quantité déjà présente dans le panier pour ce produit,
        // afin de vérifier que le TOTAL (existant + nouveau) ne dépasse pas le stock.
        int quantiteDejaAuPanier = quantiteAuPanier(produit);
        int quantiteTotale = quantiteDejaAuPanier + quantite;

        // Délégation de la vérification au Produit (Sd VerifierStock)
        // On vérifie la quantité TOTALE et non seulement la nouvelle demande.
        produit.verifierStock(quantiteTotale);

        // Réserve la quantité totale : tient compte en plus des articles déjà
        // réservés par les paniers des autres clients (RG4 modifiée).
        reserver(produit, quantiteTotale, quantiteDejaAuPanier);
        // Stock réservé : on ajoute au panier
        panier.ajouterLigne(produit, quantite);
        return panier.getNombreArticles();
    }

    // =========================================================================
    // Sd AfficherPanier
    // =========================================================================

    /**
     * Retourne un résumé du panier courant : lignes, sous-totaux, total et
     * nombre d'articles (Écran 2 : Panier).
     *
     * <p>Correspond au Sd AfficherPanier : consulte le Panier pour en
     * extraire les lignes (avec sous-totaux via LignePanier) et le total.
     *
     * @return RecapPanier prêt à être affiché par la vue
     */
    public RecapPanier consulterPanier() {
        return new RecapPanier(
                panier.getLignes(),
                panier.getTotal(),
                panier.getNombreArticles());
    }

    // =========================================================================
    // Opérations de modification du panier (Écran 2)
    // =========================================================================

    /**
     * Modifie la quantité d'un produit dans le panier (RG3).
     *
     * <p>La réservation est ajustée à la nouvelle quantité (RG4 modifiée).
     * Le stock du produit n'est pas modifié.
     *
     * @param reference référence du produit
     * @param quantite  nouvelle quantité (≥ 1)
     * @throws StockInsuffisantException si la nouvelle quantité dépasse le
     *                                   stock libre (la quantité ne change pas)
     * @throws IllegalArgumentException  si le produit est introuvable
     */
    public void modifierQuantitePanier(String reference, int quantite)
            throws StockInsuffisantException {
        gererExpiration();
        Produit produit = depotProduits.getProduit(reference);
        if (produit == null) {
            throw new IllegalArgumentException(
                    "Produit introuvable : " + reference);
        }
        int dejaAuPanier = quantiteAuPanier(produit);
        if (dejaAuPanier == 0) {
            return;                       // produit absent du panier : rien à faire
        }
        reserver(produit, quantite, dejaAuPanier);
        panier.modifierQuantite(produit, quantite);
    }

    /**
     * Retire un produit du panier (RG3) et remet ses articles en vente.
     *
     * @param reference référence du produit à retirer
     * @throws IllegalArgumentException si le produit est introuvable
     */
    public void retirerDuPanier(String reference) {
        Produit produit = depotProduits.getProduit(reference);
        if (produit == null) {
            throw new IllegalArgumentException(
                    "Produit introuvable : " + reference);
        }
        panier.retirerProduit(produit);
        depotReservations.liberer(idSession, reference);
        if (panier.getLignes().isEmpty()) {
            expiration = null;            // panier vide : plus de chrono
        }
    }

    /**
     * Vide intégralement le panier (déroulement alternatif C) et remet tous
     * ses articles en vente. Le stock des produits n'est pas modifié.
     */
    public void viderPanier() {
        panier.vider();
        depotReservations.libererTout(idSession);
        expiration = null;
    }

    // =========================================================================
    // Sd ConfirmerCommande
    // =========================================================================

    /**
     * Confirme la commande : valide les informations de livraison, vérifie le
     * stock de chaque produit du panier, enregistre la commande, décrémente
     * les stocks et vide le panier (Écrans 3 → 4).
     *
     * <p>Correspond au Sd ConfirmerCommande qui référence :
     * <ul>
     *   <li>Sd VerifierStock (pour chaque ligne du panier)</li>
     *   <li>Sd EnregistrerCommande</li>
     *   <li>Sd MettreAJourStocks (pour chaque ligne du panier)</li>
     * </ul>
     *
     * <p><b>Déroulement alternatif B</b> (stock insuffisant à la confirmation) :
     * si le stock d'au moins un produit est devenu insuffisant depuis l'ajout
     * au panier, la méthode lève {@link StockInsuffisantException} sans
     * enregistrer la commande ni modifier aucun stock.
     *
     * <p><b>Déroulement alternatif D</b> (livraison invalide) : si un champ
     * obligatoire est absent ou mal formé, la méthode lève
     * {@link ChampLivraisonInvalideException} sans enregistrer la commande.
     *
     * @param champs map des champs de livraison saisis par le client
     *               (clés : "nom", "prenom", "telephone", "email", "adresse")
     * @return ResultatConfirmation contenant la commande enregistrée et les
     *         stocks restants de chaque produit acheté
     * @throws ChampLivraisonInvalideException si un champ est invalide ou absent
     * @throws StockInsuffisantException       si le stock d'un produit est
     *                                          devenu insuffisant (alternatif B)
     * @throws PanierExpireException           si le délai de réservation est
     *                                          dépassé (le panier est alors vidé)
     */
    public ResultatConfirmation confirmerCommande(java.util.Map<String, String> champs)
            throws ChampLivraisonInvalideException, StockInsuffisantException,
                   PanierExpireException {

        // --- Étape 0 : la réservation du panier est-elle encore valable ? ---
        if (panierExpire()) {
            expirerPanier();
            throw new PanierExpireException(dureeReservation.toMinutes());
        }

        // --- Étape 1 : validation des champs de livraison (alternatif D) ---
        InfosLivraison infosLivraison = validateurLivraison.valider(champs);

        // --- Étape 2 : vérification du stock de chaque ligne (alternatif B) ---
        //     On vérifie TOUT avant de toucher au stock (atomicité logique).
        //     Le stock est relu dans le dépôt : celui du produit gardé dans le
        //     panier date de l'ajout et a pu baisser depuis (autre client).
        List<LignePanier> lignes = panier.getLignes();
        List<Produit> produitsActuels = new ArrayList<>();
        for (LignePanier ligne : lignes) {
            String reference = ligne.getProduit().getReference();
            Produit actuel = depotProduits.getProduit(reference);
            if (actuel == null) {
                throw new IllegalStateException(
                        "Produit retiré du catalogue : " + reference);
            }
            // Délègue au Produit (Sd VerifierStock)
            actuel.verifierStock(ligne.getQuantite());
            produitsActuels.add(actuel);
        }

        // --- Étape 3 : enregistrement de la commande (Sd EnregistrerCommande) ---
        String numeroTransaction = enregistrerCommande(lignes, infosLivraison);

        // --- Étape 4 : mise à jour des stocks (Sd MettreAJourStocks) ---
        //     À ce stade tous les vérifications sont passées : on décrémente
        List<Integer> stocksRestants = mettreAJourStocks(lignes, produitsActuels);

        // --- Étape 5 : récupérer la commande enregistrée et vider le panier ---
        //     Les articles sont vendus : leurs réservations sont supprimées.
        Commande commande = depotCommandes.getCommande(numeroTransaction);
        panier.vider();
        depotReservations.libererTout(idSession);
        expiration = null;

        return new ResultatConfirmation(commande, stocksRestants);
    }

    // =========================================================================
    // Sd EnregistrerCommande  (sous-diagramme)
    // =========================================================================

    /**
     * Génère le numéro de transaction, crée la {@link Commande} et l'enregistre
     * dans le dépôt (Sd EnregistrerCommande).
     *
     * <p>Entrées : lignes du panier validé, informations de livraison validées.
     * Sortie : numéro de transaction généré.
     *
     * @param lignes         lignes du panier
     * @param infosLivraison coordonnées de livraison validées
     * @return le numéro de transaction généré (ex. TR-20261118-000001)
     */
    private String enregistrerCommande(List<LignePanier> lignes,
                                        InfosLivraison infosLivraison) {
        // Génère le n° de transaction (interroge depotCommandes.dernierNumeroOrdre)
        String numeroTransaction = generateurNumeroTransaction.generer();
        // Crée la commande (factory method)
        Commande commande = Commande.creer(lignes, infosLivraison, numeroTransaction);
        // Persiste la commande
        depotCommandes.enregistrer(commande);
        return numeroTransaction;
    }

    // =========================================================================
    // Sd MettreAJourStocks  (sous-diagramme)
    // =========================================================================

    /**
     * Décrémente le stock de chaque produit commandé et persiste le stock
     * mis à jour dans le dépôt des produits (Sd MettreAJourStocks).
     *
     * <p>Entrée : lignes du panier (produit + quantité commandée).
     * Sortie : liste des stocks restants dans le même ordre que les lignes.
     *
     * <p>Précondition : le stock de chaque produit a déjà été vérifié
     * (aucune exception ne devrait être levée ici).
     *
     * @param lignes          lignes de la commande à traiter
     * @param produitsActuels produits relus dans le dépôt, dans l'ordre des lignes
     * @return stocks restants après décrémentation, dans l'ordre des lignes
     */
    private List<Integer> mettreAJourStocks(List<LignePanier> lignes,
                                            List<Produit> produitsActuels) {
        List<Integer> stocksRestants = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            LignePanier ligne = lignes.get(i);
            Produit produit = produitsActuels.get(i);
            try {
                int restant = produit.decrementerStock(ligne.getQuantite());
                // Persiste le nouveau stock dans le dépôt
                depotProduits.enregistrer(produit);
                stocksRestants.add(restant);
            } catch (StockInsuffisantException e) {
                // Ne devrait pas arriver (stock vérifié à l'étape 2)
                // On relance en RuntimeException pour signaler un état incohérent
                throw new IllegalStateException(
                        "Incohérence de stock détectée lors de la mise à jour : "
                        + e.getMessage(), e);
            }
        }
        return stocksRestants;
    }
}