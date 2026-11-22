# French Chic — Application "Ventes & stock" (Sprint 2)

Application Java Swing de vente en ligne : le client consulte le catalogue,
remplit son panier, saisit ses informations de livraison et confirme sa
commande (paiement à la livraison). Les produits et les commandes sont
enregistrés dans une base **PostgreSQL** (JDBC).

## Prérequis

- **Java 17 ou plus récent** (JDK, pas seulement JRE).
  Pour vérifier : `java -version` et `javac -version`.
- **PostgreSQL** (installé en local, port 5432).
- **Pilote JDBC PostgreSQL** : télécharger `postgresql-42.x.x.jar` sur
  <https://jdbc.postgresql.org/download/> et le placer dans un dossier `lib/`
  à la racine du projet (ce dossier n'est pas versionné).

## Installation de la base (une seule fois)

Chacun utilise sa propre base locale. Depuis la racine du projet :

```powershell
& "C:\Program Files\PostgreSQL\19\bin\psql.exe" -U postgres -c "CREATE DATABASE frenchchic"
& "C:\Program Files\PostgreSQL\19\bin\psql.exe" -U postgres -d frenchchic -f sql/schema.sql
```

(adapter `19` à votre version de PostgreSQL). Le script crée les tables
`produit`, `commande` et `ligne_commande`, et ajoute 6 produits de départ.
Il peut être relancé sans risque.

Copier ensuite `db.properties.example` en **`db.properties`** et y mettre le
mot de passe de l'utilisateur `postgres`. Ce fichier n'est pas versionné :
le mot de passe ne part jamais sur GitHub.

## Compiler et lancer

```powershell
javac -encoding UTF-8 -d out -cp "lib/*" (Get-ChildItem frenchchic -Recurse -Filter *.java).FullName
java -cp "out;lib/*" frenchchic.Main
```

Tests du lot 1 (couche métier, sans base de données) :

```powershell
java -cp out Lot1MetierTests
```

## Organisation du code

Architecture en trois couches (Vue / Contrôleur / Métier), plus la persistance :

| Package                            | Classes principales                                            | Rôle                                              |
|------------------------------------|----------------------------------------------------------------|---------------------------------------------------|
| `frenchchic`                       | `Main`                                                         | Crée les dépôts, la session et la fenêtre         |
| `frenchchic.vue`                   | `FenetreFrenchChic`, `EcranAccueil`, `EcranPanier`, `EcranLivraisonPaiement`, `EcranConfirmation` | Les 4 écrans et la navigation |
| `frenchchic.controleur`            | `SessionPasserCommande`, `ValidateurLivraison`                 | CU « Passer commande en ligne »                   |
| `frenchchic.metier`                | `Produit`, `Panier`, `LignePanier`, `Commande`, `InfosLivraison`, `GenerateurNumeroTransaction` | Règles métier |
| `frenchchic.metier.persistance`    | `DepotProduits`, `DepotCommandes`, `ConnexionBD`               | Accès JDBC à PostgreSQL                           |

Choix de conception pour la persistance :

- Chaque produit a une **référence** unique (ex. `ROBE-001`), clé de la table
  `produit`. Deux objets `Produit` de même référence sont égaux (`equals`) :
  le dépôt crée un nouvel objet à chaque lecture.
- Une commande et ses lignes sont enregistrées dans **une seule transaction**.
- Les commandes ne sont **jamais modifiées ni supprimées** (conservation
  10 ans) : la base le refuse grâce à des déclencheurs.
- Les lignes de commande gardent le libellé et le prix **du jour de l'achat**.

## Équipe — Groupe B

Sarah Benazouz (SM), Khaoula Hajlaoui (PO), Emna Bengara, Yasmine El-Hainous, Yassmin Zouarhi.
