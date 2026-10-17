package frenchchic.metier;

/**
 * Représente le produit du jour et ses informations métier.
 * Gère notamment la diminution de la quantité en stock après un achat.
 */
public class Produit {

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
     * Ne fait rien si la quantité demandée dépasse le stock disponible./**
     * Retourne le prix du produit.
     */
    public void decrementerStock(int quantite) {
        this.quantiteStock -= quantite;
    }
}