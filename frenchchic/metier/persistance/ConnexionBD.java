package frenchchic.metier.persistance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Ouvre les connexions JDBC vers PostgreSQL.
 *
 * <p>Les paramètres sont lus dans le fichier {@code db.properties} (clés
 * {@code url}, {@code utilisateur}, {@code motDePasse}) situé dans le dossier
 * de lancement, ou à l'emplacement donné par
 * {@code -Dfrenchchic.config=chemin/vers/db.properties}.
 * Ce fichier n'est pas versionné : voir {@code db.properties.example}.
 */
public final class ConnexionBD {

    private static Properties parametres;

    private ConnexionBD() {
    }

    private static synchronized Properties parametres() {
        if (parametres == null) {
            Path chemin = Path.of(System.getProperty("frenchchic.config", "db.properties"));
            if (!Files.exists(chemin)) {
                throw new PersistanceException("Fichier de configuration introuvable : "
                        + chemin.toAbsolutePath()
                        + "\nCopiez db.properties.example en db.properties et renseignez le mot de passe.");
            }
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(chemin)) {
                p.load(in);
            } catch (IOException e) {
                throw new PersistanceException("Lecture impossible de " + chemin, e);
            }
            parametres = p;
        }
        return parametres;
    }

    /**
     * Ouvre une nouvelle connexion. L'appelant doit la fermer
     * (try-with-resources).
     *
     * @return une connexion ouverte
     * @throws SQLException si la base est injoignable ou refuse la connexion
     */
    public static Connection ouvrir() throws SQLException {
        Properties p = parametres();
        return DriverManager.getConnection(
                p.getProperty("url", "jdbc:postgresql://localhost:5432/frenchchic"),
                p.getProperty("utilisateur", "postgres"),
                p.getProperty("motDePasse", ""));
    }
}
