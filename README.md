# French Chic — Application "Ventes & stock" (Sprint 1)

Application Java Swing qui affiche le **produit du jour** (libellé, prix, stock)
et permet au client d'en acheter un ou plusieurs exemplaires.
Après l'achat, l'écran confirme la commande et affiche le stock restant.

## Prérequis

- **Java 17 ou plus récent** (JDK, pas seulement JRE).
  Pour vérifier : `java -version` et `javac -version`.

## Utilisation

1. Au lancement, la fenêtre affiche le produit du jour :
   *Robe d'été fleurie — 49,90 € — Stock disponible : 12*.
2. Choisir la quantité avec les boutons **-** et **+** (1 par défaut).
3. Cliquer sur **Acheter**.
4. L'écran affiche *Achat confirmé : N exemplaire(s) acheté(s)* et le *Stock restant*.

## Organisation du code

Architecture en trois couches (Vue / Contrôleur / Métier) :

| Package                 | Classe                  | Rôle                                                   |
|-------------------------|-------------------------|--------------------------------------------------------|
| `frenchchic`            | `Main`                  | Point d'entrée : crée le produit du jour et l'écran    |
| `frenchchic.vue`        | `EcranAccueil`          | Fenêtre Swing : affichage et actions du client         |
| `frenchchic.controleur` | `SessionPasserCommande` | Gère le CU "Passer commande en ligne"                  |
| `frenchchic.metier`     | `Produit`               | Produit du jour : libellé, prix, stock, décrémentation |

## Équipe — Groupe B

Sarah Benazouz (SM), Khaoula Hajlaoui (PO), Emna Bengara, Yasmine El-Hainous, Yassmin Zouarhi.
