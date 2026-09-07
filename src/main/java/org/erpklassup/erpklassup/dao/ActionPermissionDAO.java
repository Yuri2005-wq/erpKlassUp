package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ActionPermissionDAO {

    /** codeAction -> codePermission, pour toutes les actions actives. */
    public Map<String, String> chargerMapping() {
        String sql = """
                SELECT ap.codeAction, p.codePermission
                FROM ActionPermission ap
                INNER JOIN Permission p ON p.idPermission = ap.idPermission
                WHERE ap.estActive = 1 AND ap.deleted_at IS NULL AND p.deleted_at IS NULL
                """;
        Map<String, String> mapping = new HashMap<>();
        try (Connection conn = Database.getConnexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) mapping.put(rs.getString("codeAction"), rs.getString("codePermission"));
        } catch (SQLException e) {
            System.err.println("Erreur chargerMapping (ActionPermission) : " + e.getMessage());
        }
        return mapping;
    }
}