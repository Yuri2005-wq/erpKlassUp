package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.RoleHeritage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleHeritageDAO {

    public boolean create(RoleHeritage heritage) {
        String sql = "INSERT INTO RoleHeritage (idRoleHeritage, idRoleParent, idRoleEnfant, idEcole) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, heritage.getIdRoleHeritage());
            stmt.setString(2, heritage.getIdRoleParent());
            stmt.setString(3, heritage.getIdRoleEnfant());
            stmt.setString(4, heritage.getIdEcole());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create RoleHeritage : " + e.getMessage());
            return false;
        }
    }

    public List<RoleHeritage> findByRoleParent(String idRoleParent) {
        String sql = "SELECT * FROM RoleHeritage WHERE idRoleParent = ? AND deleted_at IS NULL";
        List<RoleHeritage> heritages = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idRoleParent);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    heritages.add(mapToRoleHeritage(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByRoleParent : " + e.getMessage());
        }
        return heritages;
    }

    public boolean softDelete(String idRoleHeritage) {
        String sql = "UPDATE RoleHeritage SET deleted_at = NOW(3) WHERE idRoleHeritage = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idRoleHeritage);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete RoleHeritage : " + e.getMessage());
            return false;
        }
    }

    private RoleHeritage mapToRoleHeritage(ResultSet rs) throws SQLException {
        RoleHeritage heritage = new RoleHeritage();
        heritage.setIdRoleHeritage(rs.getString("idRoleHeritage"));
        heritage.setIdRoleParent(rs.getString("idRoleParent"));
        heritage.setIdRoleEnfant(rs.getString("idRoleEnfant"));
        heritage.setIdEcole(rs.getString("idEcole"));
        heritage.setVersion(rs.getLong("version"));
        return heritage;
    }
}