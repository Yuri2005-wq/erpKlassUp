package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.models.Permission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PermissionDAO {
    public List<PermissionOption> listerToutes() {
        String sql = "SELECT idPermission, codePermission, libelle, categorie FROM Permission WHERE deleted_at IS NULL ORDER BY categorie, libelle";
        List<PermissionOption> permissions = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                permissions.add(new PermissionOption(
                        rs.getString("idPermission"), rs.getString("codePermission"),
                        rs.getString("libelle"), rs.getString("categorie")));
            }
        } catch (SQLException e) {
            System.err.println("Erreur listerToutes (Permission) : " + e.getMessage());
        }
        return permissions;
    }

    public List<Permission> findByRole(String idRole) {
        String sql = "SELECT p.* FROM Permission p " +
                "JOIN RolePermission rp ON p.idPermission = rp.idPermission " +
                "WHERE rp.idRole = ? AND rp.deleted_at IS NULL AND rp.estActive = 1 " +
                "AND p.deleted_at IS NULL " +
                "ORDER BY p.categorie, p.libelle";

        List<Permission> permissions = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idRole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapToPermission(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findByRole permissions : " + e.getMessage());
        }

        return permissions;
    }



    private Permission mapToPermission(ResultSet rs) throws SQLException {
        Permission permission = new Permission();
        permission.setIdPermission(rs.getString("idPermission"));
        permission.setCodePermission(rs.getString("codePermission"));
        permission.setLibelle(rs.getString("libelle"));
        permission.setCategorie(rs.getString("categorie"));

        // Champs de synchronisation (si présents dans la table)
        try {
            permission.setVersion(rs.getLong("version"));
        } catch (SQLException ignored) {
            // La colonne version n'existe pas
        }

        try {
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                permission.setDeletedAt(deletedAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {
            // La colonne deleted_at n'existe pas
        }

        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                permission.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {
            // La colonne created_at n'existe pas
        }

        try {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                permission.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {
            // La colonne updated_at n'existe pas
        }

        return permission;
    }

}