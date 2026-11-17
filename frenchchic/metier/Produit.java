package frenchchic.metier;

/**
 * Représente un produit du catalogue et ses informations métier :
 * libellé, prix et quantité en stock.
 *
 * <p>Gère notamment la vérification d'une quantité demandée et la diminution
 * du stock à la confirmation de la commande.
 */
public class Produit {

    /** Nom du produit. */
    private final String libelle;

    /** Prix unitaire en euros. */
    private final double prix;

    /** Quantité actuellement disponible en stock. */
    private int quantiteStock;

    /**
     * Crée un produit.
     *
     * @param libelle       le nom du produit
     * @param prix          le prix unitaire
     * @param quantiteStock la quantité disponible en stock
     */
    public Produit(String libelle, double prix, int quantiteStock) {
        this.libelle = libelle;
        this.prix = prix;
        this.quantiteStock = quantiteStock;
    }

    /** @return le nom du produit */
    public String getLibelle() {
        return libelle;
    }

    /** @return le prix unitaire */
    public double getPrix() {
        return prix;
    }

    /** @return la quantité disponible en stock */
    public int getQuantiteStock() {
        return quantiteStock;
    }

    /**
     * Vérifie que la quantité demandée est disponible en stock.
     * Ne modifie pas le stock.
     *
     * @param quantite la quantité demandée par le client
     * @throws StockInsuffisantException si la quantité dépasse le stock disponible
     */
    public void verifierStock(int quantite) throws StockInsuffisantException {
        if (quantite > quantiteStock) {
            throw new StockInsuffisantException(this, quantiteStock);
        }
    }

    /**
     * Diminue le stock de la quantité achetée. À appeler uniquement à la
     * confirmation de la commande : le panier ne réserve jamais le stock.
     *
     * @param quantite le nombre d'exemplaires achetés
     * @return le stock restant après la diminution
     * @throws StockInsuffisantException si la quantité dépasse le stock disponible
     *                                   (le stock reste alors inchangé)
     */
    public int decrementerStock(int quantite) throws StockInsuffisantException {
        verifierStock(quantite);
        quantiteStock -= quantite;
        return quantiteStock;
    }
}
