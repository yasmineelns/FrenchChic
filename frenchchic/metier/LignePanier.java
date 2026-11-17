package frenchchic.metier;

public class LignePanier {

    private final Produit produit;
    private int quantite;

    public LignePanier(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = quantite;
    }

    public Produit getProduit() {
        return produit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    // Méthode du Lot 1
    public double getSousTotal() {
        return produit.getPrix() * quantite;
    }

    // Compatibilité avec l'ancien FrenchChic
    public double calculerSousTotal() {
        return getSousTotal();
    }
}
