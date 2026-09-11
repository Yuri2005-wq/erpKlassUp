package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.SessionInfo;

import java.sql.*;
import java.util.Optional;

public class SessionUtilisateurDAO {

    // Durée en minutes au-delà de laquelle une session sans activité est considérée inactive
    private static final int INACTIVITY_TIMEOUT_MINUTES = 15;

    /**
     * Crée une nouvelle session lors d'un login réussi.
     */
    public boolean creerSession(String idSession, String idUtilisateur, String idEcole,
                                String refreshTokenHash, String familleToken,
                                String ip, String userAgent, String nomAppareil) {
        String sql = """
            INSERT INTO SessionUtilisateur (
                idSession, idUtilisateur, idEcoleActive, refreshTokenHash, 
                familleToken, adresseIp, userAgent, nomAppareil,
                dateCreation, dateDerniereActivite, 
                dateExpirationAccess, dateExpirationRefresh, estRevoque
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 
                      DATE_ADD(NOW(), INTERVAL 1 HOUR), 
                      DATE_ADD(NOW(), INTERVAL 7 DAY), 0)
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSession);
            stmt.setString(2, idUtilisateur);
            stmt.setString(3, idEcole);
            stmt.setString(4, refreshTokenHash);
            stmt.setString(5, familleToken);
            stmt.setString(6, ip);
            stmt.setString(7, userAgent);
            stmt.setString(8, nomAppareil);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur création session : " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si un utilisateur possède au moins une session active récente.
     */
    public boolean estUtilisateurConnecte(String idUtilisateur) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM SessionUtilisateur
            WHERE idUtilisateur = ?
              AND estRevoque = 0
              AND dateDeconnexion IS NULL
              AND dateExpirationRefresh > NOW()
              AND dateDerniereActivite >= DATE_SUB(NOW(), INTERVAL ? MINUTE)
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUtilisateur);
            stmt.setInt(2, INACTIVITY_TIMEOUT_MINUTES);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification statut en ligne : " + e.getMessage());
        }
        return false;
    }

    /**
     * Met à jour l'horodatage de dernière activité (Heartbeat).
     */
    public void toucherSession(String idSession) {
        String sql = """
            UPDATE SessionUtilisateur 
            SET dateDerniereActivite = NOW() 
            WHERE idSession = ? AND estRevoque = 0 AND dateDeconnexion IS NULL
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSession);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour heartbeat session : " + e.getMessage());
        }
    }

    /**
     * Clôture propre d'une session (Logout utilisateur ou révocation administrative).
     */
    public boolean InvaliderSession(String idSession, String motif) {
        String sql = """
            UPDATE SessionUtilisateur 
            SET dateDeconnexion = NOW(), 
                motifDeconnexion = ?, 
                estRevoque = 1 
            WHERE idSession = ?
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, motif);
            stmt.setString(2, idSession);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur fermeture session : " + e.getMessage());
            return false;
        }
    }

    /**
     * Invalide toutes les autres sessions d'un utilisateur (option de sécurité "Se déconnecter de tous les appareils").
     */
    public void revoquerToutesLesSessionsUtilisateur(String idUtilisateur, String motif) {
        String sql = """
            UPDATE SessionUtilisateur 
            SET dateDeconnexion = NOW(), motifDeconnexion = ?, estRevoque = 1 
            WHERE idUtilisateur = ? AND estRevoque = 0
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, motif);
            stmt.setString(2, idUtilisateur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur révocation globale des sessions : " + e.getMessage());
        }
    }
}