package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.TokenReinitialisation;

import java.sql.*;
import java.util.Optional;

public class TokenReinitialisationDAO {

    public boolean create(TokenReinitialisation token) {
        String sql = "INSERT INTO TokenReinitialisation (idToken, idUtilisateur, tokenHash, " +
                "dateExpiration, adresseIpDemande) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token.getIdToken());
            stmt.setString(2, token.getIdUtilisateur());
            stmt.setString(3, token.getTokenHash());
            stmt.setTimestamp(4, Timestamp.valueOf(token.getDateExpiration()));
            stmt.setString(5, token.getAdresseIpDemande());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create TokenReinitialisation : " + e.getMessage());
            return false;
        }
    }

    public Optional<TokenReinitialisation> findByTokenHash(String tokenHash) {
        String sql = "SELECT * FROM TokenReinitialisation WHERE tokenHash = ? AND estUtilise = 0 " +
                "AND dateExpiration > NOW()";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tokenHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TokenReinitialisation token = new TokenReinitialisation();
                    token.setIdToken(rs.getString("idToken"));
                    token.setIdUtilisateur(rs.getString("idUtilisateur"));
                    token.setTokenHash(rs.getString("tokenHash"));
                    token.setDateCreation(rs.getTimestamp("dateCreation").toLocalDateTime());
                    token.setDateExpiration(rs.getTimestamp("dateExpiration").toLocalDateTime());
                    token.setEstUtilise(rs.getBoolean("estUtilise"));
                    return Optional.of(token);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByTokenHash : " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean marquerUtilise(String idToken) {
        String sql = "UPDATE TokenReinitialisation SET estUtilise = 1, dateUtilisation = NOW() " +
                "WHERE idToken = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idToken);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur marquerUtilise : " + e.getMessage());
            return false;
        }
    }
}