package frenchchic.metier;

/**
 * Représente le produit du jour et ses informations métier.
 * Gère notamment la diminution de la quantité en stock après un achat.
 */
public class Produit {

    // --- Exception métier (classe imbriquée) ---

    /** Levée quand la quantité demandée dépasse le stock disponible. */
    public static class StockInsuffisantException extends Exception {
        public StockInsuffisantException(int demandee, int disponible) {
            super("Stock insuffisant : " + demandee + " demandé(s), "
                  + disponible + " disponible(s).");
        }
    }
    // --- Attributs ---

    
    private String libelle;

    
    private double prix;

    
    private int quantiteStock;

    // --- Constructeur ---

    /**
     * Crée un produit avec son libellé, son prix et sa quantité en stock.
     *
     * @param libelle       le nom du produit
     * @param prix          le prix unitaire
     * @param quantiteStock la quantité disponible en stock
     */
    public Produit(String libelle, double prix, int quantiteStock) {
        this.libelle       = libelle;
        this.prix          = prix;
        this.quantiteStock = quantiteStock;
    }


    // --- Accesseurs (getters) ---

    
    public String getLibelle() {
        return libelle;
    }

    
    public double getPrix() {
        return prix;
    }

    
    public int getQuantiteStock() {
        return quantiteStock;
    }

    // --- Méthode métier ---

    /**
     * Décrémente le stock du produit de la quantité achetée.
     *
     * @param quantite nombre d'exemplaires achetés
     * @throws StockInsuffisantException si la quantité dépasse le stock disponible
     */
    public void decrementerStock(int quantite) throws StockInsuffisantException {
        if (quantite > this.quantiteStock) {
            throw new StockInsuffisantException(quantite, this.quantiteStock);
        }
        this.quantiteStock -= quantite;
    }
}

