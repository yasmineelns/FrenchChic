package frenchchic.metier.persistance;

import frenchchic.metier.Commande;

/**
 * SQUELETTE (lot 2 - persistance) : à remplacer par l'implémentation JDBC.
 *
 * <p>Enregistre et relit les commandes confirmées, sans jamais les
 * supprimer ni les écraser (exigence de robustesse : 10 ans).
 * Ce squelette ne sert qu'à faire compiler {@code GenerateurNumeroTransaction}.
 */
public class DepotCommandes {

    /**
     * @return le dernier numéro d'ordre enregistré (0 s'il n'y a aucune commande)
     */
    public int dernierNumeroOrdre() {
        throw new UnsupportedOperationException("À implémenter (lot 2)");
    }

    /**
     * Enregistre durablement une commande confirmée.
     *
     * @param commande la commande à enregistrer
     */
    public void enregistrer(Commande commande) {
        throw new UnsupportedOperationException("À implémenter (lot 2)");
    }

    public Commande getCommande(String numeroTransaction) {
    throw new UnsupportedOperationException("À implémenter (lot 2)");
}
}
