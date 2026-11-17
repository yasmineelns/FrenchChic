package frenchchic.metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Représente une commande confirmée : lignes, informations de livraison,
 * mode de paiement « à la livraison », total à payer et numéro de transaction.
 *
 * <p>Une commande est créée à la confirmation, puis conservée durablement
 * (exigence de robustesse : 10 ans) sans jamais être modifiée.
 */
public class Commande {

    /** Seul mode de paiement proposé au sprint 2. */
    public static final String PAIEMENT_A_LA_LIVRAISON = "à la livraison";

    /** Lignes commandées (copie figée des lignes du panier). */
    private final List<LignePanier> lignes;

    /** Coordonnées de livraison du client invité. */
    private final InfosLivraison infosLivraison;

    /** Mode de paiement : toujours « à la livraison ». */
    private final String modePaiement;

    /** Total à payer à la livraison. */
    private final double total;

    /** Numéro de transaction, au format TR-AAAAMMJJ-NNNNNN. */
    private final String numeroTransaction;

    private Commande(List<LignePanier> lignes, InfosLivraison infosLivraison,
                     String numeroTransaction) {
        // Copie de la liste : le panier est vidé juste après la confirmation,
        // et la commande ne doit pas être affectée.
        this.lignes = new ArrayList<>(lignes);
        this.infosLivraison = infosLivraison;
        this.modePaiement = PAIEMENT_A_LA_LIVRAISON;
        this.numeroTransaction = numeroTransaction;

        double somme = 0;
        for (LignePanier ligne : this.lignes) {
            somme += ligne.getSousTotal();
        }
        this.total = somme;
    }

    /**
     * Crée la commande confirmée (message « create » de la séquence
     * EnregistrerCommande).
     *
     * @param lignes            les lignes du panier validé
     * @param infosLivraison    les informations de livraison validées
     * @param numeroTransaction le numéro généré par {@link GenerateurNumeroTransaction}
     * @return la commande créée, avec le paiement « à la livraison »
     */
    public static Commande creer(List<LignePanier> lignes,
                                 InfosLivraison infosLivraison,
                                 String numeroTransaction) {
        return new Commande(lignes, infosLivraison, numeroTransaction);
    }

    /** @return les lignes de la commande (lecture seule) */
    public List<LignePanier> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    /** @return les informations de livraison */
    public InfosLivraison getInfosLivraison() {
        return infosLivraison;
    }

    /** @return le mode de paiement (« à la livraison ») */
    public String getModePaiement() {
        return modePaiement;
    }

    /** @return le total à payer à la livraison */
    public double getTotal() {
        return total;
    }

    /** @return le numéro de transaction */
    public String getNumeroTransaction() {
        return numeroTransaction;
    }
}


