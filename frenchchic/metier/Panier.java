package frenchchic.metier;

import java.util.ArrayList;
import java.util.List;

public class Panier {

    private final List<LignePanier> lignes = new ArrayList<>();;



    // Les produits sont comparés avec equals (même référence) et non avec == :
    // le dépôt JDBC renvoie un nouvel objet Produit à chaque lecture.
    public void ajouterLigne(Produit produit, int quantite) {
        for (LignePanier ligne : lignes) {
            if (ligne.getProduit().equals(produit)) {
                ligne.setQuantite(ligne.getQuantite() + quantite);
                return;
            }
        }
        lignes.add(new LignePanier(produit, quantite));
    }

    public void modifierQuantite(Produit produit, int quantite) {
        for (LignePanier ligne : lignes) {
            if (ligne.getProduit().equals(produit)) {
                ligne.setQuantite(quantite);
                return;
            }
        }
    }

    public void retirerProduit(Produit produit) {
        lignes.removeIf(ligne -> ligne.getProduit().equals(produit));
    }

    public void vider() {
        lignes.clear();
    }

    /** @return les lignes du panier */
    public List<LignePanier> getLignes() {
        return lignes;
    }

    /** @return le total du panier : somme des sous-totaux des lignes */
    public double getTotal() {
        double total = 0;
        for (LignePanier ligne : lignes) {
            total += ligne.getSousTotal();
        }
        return total;
    }

    /**
     * @return le nombre d'articles du panier : somme des quantités de toutes
     *         les lignes (affiché dans le bouton « Panier (n) »)
     */
    public int getNombreArticles() {
        int nombre = 0;
        for (LignePanier ligne : lignes) {
            nombre += ligne.getQuantite();
        }
        return nombre;
    }
}
