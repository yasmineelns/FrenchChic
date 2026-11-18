package frenchchic.controleur;

import java.util.Collections;
import java.util.List;


public class ChampLivraisonInvalideException extends Exception {

    /** Messages d'erreur détaillés, un par champ invalide. */
    private final List<String> erreurs;

    /**
     * Crée l'exception avec la liste des erreurs de validation.
     *
     * @param erreurs liste des messages d'erreur (non nulle, non vide)
     */
    public ChampLivraisonInvalideException(List<String> erreurs) {
        super("Informations de livraison invalides : " + erreurs);
        this.erreurs = Collections.unmodifiableList(erreurs);
    }

    /**
     * Retourne les messages d'erreur à afficher dans la vue.
     *
     * @return liste immuable des messages d'erreur
     */
    public List<String> getErreurs() {
        return erreurs;
    }
}
