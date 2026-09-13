package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.models.Permission;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionDAO {

    /**
     * Récupère tous les codes de permissions effectives d'un utilisateur (directes + héritées via rôle parent/enfant)
     * en 1 seule requête SQL récursive (CTE).
     *
     */
    public Set<String> findPermissionsEffectivesPourUtilisateur(String idUtilisateur) {
        Set<String> permissions = new HashSet<>();

        String sql = """
            WITH RECURSIVE HierarchieRoles AS (
                -- Ancre : rôles directement attribués à l'utilisateur
                SELECT idRole
                FROM UtilisateurRole
                WHERE idUtilisateur = ? AND deleted_at IS NULL

                UNION

                -- Récursion : remontée vers les rôles parents
                SELECT h.idRoleParent
                FROM RoleHeritage h
                INNER JOIN HierarchieRoles hr ON h.idRoleEnfant = hr.idRole
                WHERE h.deleted_at IS NULL
            )
            -- 1️⃣ Permissions issues des rôles (héritage inclus)
            SELECT DISTINCT p.codePermission
            FROM Permission p
            INNER JOIN RolePermission rp ON p.idPermission = rp.idPermission
            INNER JOIN HierarchieRoles hr ON rp.idRole = hr.idRole
            WHERE rp.deleted_at IS NULL
              AND rp.estActive = 1
              AND p.deleted_at IS NULL

            UNION

            -- 2️⃣ ✅ Permissions d'exception attribuées directement à l'utilisateur
            SELECT DISTINCT p.codePermission
            FROM Permission p
            INNER JOIN UtilisateurPermission up ON up.idPermission = p.idPermission
            WHERE up.idUtilisateur = ?
              AND up.estActive = 1
              AND up.deleted_at IS NULL
              AND p.deleted_at IS NULL;
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUtilisateur);
            stmt.setString(2, idUtilisateur);   // 2e paramètre pour la 2e partie

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("codePermission"));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findPermissionsEffectivesPourUtilisateur : " + e.getMessage());
            e.printStackTrace();
        }

        return permissions;
    }

//    public Set<String> findPermissionsEffectivesPourUtilisateur(String idUtilisateur) {
//        Set<String> permissions = new HashSet<>();
//
//        String sql = """
//            WITH RECURSIVE HierarchieRoles AS (
//                -- 1. Ancre : Rôles directes attribués à l'utilisateur
//                SELECT idRole
//                FROM UtilisateurRole
//                WHERE idUtilisateur = ? AND deleted_at IS NULL
//
//                UNION
//
//                -- 2. Récursion : Remontée vers les rôles parents
//                SELECT h.idRoleParent
//                FROM RoleHeritage h
//                INNER JOIN HierarchieRoles hr ON h.idRoleEnfant = hr.idRole
//                WHERE h.deleted_at IS NULL
//            )
//            SELECT DISTINCT p.codePermission
//            FROM Permission p
//            INNER JOIN RolePermission rp ON p.idPermission = rp.idPermission
//            INNER JOIN HierarchieRoles hr ON rp.idRole = hr.idRole
//            WHERE rp.deleted_at IS NULL
//              AND rp.estActive = 1
//              AND p.deleted_at IS NULL;
//        """;
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, idUtilisateur);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    permissions.add(rs.getString("codePermission"));
//                }
//            }
//
//        } catch (SQLException e) {
//            System.err.println("❌ Erreur findPermissionsEffectivesPourUtilisateur : " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return permissions;
//    }

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

        // Champs de synchronisation
        try {
            permission.setVersion(rs.getLong("version"));
        } catch (SQLException ignored) {}

        try {
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                permission.setDeletedAt(deletedAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}

        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                permission.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}

        try {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                permission.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}

        return permission;
    }
}