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

import java.util.ArrayList;
import java.util.List;


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
     * Crée la session en injectant les dépôts et les collaborateurs.
     *
     * @param depotProduits   dépôt des produits (lecture + mise à jour stock)
     * @param depotCommandes  dépôt des commandes (enregistrement durable)
     */
    public SessionPasserCommande(DepotProduits depotProduits,
                                  DepotCommandes depotCommandes) {
        this.depotProduits = depotProduits;
        this.depotCommandes = depotCommandes;
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
     * @return liste d'InfosProduit à afficher dans l'écran 1
     */
    public List<InfosProduit> consulterProduits() {
        List<Produit> produits = depotProduits.getProduits();
        List<InfosProduit> result = new ArrayList<>();
        for (Produit p : produits) {
            result.add(new InfosProduit(
                    p.getReference(),
                    p.getLibelle(),
                    p.getPrix(),
                    p.getQuantiteStock()));
        }
        return result;
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
     *   <li>Délègue la vérification de stock au Produit (VerifierStock).</li>
     *   <li>En cas de succès, ajoute la ligne au Panier et retourne le nombre
     *       d'articles mis à jour (bouton « Panier (n) »).</li>
     * </ol>
     *
     * <p><b>Déroulement alternatif A</b> : si le stock est insuffisant,
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
        Produit produit = depotProduits.getProduit(reference);
        if (produit == null) {
            throw new IllegalArgumentException(
                    "Produit introuvable : " + reference);
        }
        // Délégation de la vérification au Produit (Sd VerifierStock)
        produit.verifierStock(quantite);
        // Stock suffisant : on ajoute au panier
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
     * <p>Aucun stock n'est modifié (RG4) : le panier ne réserve pas le stock.
     *
     * @param reference référence du produit
     * @param quantite  nouvelle quantité (≥ 1)
     * @throws IllegalArgumentException si le produit est introuvable
     */
    public void modifierQuantitePanier(String reference, int quantite) {
        Produit produit = depotProduits.getProduit(reference);
        if (produit == null) {
            throw new IllegalArgumentException(
                    "Produit introuvable : " + reference);
        }
        panier.modifierQuantite(produit, quantite);
    }

    /**
     * Retire un produit du panier (RG3, RG4).
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
    }

    /**
     * Vide intégralement le panier (déroulement alternatif C et RG4).
     *
     * <p>Aucun stock n'est modifié.
     */
    public void viderPanier() {
        panier.vider();
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
     */
    public ResultatConfirmation confirmerCommande(java.util.Map<String, String> champs)
            throws ChampLivraisonInvalideException, StockInsuffisantException {

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
        Commande commande = depotCommandes.getCommande(numeroTransaction);
        panier.vider();

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
