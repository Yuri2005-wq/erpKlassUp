package org.erpklassup.erpklassup;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class Database {
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "erpklassup";
    private static final String USER = "root";
    private static final String PASSWORD = "yuriDjaleu";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&serverTimezone=UTC&allowMultiQueries=true";

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Dimensionnement raisonnable pour une appli desktop mono-poste ou petit réseau local.
        // Pas besoin de plus pour une école primaire avec 2-3 postes de saisie simultanés.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000); // 10s avant d'abandonner si le pool est saturé
        config.setIdleTimeout(300_000);      // 5 min avant de fermer une connexion inactive
        config.setMaxLifetime(1_800_000);    // 30 min avant recyclage forcé (évite les timeouts MySQL silencieux)

        // Optimisations MySQL recommandées par HikariCP lui-même
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    /**
     * Retourne une connexion depuis le pool — quasi instantané, contrairement
     * à DriverManager.getConnection() qui rouvrait une connexion TCP à chaque appel.
     * IMPORTANT : cette connexion doit TOUJOURS être fermée (try-with-resources) —
     * "fermer" ici ne fait que la rendre au pool, elle n'est pas réellement détruite.
     */
    public static Connection getConnexion() throws SQLException {
        return dataSource.getConnection();
    }

    /** À appeler une seule fois à la fermeture propre de l'application. */
    public static void fermerPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnexion(); Statement stmt = conn.createStatement()) {
            InputStream is = Database.class.getResourceAsStream("DB.sql");

            if (is == null) {
                System.err.println("Le fichier DB.sql est introuvable dans les ressources.");
                return;
            }

            String sqlScript = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

            String[] commands = sqlScript.split(";(?=(?:[^`]*`[^`]*`)*[^`]*$)(?=(?:[^']*'[^']*')*[^']*$)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            StringBuilder triggerBuffer = new StringBuilder();
            boolean dansUnTrigger = false;

            for (String command : commands) {
                String trimmedCommand = command.trim();
                if (trimmedCommand.isEmpty()) continue;

                if (trimmedCommand.toUpperCase().contains("CREATE TRIGGER")) {
                    dansUnTrigger = true;
                }

                if (dansUnTrigger) {
                    triggerBuffer.append(trimmedCommand).append(";");

                    if (trimmedCommand.toUpperCase().endsWith("END")) {
                        String finalTrigger = triggerBuffer.toString().trim();

                        stmt.execute("DROP TRIGGER IF EXISTS before_insert_ecole_unique");
                        stmt.execute(finalTrigger);

                        dansUnTrigger = false;
                        triggerBuffer.setLength(0);
                    }
                } else {
                    stmt.execute(trimmedCommand);
                }
            }

            System.out.println("Base de données initialisée avec succès avec l'entité Ecole et son Trigger !");

        } catch (SQLException e) {
            throw new RuntimeException("Erreur d'initialisation MySQL : " + e.getMessage(), e);
        }
    }


    }

