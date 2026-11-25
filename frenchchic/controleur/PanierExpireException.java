package frenchchic.controleur;

/**
 * Le délai de réservation du panier est dépassé : les articles ont été remis
 * en vente et le panier a été vidé. La commande n'est pas enregistrée.
 */
public class PanierExpireException extends Exception {

    public PanierExpireException(long minutes) {
        super("Votre panier a expiré : il était réservé " + minutes
                + " minutes. Les articles ont été remis en vente.");
    }
}
