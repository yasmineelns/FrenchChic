package frenchchic.metier;

/**
 * Regroupe les coordonnées de livraison du client invité (sans compte) :
 * nom, prénom, téléphone, email et adresse de livraison.
 *
 * <p>La validité des champs (obligatoires, email, téléphone à 10 chiffres…)
 * est contrôlée par {@code ValidateurLivraison}, pas par cette classe.
 */
public class InfosLivraison {

    /** Nom du client. */
    private final String nom;

    /** Prénom du client. */
    private final String prenom;

    /** Numéro de téléphone du client. */
    private final String telephone;

    /** Adresse e-mail du client. */
    private final String email;

    /** Adresse de livraison. */
    private final String adresse;

    /**
     * Crée les informations de livraison.
     *
     * @param nom       le nom
     * @param prenom    le prénom
     * @param telephone le téléphone
     * @param email     l'adresse e-mail
     * @param adresse   l'adresse de livraison
     */
    public InfosLivraison(String nom, String prenom, String telephone,
                          String email, String adresse) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.adresse = adresse;
    }

    /** @return le nom */
    public String getNom() {
        return nom;
    }

    /** @return le prénom */
    public String getPrenom() {
        return prenom;
    }

    /** @return le téléphone */
    public String getTelephone() {
        return telephone;
    }

    /** @return l'adresse e-mail */
    public String getEmail() {
        return email;
    }

    /** @return l'adresse de livraison */
    public String getAdresse() {
        return adresse;
    }
}
