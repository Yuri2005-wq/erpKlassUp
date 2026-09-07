package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.TentativeConnexion;

import java.sql.*;

public class TentativeConnexionDAO {

    public boolean enregistrer(TentativeConnexion tentative) {
        String sql = "INSERT INTO TentativeConnexion (idTentative, usernameSaisi, adresseIp, " +
                "userAgent, succes, motifEchec) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tentative.getIdTentative());
            stmt.setString(2, tentative.getUsernameSaisi());
            stmt.setString(3, tentative.getAdresseIp());
            stmt.setString(4, tentative.getUserAgent());
            stmt.setBoolean(5, tentative.isSucces());
            stmt.setString(6, tentative.getMotifEchec());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur enregistrer TentativeConnexion : " + e.getMessage());
            return false;
        }
    }
}