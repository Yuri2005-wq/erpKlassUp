package org.erpklassup.erpklassup.sync;

import com.fasterxml.jackson.databind.JsonNode;
import org.quartz.*;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SyncOrchestrator — job Quartz exécuté périodiquement (ex : toutes les 30s
 * en fonctionnement normal, ou déclenché manuellement par un bouton
 * "Synchroniser maintenant").
 *
 * Cycle complet : PUSH (vider l'outbox local) puis PULL (récupérer les
 * changements distants), jamais l'inverse — pousser d'abord réduit le
 * risque de conflit avec ses propres écritures en attente.
 *
 * NOTE : SyncFile n'est plus alimentée par un appel manuel (ancien
 * OutboxDao) mais par des triggers MySQL (BEFORE/AFTER UPDATE, AFTER
 * INSERT) sur chaque table synchronisée — voir local_triggers/. Cet
 * orchestrateur n'a rien à savoir de ça : il consomme SyncFile telle
 * quelle, peu importe qui y écrit.
 *
 * Résilience réseau :
 *  - Connexion faible/latence Cameroun rural → timeouts courts + retry
 *    exponentiel (1s, 2s, 4s, 8s... plafonné) plutôt qu'un blocage long.
 *  - Après MAX_ATTEMPTS échecs consécutifs sur une même entrée, elle est
 *    marquée ECHEC_DEFINITIF ("dead letter") : elle sort de la boucle de
 *    retry automatique et apparaît dans un écran d'administration pour
 *    intervention manuelle, plutôt que de bloquer indéfiniment tout le
 *    reste de la file.
 */
public class SyncOrchestrator implements Job {

    private static final Logger log = Logger.getLogger(SyncOrchestrator.class.getName());
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 8;

    private final ConnectionProvider connections;   // fournit des Connection JDBC au pool local
    private final SyncApiClient api;
    private final String idClient;

    public SyncOrchestrator(ConnectionProvider connections, SyncApiClient api, String idClient) {
        this.connections = connections;
        this.api = api;
        this.idClient = idClient;
    }

    @Override
    public void execute(JobExecutionContext ctx) {
        try {
            runCycle();
        } catch (Exception e) {
            // Un job Quartz ne doit jamais lever d'exception non gérée :
            // on logue et on laisse le prochain tick réessayer.
            log.log(Level.SEVERE, "Cycle de synchronisation en échec", e);
        }
    }

    public void runCycle() throws Exception {
        pushPending();
        pullRemote();
    }

    // ------------------------------------------------------------------ PUSH
    private void pushPending() throws SQLException {
        try (Connection cx = connections.get()) {
            List<Row> batch = fetchPendingBatch(cx);
            if (batch.isEmpty()) return;

            List<SyncApiClient.PendingEntry> entries = new ArrayList<>();
            for (Row r : batch) entries.add(r.toEntry());

            JsonNode response = pushWithRetry(entries);
            if (response == null) {
                incrementAttempts(cx, batch);   // échec réseau total : on tente au prochain cycle
                return;
            }
            applyPushResults(cx, batch, response.get("results"));
        }
    }

    private JsonNode pushWithRetry(List<SyncApiClient.PendingEntry> entries) {
        long backoffMs = 1000;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                return api.push(entries);
            } catch (Exception e) {
                log.warning("Échec push (tentative " + attempt + "/4) : " + e.getMessage());
                sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, Duration.ofSeconds(30).toMillis());
            }
        }
        return null;
    }

    private void applyPushResults(Connection cx, List<Row> batch, JsonNode results) throws SQLException {
        for (JsonNode r : results) {
            String idFile = r.get("idFile").asText();
            String statut = r.get("status").asText();   // APPLIED | CONFLICT | ERROR
            switch (statut) {
                case "APPLIED" -> markStatut(cx, idFile, "ENVOYE", null);
                case "CONFLICT" -> markStatut(cx, idFile, "CONFLIT",
                        r.has("message") ? r.get("message").asText() : null);
                default -> markAttemptOrDeadLetter(cx, idFile);
            }
        }
    }

    // ------------------------------------------------------------------ PULL
    private void pullRemote() throws Exception {
        try (Connection cx = connections.get()) {
            long since = readSyncCursor(cx);
            boolean hasMore = true;

            // Garde anti-écho : empêche les triggers locaux de reverser
            // dans SyncFile ce qu'on est en train d'appliquer depuis le pull.
            // Le reset dans `finally` est CRITIQUE : cette connexion retourne
            // au pool Hikari après le try-with-resources, et si le flag
            // restait à 1, le prochain emprunteur verrait ses propres
            // écritures locales silencieusement ignorées par les triggers.
            setSyncGuard(cx, true);
            try {
                while (hasMore) {
                    JsonNode page = api.pull(since, 500);
                    JsonNode changes = page.get("changes");
                    for (JsonNode change : changes) {
                        applyRemoteChange(cx, change);
                    }
                    since = page.get("lastSyncLogId").asLong(since);
                    hasMore = page.get("hasMore").asBoolean(false);
                    writeSyncCursor(cx, since);
                }
            } finally {
                setSyncGuard(cx, false);
            }
        }
    }

    private void setSyncGuard(Connection cx, boolean active) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement(
                "SET @klassup_sync_in_progress = ?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.execute();
        }
    }

    /**
     * Applique un changement distant à la table locale correspondante.
     * NOTE : dans une implémentation réelle, ceci délègue à un
     * EntityApplier générique (miroir Java du registry.py côté Django) qui
     * fait l'UPSERT via PreparedStatement selon nomTable — omis ici pour
     * rester lisible ; le principe important est ci-dessous :
     */
    private void applyRemoteChange(Connection cx, JsonNode change) throws SQLException {
        String action = change.get("actionEnum").asText();
        // Un DELETE distant doit rester un DELETE mou local (deleted_at),
        // jamais un DELETE SQL physique, pour rester cohérent avec le
        // reste du schéma (colonnes *_actif générées, contraintes uniques).
        // Un INSERT/UPDATE distant doit vérifier si la ligne locale a été
        // modifiée depuis (dirty flag / version locale) avant d'écraser :
        // si oui → conflit local à signaler, pas d'écrasement silencieux.
        EntityApplier.apply(cx, change); // voir EntityApplier (registre local table -> DAO)
    }

    // ------------------------------------------------------------------ util
    private List<Row> fetchPendingBatch(Connection cx) throws SQLException {
        String sql = """
            SELECT idFile, idEcole, nomTable, idEntity, actionEnum, donneesJSON, versionClient
            FROM SyncFile
            WHERE idClient = ? AND statut = 'EN_ATTENTE' AND nombreTentatives < ?
            ORDER BY dateCreation ASC
            LIMIT ?
            """;
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, idClient);
            ps.setInt(2, MAX_ATTEMPTS);
            ps.setInt(3, BATCH_SIZE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(Row.fromResultSet(rs));
            }
        }
        return rows;
    }

    private void markStatut(Connection cx, String idFile, String statut, String message) throws SQLException {
        String sql = "UPDATE SyncFile SET statut=?, messageErreur=?, dateEnvoi=? WHERE idFile=?";
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setString(2, message);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setString(4, idFile);
            ps.executeUpdate();
        }
    }

    private void markAttemptOrDeadLetter(Connection cx, String idFile) throws SQLException {
        String sql = """
            UPDATE SyncFile
            SET nombreTentatives = nombreTentatives + 1,
                statut = CASE WHEN nombreTentatives + 1 >= ? THEN 'ECHEC_DEFINITIF' ELSE 'EN_ATTENTE' END
            WHERE idFile = ?
            """;
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, MAX_ATTEMPTS);
            ps.setString(2, idFile);
            ps.executeUpdate();
        }
    }

    private void incrementAttempts(Connection cx, List<Row> batch) throws SQLException {
        for (Row r : batch) markAttemptOrDeadLetter(cx, r.idFile);
    }

    private long readSyncCursor(Connection cx) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement(
                "SELECT dernierSyncLogId FROM SyncClient WHERE idClient = ?")) {
            ps.setString(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private void writeSyncCursor(Connection cx, long value) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement(
                "UPDATE SyncClient SET dernierSyncLogId = ?, derniereSynchro = ? WHERE idClient = ?")) {
            ps.setLong(1, value);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, idClient);
            ps.executeUpdate();
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private record Row(String idFile, String idEcole, String nomTable, String idEntity,
                        String actionEnum, String donneesJSON, long versionClient) {
        static Row fromResultSet(ResultSet rs) throws SQLException {
            return new Row(
                    rs.getString("idFile"), rs.getString("idEcole"), rs.getString("nomTable"),
                    rs.getString("idEntity"), rs.getString("actionEnum"),
                    rs.getString("donneesJSON"), rs.getLong("versionClient"));
        }
        SyncApiClient.PendingEntry toEntry() throws SQLException {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var json = donneesJSON != null ? mapper.readTree(donneesJSON) : null;
                return new SyncApiClient.PendingEntry(idFile, nomTable, idEntity, actionEnum, json, versionClient);
            } catch (Exception e) {
                throw new SQLException("JSON invalide dans SyncFile.donneesJSON pour " + idFile, e);
            }
        }
    }

    /** Abstraction simple pour obtenir une Connection du pool local (HikariCP par ex.) */
    public interface ConnectionProvider {
        Connection get() throws SQLException;
    }
}
