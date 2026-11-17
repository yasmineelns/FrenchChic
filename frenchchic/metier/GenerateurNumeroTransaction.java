package frenchchic.metier;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import frenchchic.metier.persistance.DepotCommandes;

/**
 * Génère le numéro de transaction d'une commande, unique et au format
 * {@code TR-AAAAMMJJ-NNNNNN} (ex. : {@code TR-20261026-000123}) :
 * la date de la commande suivie d'un numéro d'ordre sur 6 chiffres.
 */
public class GenerateurNumeroTransaction {

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Dépôt des commandes, interrogé pour connaître le dernier numéro d'ordre. */
    private final DepotCommandes depotCommandes;

    /**
     * Crée le générateur.
     *
     * @param depotCommandes le dépôt des commandes
     */
    public GenerateurNumeroTransaction(DepotCommandes depotCommandes) {
        this.depotCommandes = depotCommandes;
    }

    /**
     * Génère le prochain numéro de transaction : la date du jour et le
     * numéro d'ordre qui suit le dernier enregistré.
     *
     * @return le numéro de transaction, ex. {@code TR-20261026-000123}
     */
    public String generer() {
        int numeroOrdre = depotCommandes.dernierNumeroOrdre() + 1;
        return String.format("TR-%s-%06d",
                LocalDate.now().format(FORMAT_DATE), numeroOrdre);
    }
}
