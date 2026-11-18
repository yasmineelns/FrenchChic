package frenchchic.controleur;

import frenchchic.metier.InfosLivraison;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ValidateurLivraison {

    /** Noms attendus des champs dans la map en entrée. */
    public static final String NOM       = "nom";
    public static final String PRENOM    = "prenom";
    public static final String TELEPHONE = "telephone";
    public static final String EMAIL     = "email";
    public static final String ADRESSE   = "adresse";

    /**
     * Valide les champs et construit {@link InfosLivraison}.
     *
     * @param champs map des valeurs saisies par le client
     * @return InfosLivraison si tous les champs sont valides
     * @throws ChampLivraisonInvalideException si un ou plusieurs champs sont
     *                                          invalides ou absents
     */
    public InfosLivraison valider(Map<String, String> champs)
            throws ChampLivraisonInvalideException {

        List<String> erreurs = new ArrayList<>();

        String nom       = extraire(champs, NOM,       erreurs, "Le champ Nom est obligatoire.");
        String prenom    = extraire(champs, PRENOM,    erreurs, "Le champ Prénom est obligatoire.");
        String telephone = extraire(champs, TELEPHONE, erreurs, "Le champ Téléphone est obligatoire.");
        String email     = extraire(champs, EMAIL,     erreurs, "Le champ Email est obligatoire.");
        String adresse   = extraire(champs, ADRESSE,   erreurs, "Le champ Adresse est obligatoire.");

        // Validation du format email (RG5 : doit contenir "@")
        if (email != null && !email.contains("@")) {
            erreurs.add("L'adresse email est invalide (doit contenir '@').");
        }

        // Validation du format téléphone (RG5 : exactement 10 chiffres)
        if (telephone != null && !telephone.matches("\\d{10}")) {
            erreurs.add("Le numéro de téléphone doit comporter exactement 10 chiffres.");
        }

        if (!erreurs.isEmpty()) {
            throw new ChampLivraisonInvalideException(erreurs);
        }

        return new InfosLivraison(nom, prenom, telephone, email, adresse);
    }

    /**
     * Extrait et nettoie un champ de la map. Ajoute un message d'erreur si
     * le champ est absent ou vide.
     *
     * @param champs   map source
     * @param cle      clé du champ
     * @param erreurs  liste des erreurs à compléter
     * @param msgErreur message à ajouter si le champ est absent/vide
     * @return la valeur nettoyée (trim), ou {@code null} si absente/vide
     */
    private String extraire(Map<String, String> champs, String cle,
                             List<String> erreurs, String msgErreur) {
        String valeur = champs != null ? champs.get(cle) : null;
        if (valeur == null || valeur.trim().isEmpty()) {
            erreurs.add(msgErreur);
            return null;
        }
        return valeur.trim();
    }
}
