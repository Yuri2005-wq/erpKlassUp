package org.erpklassup.erpklassup.sync;

import org.erpklassup.erpklassup.Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DatabaseConnectionProvider — adapte le pool HikariCP existant (Database)
 * à l'interface ConnectionProvider attendue par SyncOrchestrator.
 *
 * On réutilise Database.getConnexion() tel quel, donc chaque connexion
 * empruntée par l'orchestrateur a déjà ses variables de session MySQL
 * initialisées (@klassup_client_id, @klassup_user_id,
 * @klassup_sync_in_progress) — indispensable pour que le garde anti-écho
 * dans SyncOrchestrator.setSyncGuard() fonctionne correctement pendant
 * le pull, et que les triggers locaux attribuent les bonnes valeurs.
 */
public class DatabaseConnectionProvider implements SyncOrchestrator.ConnectionProvider {

    @Override
    public Connection get() throws SQLException {
        return Database.getConnexion();
    }
}
