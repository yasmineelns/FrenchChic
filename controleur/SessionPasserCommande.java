package frenchchic.controleur;

import frenchchic.metier.Produit;


public class SessionPasserCommande {

    /** Le produit du jour, déjà défini quand le client accède à l'application. */
    private final Produit produit;

    /** Informations renvoyées à la vue : libellé, prix, stock. */
    public record InfosProduit(String libelle, double prix, int stock) { }

    public SessionPasserCommande(Produit produit) {
        this.produit = produit;
    }

    /**
     * Interroge le Produit et renvoie à la vue : libellé, prix, stock.
     */
    public InfosProduit consulterProduitDuJour() {
        String libelle = produit.getLibelle();
        double prix = produit.getPrix();
        int stock = produit.getQuantiteStock();
        return new InfosProduit(libelle, prix, stock);
    }

    /**
     * Demande au Produit de décrémenter son stock, puis renvoie
     * le stock restant à la vue.
     *
     * @param quantite nombre d'exemplaires choisis par le client
     * @return le stock restant après l'achat
     */
    public int acheter(int quantite) {
        produit.decrementerStock(quantite);
        return produit.getQuantiteStock();
    }
}
