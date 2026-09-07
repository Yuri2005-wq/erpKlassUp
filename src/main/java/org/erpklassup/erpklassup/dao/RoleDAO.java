package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.models.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleDAO {

    public boolean create(Role role) {
        String sql = "INSERT INTO Role (idRole, idEcole, idGroupe, nomRole, description, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.getIdRole());
            stmt.setString(2, role.getIdEcole());
            stmt.setString(3, role.getIdGroupe());
            stmt.setString(4, role.getNomRole());
            stmt.setString(5, role.getDescription());
            stmt.setBoolean(6, role.isActive());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create role : " + e.getMessage());
            return false;
        }
    }

    public Optional<Role> findById(String idRole) {
        String sql = "SELECT * FROM Role WHERE idRole = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idRole);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToRole(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById role : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<RoleOption> listerParEcole(String idEcole) {
        String sql = "SELECT idRole, nomRole, description FROM Role WHERE idEcole = ? AND deleted_at IS NULL AND is_active = 1 ORDER BY nomRole";
        List<RoleOption> roles = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(new RoleOption(rs.getString("idRole"), rs.getString("nomRole"), rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur listerParEcole (Role) : " + e.getMessage());
        }
        return roles;
    }

    public boolean renommer(String idRole, String nouveauNom, String nouvelleDescription) {
        String sql = "UPDATE Role SET nomRole = ?, description = ?, version = version + 1 WHERE idRole = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nouveauNom);
            stmt.setString(2, nouvelleDescription);
            stmt.setString(3, idRole);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur renommer role : " + e.getMessage());
            return false;
        }
    }

    public boolean softDelete(String idRole) {
        String sql = "UPDATE Role SET deleted_at = CURRENT_TIMESTAMP(3), version = version + 1 WHERE idRole = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idRole);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete role : " + e.getMessage());
            return false;
        }
    }

    private Role mapToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setIdRole(rs.getString("idRole"));
        role.setIdEcole(rs.getString("idEcole"));
        role.setIdGroupe(rs.getString("idGroupe"));
        role.setNomRole(rs.getString("nomRole"));
        role.setDescription(rs.getString("description"));
        role.setActive(rs.getBoolean("is_active"));
        return role;
    }
}