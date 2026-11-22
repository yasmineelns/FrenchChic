package frenchchic.metier.persistance;

/**
 * Erreur d'accès à la base de données (connexion impossible, requête
 * refusée…). Exception non vérifiée : les signatures des dépôts restent
 * celles prévues par la conception.
 */
public class PersistanceException extends RuntimeException {

    public PersistanceException(String message) {
        super(message);
    }

    public PersistanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
