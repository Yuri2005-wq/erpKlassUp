package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.SessionUtilisateur;

import java.sql.*;
import java.util.Optional;

public class SessionDAO {

    public boolean create(SessionUtilisateur session) {
        String sql = "INSERT INTO SessionUtilisateur (idSession, idUtilisateur, idEcoleActive, idGroupeActif, " +
                "accessTokenHash, refreshTokenHash, familleToken, typeSession, adresseIp, userAgent, " +
                "nomAppareil, typeAppareil, systemeExploitation, navigateur, localisation, " +
                "dateExpirationAccess, dateExpirationRefresh) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, session.getIdSession());
            stmt.setString(2, session.getIdUtilisateur());
            stmt.setString(3, session.getIdEcoleActive());
            stmt.setString(4, session.getIdGroupeActif());
            stmt.setString(5, session.getAccessTokenHash());
            stmt.setString(6, session.getRefreshTokenHash());
            stmt.setString(7, session.getFamilleToken());
            stmt.setString(8, session.getTypeSession());
            stmt.setString(9, session.getAdresseIp());
            stmt.setString(10, session.getUserAgent());
            stmt.setString(11, session.getNomAppareil());
            stmt.setString(12, session.getTypeAppareil());
            stmt.setString(13, session.getSystemeExploitation());
            stmt.setString(14, session.getNavigateur());
            stmt.setString(15, session.getLocalisation());
            stmt.setTimestamp(16, Timestamp.valueOf(session.getDateExpirationAccess()));
            stmt.setTimestamp(17, Timestamp.valueOf(session.getDateExpirationRefresh()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create session : " + e.getMessage());
            return false;
        }
    }

    public Optional<SessionUtilisateur> findByRefreshTokenHash(String refreshTokenHash) {
        String sql = "SELECT * FROM SessionUtilisateur WHERE refreshTokenHash = ? AND estRevoque = 0";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, refreshTokenHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToSession(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByRefreshTokenHash : " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean revoquerSession(String idSession, String motif) {
        String sql = "UPDATE SessionUtilisateur SET estRevoque = 1, dateDeconnexion = NOW(3), " +
                "motifDeconnexion = ? WHERE idSession = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, motif);
            stmt.setString(2, idSession);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur revoquerSession : " + e.getMessage());
            return false;
        }
    }

    private SessionUtilisateur mapToSession(ResultSet rs) throws SQLException {
        SessionUtilisateur session = new SessionUtilisateur();
        session.setIdSession(rs.getString("idSession"));
        session.setIdUtilisateur(rs.getString("idUtilisateur"));
        session.setIdEcoleActive(rs.getString("idEcoleActive"));
        session.setIdGroupeActif(rs.getString("idGroupeActif"));
        session.setRefreshTokenHash(rs.getString("refreshTokenHash"));
        session.setFamilleToken(rs.getString("familleToken"));
        session.setAdresseIp(rs.getString("adresseIp"));
        session.setEstRevoque(rs.getBoolean("estRevoque"));
        return session;
    }
}