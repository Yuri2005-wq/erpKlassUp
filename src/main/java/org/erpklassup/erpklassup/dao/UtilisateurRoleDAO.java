package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.UtilisateurRole;
import org.erpklassup.erpklassup.models.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurRoleDAO {

    // ========== CREATE ==========
    public boolean ajouterRole(UtilisateurRole ur) {
        String sql = "INSERT INTO UtilisateurRole (idUtilisateurRole, idUtilisateur, idRole, " +
                "idEcole, dateAttribution, attribuePar, dateDebut, dateFin, estActive) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ur.getIdUtilisateurRole());
            stmt.setString(2, ur.getIdUtilisateur());
            stmt.setString(3, ur.getIdRole());
            stmt.setString(4, ur.getIdEcole());
            stmt.setTimestamp(5, Timestamp.valueOf(ur.getDateAttribution()));
            stmt.setString(6, ur.getAttribuePar());

            if (ur.getDateDebut() != null) stmt.setTimestamp(7, Timestamp.valueOf(ur.getDateDebut()));
            else stmt.setNull(7, Types.TIMESTAMP);

            if (ur.getDateFin() != null) stmt.setTimestamp(8, Timestamp.valueOf(ur.getDateFin()));
            else stmt.setNull(8, Types.TIMESTAMP);

            stmt.setBoolean(9, ur.isEstActive());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajouterRole : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public List<Role> findRolesByUtilisateur(String idUtilisateur) {
        String sql = "SELECT r.* FROM Role r " +
                "JOIN UtilisateurRole ur ON r.idRole = ur.idRole " +
                "WHERE ur.idUtilisateur = ? AND ur.deleted_at IS NULL " +
                "AND ur.estActive = 1 AND r.deleted_at IS NULL";

        List<Role> roles = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUtilisateur);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Role role = new Role();
                    role.setIdRole(rs.getString("idRole"));
                    role.setIdEcole(rs.getString("idEcole"));
                    role.setIdGroupe(rs.getString("idGroupe"));
                    role.setNomRole(rs.getString("nomRole"));
                    role.setDescription(rs.getString("description"));
                    role.setActive(rs.getBoolean("is_active"));
                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findRolesByUtilisateur : " + e.getMessage());
        }
        return roles;
    }

    // ========== DELETE ==========
    public boolean retirerRole(String idUtilisateur, String idRole) {
        String sql = "UPDATE UtilisateurRole SET deleted_at = NOW(3), estActive = 0 " +
                "WHERE idUtilisateur = ? AND idRole = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            stmt.setString(2, idRole);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur retirerRole : " + e.getMessage());
            return false;
        }
    }

    public boolean retirerTousRoles(String idUtilisateur) {
        String sql = "UPDATE UtilisateurRole SET deleted_at = NOW(3), estActive = 0 " +
                "WHERE idUtilisateur = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur retirerTousRoles : " + e.getMessage());
            return false;
        }
    }
    // ========== ASSIGNATION SIMPLIFIÉE ==========
    public boolean assignerRole(String idUtilisateur, String idRole, String idEcole) {
        UtilisateurRole ur = new UtilisateurRole();
        ur.setIdUtilisateur(idUtilisateur);
        ur.setIdRole(idRole);
        ur.setIdEcole(idEcole);
        ur.setDateAttribution(java.time.LocalDateTime.now());
        ur.setEstActive(true);

        return ajouterRole(ur);
    }
}