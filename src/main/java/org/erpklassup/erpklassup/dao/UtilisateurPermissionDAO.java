package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.UtilisateurPermission;
import org.erpklassup.erpklassup.models.Permission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurPermissionDAO {

    public boolean ajouter(UtilisateurPermission up) {
        String sql = "INSERT INTO UtilisateurPermission (idUtilisateurPermission, idUtilisateur, " +
                "idPermission, dateAttribution, attribuePar, dateDebut, dateFin, " +
                "motifAttribution, estActive) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, up.getIdUtilisateurPermission());
            stmt.setString(2, up.getIdUtilisateur());
            stmt.setString(3, up.getIdPermission());
            stmt.setTimestamp(4, Timestamp.valueOf(up.getDateAttribution()));
            stmt.setString(5, up.getAttribuePar());

            if (up.getDateDebut() != null) stmt.setTimestamp(6, Timestamp.valueOf(up.getDateDebut()));
            else stmt.setNull(6, Types.TIMESTAMP);

            if (up.getDateFin() != null) stmt.setTimestamp(7, Timestamp.valueOf(up.getDateFin()));
            else stmt.setNull(7, Types.TIMESTAMP);

            stmt.setString(8, up.getMotifAttribution());
            stmt.setBoolean(9, up.isEstActive());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajouter UtilisateurPermission : " + e.getMessage());
            return false;
        }
    }

    public List<Permission> findByUtilisateur(String idUtilisateur) {
        String sql = "SELECT p.* FROM Permission p " +
                "JOIN UtilisateurPermission up ON p.idPermission = up.idPermission " +
                "WHERE up.idUtilisateur = ? AND up.deleted_at IS NULL AND up.estActive = 1";
        List<Permission> permissions = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUtilisateur);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Permission permission = new Permission();
                    permission.setIdPermission(rs.getString("idPermission"));
                    permission.setCodePermission(rs.getString("codePermission"));
                    permission.setLibelle(rs.getString("libelle"));
                    permission.setCategorie(rs.getString("categorie"));
                    permissions.add(permission);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateur : " + e.getMessage());
        }
        return permissions;
    }

    public boolean retirer(String idUtilisateurPermission) {
        String sql = "UPDATE UtilisateurPermission SET deleted_at = NOW(3), estActive = 0 " +
                "WHERE idUtilisateurPermission = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateurPermission);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur retirer UtilisateurPermission : " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Supprime (soft delete) TOUTES les permissions d'exception
     * d'un utilisateur. Utilisée avant de réinsérer les nouvelles.
     */
    public boolean supprimerToutes(String idUtilisateur) {
        String sql = "UPDATE UtilisateurPermission " +
                "SET deleted_at = NOW(3), estActive = 0, version = version + 1 " +
                "WHERE idUtilisateur = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            int rows = stmt.executeUpdate();
            System.out.println("🗑️ " + rows + " permissions supprimées pour : " + idUtilisateur);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur supprimerToutes : " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Vérifie si un utilisateur a déjà une permission spécifique
     */
    public boolean possedePermission(String idUtilisateur, String idPermission) {
        String sql = "SELECT COUNT(*) FROM UtilisateurPermission " +
                "WHERE idUtilisateur = ? AND idPermission = ? " +
                "AND deleted_at IS NULL AND estActive = 1";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            stmt.setString(2, idPermission);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur possedePermission : " + e.getMessage());
        }
        return false;
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Récupère tous les IDs de permissions d'un utilisateur
     */
    public List<String> findIdsPermissionsByUtilisateur(String idUtilisateur) {
        String sql = "SELECT idPermission FROM UtilisateurPermission " +
                "WHERE idUtilisateur = ? AND deleted_at IS NULL AND estActive = 1";
        List<String> ids = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("idPermission"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findIdsPermissionsByUtilisateur : " + e.getMessage());
        }
        return ids;
    }
}
//package org.erpklassup.erpklassup.dao;
//
//import org.erpklassup.erpklassup.Database;
//import org.erpklassup.erpklassup.models.UtilisateurPermission;
//import org.erpklassup.erpklassup.models.Permission;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public class UtilisateurPermissionDAO {
//
//    public boolean ajouter(UtilisateurPermission up) {
//        String sql = "INSERT INTO UtilisateurPermission (idUtilisateurPermission, idUtilisateur, " +
//                "idPermission, dateAttribution, attribuePar, dateDebut, dateFin, " +
//                "motifAttribution, estActive) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, up.getIdUtilisateurPermission());
//            stmt.setString(2, up.getIdUtilisateur());
//            stmt.setString(3, up.getIdPermission());
//            stmt.setTimestamp(4, Timestamp.valueOf(up.getDateAttribution()));
//            stmt.setString(5, up.getAttribuePar());
//
//            if (up.getDateDebut() != null) stmt.setTimestamp(6, Timestamp.valueOf(up.getDateDebut()));
//            else stmt.setNull(6, Types.TIMESTAMP);
//
//            if (up.getDateFin() != null) stmt.setTimestamp(7, Timestamp.valueOf(up.getDateFin()));
//            else stmt.setNull(7, Types.TIMESTAMP);
//
//            stmt.setString(8, up.getMotifAttribution());
//            stmt.setBoolean(9, up.isEstActive());
//
//            return stmt.executeUpdate() > 0;
//        } catch (SQLException e) {
//            System.err.println("Erreur ajouter UtilisateurPermission : " + e.getMessage());
//            return false;
//        }
//    }
//
//    public List<Permission> findByUtilisateur(String idUtilisateur) {
//        String sql = "SELECT p.* FROM Permission p " +
//                "JOIN UtilisateurPermission up ON p.idPermission = up.idPermission " +
//                "WHERE up.idUtilisateur = ? AND up.deleted_at IS NULL AND up.estActive = 1";
//        List<Permission> permissions = new ArrayList<>();
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, idUtilisateur);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    Permission permission = new Permission();
//                    permission.setIdPermission(rs.getString("idPermission"));
//                    permission.setCodePermission(rs.getString("codePermission"));
//                    permission.setLibelle(rs.getString("libelle"));
//                    permission.setCategorie(rs.getString("categorie"));
//                    permissions.add(permission);
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur findByUtilisateur : " + e.getMessage());
//        }
//        return permissions;
//    }
//
//    public boolean retirer(String idUtilisateurPermission) {
//        String sql = "UPDATE UtilisateurPermission SET deleted_at = NOW(3), estActive = 0 " +
//                "WHERE idUtilisateurPermission = ?";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setString(1, idUtilisateurPermission);
//            return stmt.executeUpdate() > 0;
//        } catch (SQLException e) {
//            System.err.println("Erreur retirer UtilisateurPermission : " + e.getMessage());
//            return false;
//        }
//    }
//}