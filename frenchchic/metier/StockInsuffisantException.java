package frenchchic.metier;

/**
 * Signale qu'une quantité demandée dépasse le stock disponible d'un produit.
 *
 * <p>Utilisée dans les deux déroulements alternatifs du cas d'utilisation
 * « Passer commande en ligne » :
 * <ul>
 *   <li>A : stock insuffisant à l'ajout au panier ;</li>
 *   <li>B : stock devenu insuffisant à la confirmation de la commande.</li>
 * </ul>
 * Elle indique le produit concerné et la quantité réellement disponible,
 * afin que la vue puisse afficher « Seulement X articles disponibles ».
 */
public class StockInsuffisantException extends Exception {

    /** Produit dont le stock est insuffisant. */
    private final Produit produit;

    /** Quantité réellement disponible en stock au moment du contrôle. */
    private final int stockDisponible;

    /**
     * Crée l'exception avec le produit concerné et le stock disponible.
     *
     * @param produit         le produit dont le stock est insuffisant
     * @param stockDisponible la quantité disponible en stock
     */
    public StockInsuffisantException(Produit produit, int stockDisponible) {
        super("Stock insuffisant pour " + produit.getLibelle()
              + ". Seulement " + stockDisponible + " articles disponibles.");
        this.produit = produit;
        this.stockDisponible = stockDisponible;
    }

    /** @return le produit dont le stock est insuffisant */
    public Produit getProduit() {
        return produit;
    }

    /** @return la quantité disponible en stock */
    public int getStockDisponible() {
        return stockDisponible;
    }
}
