package org.erpklassup.erpklassup;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class Database {

    // ==========================================
    // CONFIGURATION
    // ==========================================
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "erpklassup";
    private static final String USER = "root";
    private static final String PASSWORD = "yuriDjaleu";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Douala&allowMultiQueries=true";

    private static final HikariDataSource dataSource;

    // ==========================================
    // VARIABLES DE SESSION POUR LES TRIGGERS
    // ==========================================
    private static String clientIdCache;
    private static final String SYSTEM_USER_ID = "SYSTEM";

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_800_000);

        // Optimisations MySQL recommandées
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    // ==========================================
    // OBTENTION DE CONNEXION
    // ==========================================

    /**
     * Retourne une connexion depuis le pool ET initialise automatiquement
     * les variables de session utilisées par les triggers MySQL :
     *   - @klassup_client_id : identifiant de la machine courante
     *   - @klassup_user_id   : identifiant de l'utilisateur connecté
     *
     * ⚠️ Ces variables sont OBLIGATOIRES pour que les triggers SyncLog
     *     puissent enregistrer correctement l'origine des modifications.
     *
     * La connexion DOIT toujours être fermée (try-with-resources) —
     * "fermer" ici ne fait que la rendre au pool.
     */
    public static Connection getConnexion() throws SQLException {
        Connection conn = dataSource.getConnection();

        try {
            initialiserVariablesSession(conn);
        } catch (SQLException e) {
            // En cas d'échec, on rend la connexion au pool et on propage l'erreur
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
            throw new SQLException(
                    "Impossible d'initialiser les variables de session MySQL : "
                            + e.getMessage(), e);
        }

        return conn;
    }

    private static void initialiserVariablesSession(Connection conn) throws SQLException {
        String clientId = getClientId();
        String userId = getUserId();

        // @klassup_sync_in_progress remis à 0 défensivement à CHAQUE emprunt
        // de connexion — même si SyncOrchestrator.setSyncGuard(cx, false)
        // dans son `finally` est censé déjà l'avoir fait. Cette double
        // sécurité protège contre le cas où le JVM crashe / est tué en plein
        // cycle de pull avant que le `finally` ait pu s'exécuter : sans ce
        // reset ici, la connexion reviendrait au pool avec le garde bloqué
        // à 1, et les écritures locales de tout autre thread cesseraient
        // silencieusement d'être mises en file dans SyncFile.
        String sql = "SET @klassup_client_id = ?, @klassup_user_id = ?, "
                + "@klassup_sync_in_progress = 0";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, clientId);
            stmt.setString(2, userId);
            stmt.execute();
        }
    }


    /**
     * Récupère l'identifiant unique du client (machine).
     * Stocké dans les préférences Java pour persister entre redémarrages.
     */
    public static synchronized String getClientId() {
        if (clientIdCache != null) {
            return clientIdCache;
        }

        // 1. Priorité : propriété système (définie par HelloApplication au démarrage)
        String clientId = System.getProperty("klassup.clientId");

        if (clientId == null || clientId.isBlank()) {
            // 2. Fallback : préférences Java
            Preferences prefs = Preferences.userNodeForPackage(Database.class);
            clientId = prefs.get("klassup.clientId", null);

            if (clientId == null || clientId.isBlank()) {
                // 3. Génère un nouveau clientId
                clientId = "CLI-" + UUID.randomUUID().toString().substring(0, 8);
                prefs.put("klassup.clientId", clientId);
                System.out.println("🆔 Nouveau clientId généré : " + clientId);
            }

            System.setProperty("klassup.clientId", clientId);
        }

        clientIdCache = clientId;
        return clientId;
    }

    /**
     * Récupère l'identifiant de l'utilisateur connecté.
     * Retourne "SYSTEM" si aucun utilisateur n'est connecté (ex: démarrage).
     */
    private static String getUserId() {
        try {
            var session = org.erpklassup.erpklassup.service.SessionManager.getInstance();
            if (session.getUtilisateurCourant() != null
                    && session.getUtilisateurCourant().getIdUtilisateur() != null) {
                return session.getUtilisateurCourant().getIdUtilisateur();
            }
        } catch (Exception ignored) {
            // SessionManager pas encore initialisé (ex: au démarrage)
        }
        return SYSTEM_USER_ID;
    }

    // ==========================================
    // FERMETURE
    // ==========================================

    /** À appeler une seule fois à la fermeture propre de l'application. */
    public static void fermerPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("🔒 Pool de connexions fermé");
        }
    }

    // ==========================================
    // INITIALISATION SCHEMA
    // ==========================================

    /**
     * Initialise la base de données :
     *   1. Charge et exécute le fichier DB.sql (tables + triggers + procédures)
     *   2. Idempotent : peut être appelé plusieurs fois sans erreur
     */
    public static void initializeDatabase() {
        executerScriptSql("DB.sql");
        System.out.println("✅ Base de données initialisée (Tables, Triggers et Procédures)");
    }

    /**
     * Exécute un script SQL depuis les ressources.
     * Gère les blocs complexes (triggers, procédures) qui contiennent des `;` internes.
     */
    private static void executerScriptSql(String nomFichier) {
        try (Connection conn = getConnexion();
             Statement stmt = conn.createStatement()) {

            InputStream is = Database.class.getResourceAsStream(nomFichier);

            if (is == null) {
                System.err.println("❌ Fichier introuvable dans les ressources : " + nomFichier);
                return;
            }

            String sqlScript = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Découpage intelligent qui ignore les `;` dans les blocs complexes
            String[] commands = sqlScript.split(
                    ";(?=(?:[^`]*`[^`]*`)*[^`]*$)(?=(?:[^']*'[^']*')*[^']*$)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            StringBuilder blocBuffer = new StringBuilder();
            boolean dansUnBlocComplexe = false;

            for (String command : commands) {
                String trimmedCommand = command.trim();

                // Ignore les mots-clés DELIMITER (non supportés par JDBC)
                if (trimmedCommand.isEmpty()
                        || trimmedCommand.toUpperCase().startsWith("DELIMITER")) {
                    continue;
                }

                String upper = trimmedCommand.toUpperCase();

                // Détection du début d'un TRIGGER ou d'une PROCEDURE
                if (upper.contains("CREATE TRIGGER") || upper.contains("CREATE PROCEDURE")) {
                    dansUnBlocComplexe = true;
                }

                if (dansUnBlocComplexe) {
                    blocBuffer.append(trimmedCommand).append(";");

                    // Le bloc se termine quand on rencontre 'END' en fin d'instruction
                    if (upper.endsWith("END")) {
                        String finalBlock = blocBuffer.toString().trim();

                        // Nettoyage du délimiteur final en trop
                        if (finalBlock.endsWith(";;")) {
                            finalBlock = finalBlock.substring(0, finalBlock.length() - 1);
                        }

                        stmt.execute(finalBlock);

                        dansUnBlocComplexe = false;
                        blocBuffer.setLength(0);
                    }
                } else {
                    stmt.execute(trimmedCommand);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erreur d'exécution du script " + nomFichier + " : " + e.getMessage(), e);
        }
    }
}