package frenchchic.metier;

import java.util.ArrayList;
import java.util.List;

public class Panier {

    private List<LignePanier> lignes;

    public Panier() {
        this.lignes = new ArrayList<>();
    }

    public List<LignePanier> getLignes() {
        return lignes;
    }

    public void ajouterProduit(Produit produit, int quantite) {
        for (LignePanier ligne : lignes) {
            if (ligne.getProduit() == produit) {
                ligne.setQuantite(ligne.getQuantite() + quantite);
                return;
            }
        }

        lignes.add(new LignePanier(produit, quantite));
    }

    public void modifierQuantite(Produit produit, int quantite) {
        for (LignePanier ligne : lignes) {
            if (ligne.getProduit() == produit) {
                ligne.setQuantite(quantite);
                return;
            }
        }
    }

    public void retirerProduit(Produit produit) {
        lignes.removeIf(ligne -> ligne.getProduit() == produit);
    }

    public void vider() {
        lignes.clear();
    }

    public double calculerTotal() {
        double total = 0;

        for (LignePanier ligne : lignes) {
            total += ligne.calculerSousTotal();
        }

        return total;
    }
}