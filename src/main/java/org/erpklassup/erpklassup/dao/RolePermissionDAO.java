package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;

import java.sql.*;
import java.util.*;

public class RolePermissionDAO {

    public Set<String> listerIdsActifsParRole(String idRole) {
        String sql = "SELECT idPermission FROM RolePermission WHERE idRole = ? AND estActive = 1";
        Set<String> ids = new HashSet<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idRole);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("idPermission"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur listerIdsActifsParRole : " + e.getMessage());
        }
        return ids;
    }

    /** Applique en une transaction l'état complet des cases cochées pour un rôle. */
    public void synchroniser(String idRole, String idEcole, Set<String> idsPermissionCochees, String attribuePar) throws SQLException {
        String sqlExistants = "SELECT idPermission, estActive FROM RolePermission WHERE idRole = ?";
        String sqlInsert = "INSERT INTO RolePermission (idRolePermission, idRole, idPermission, idEcole, attribuePar, estActive) VALUES (?, ?, ?, ?, ?, 1)";
        String sqlReactiver = "UPDATE RolePermission SET estActive = 1, attribuePar = ?, dateAttribution = CURRENT_TIMESTAMP(3), version = version + 1 WHERE idRole = ? AND idPermission = ?";
        String sqlDesactiver = "UPDATE RolePermission SET estActive = 0, version = version + 1 WHERE idRole = ? AND idPermission = ?";

        try (Connection conn = Database.getConnexion()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Boolean> existants = new HashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement(sqlExistants)) {
                    stmt.setString(1, idRole);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) existants.put(rs.getString("idPermission"), rs.getBoolean("estActive"));
                    }
                }

                try (PreparedStatement insert = conn.prepareStatement(sqlInsert);
                     PreparedStatement reactiver = conn.prepareStatement(sqlReactiver);
                     PreparedStatement desactiver = conn.prepareStatement(sqlDesactiver)) {

                    for (String idPermission : idsPermissionCochees) {
                        if (!existants.containsKey(idPermission)) {
                            insert.setString(1, UUID.randomUUID().toString());
                            insert.setString(2, idRole);
                            insert.setString(3, idPermission);
                            insert.setString(4, idEcole);
                            insert.setString(5, attribuePar);
                            insert.addBatch();
                        } else if (!existants.get(idPermission)) {
                            reactiver.setString(1, attribuePar);
                            reactiver.setString(2, idRole);
                            reactiver.setString(3, idPermission);
                            reactiver.addBatch();
                        }
                    }
                    for (var entree : existants.entrySet()) {
                        if (entree.getValue() && !idsPermissionCochees.contains(entree.getKey())) {
                            desactiver.setString(1, idRole);
                            desactiver.setString(2, entree.getKey());
                            desactiver.addBatch();
                        }
                    }

                    insert.executeBatch();
                    reactiver.executeBatch();
                    desactiver.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}