package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.GroupeMatiere;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupeMatiereDAO {

    // ========== CREATE ==========
    public boolean create(GroupeMatiere groupe) {
        String sql = "INSERT INTO GroupeMatiere (idGroupe, idEcole, codeGroupe, nomGroupe, ordreAffichage, version) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, groupe.getIdGroupe());
            stmt.setString(2, groupe.getIdEcole());
            stmt.setString(3, groupe.getCodeGroupe());
            stmt.setString(4, groupe.getNomGroupe());
            stmt.setInt(5, groupe.getOrdreAffichage());
            stmt.setLong(6, groupe.getVersion() != null ? groupe.getVersion() : 1L);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create GroupeMatiere : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<GroupeMatiere> findById(String idGroupe) {
        String sql = "SELECT * FROM GroupeMatiere WHERE idGroupe = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idGroupe);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToGroupeMatiere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById GroupeMatiere : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<GroupeMatiere> findByEcole(String idEcole) {
        String sql = "SELECT * FROM GroupeMatiere WHERE idEcole = ? AND deleted_at IS NULL ORDER BY ordreAffichage ASC";
        List<GroupeMatiere> groupes = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupes.add(mapToGroupeMatiere(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole GroupeMatiere : " + e.getMessage());
        }
        return groupes;
    }

    // ========== UPDATE ==========
    public boolean update(GroupeMatiere groupe) {
        String sql = "UPDATE GroupeMatiere SET codeGroupe = ?, nomGroupe = ?, ordreAffichage = ?, version = version + 1 " +
                "WHERE idGroupe = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, groupe.getCodeGroupe());
            stmt.setString(2, groupe.getNomGroupe());
            stmt.setInt(3, groupe.getOrdreAffichage());
            stmt.setString(4, groupe.getIdGroupe());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update GroupeMatiere : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idGroupe) {
        String sql = "UPDATE GroupeMatiere SET deleted_at = NOW(3), version = version + 1 WHERE idGroupe = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idGroupe);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete GroupeMatiere : " + e.getMessage());
            return false;
        }
    }

    // ========== MAPPING HELPER ==========
    private GroupeMatiere mapToGroupeMatiere(ResultSet rs) throws SQLException {
        GroupeMatiere groupe = new GroupeMatiere();
        groupe.setIdGroupe(rs.getString("idGroupe"));
        groupe.setIdEcole(rs.getString("idEcole"));
        groupe.setCodeGroupe(rs.getString("codeGroupe"));
        groupe.setNomGroupe(rs.getString("nomGroupe"));
        groupe.setOrdreAffichage(rs.getInt("ordreAffichage"));
        groupe.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) groupe.setDeletedAt(deletedAt.toLocalDateTime());

        return groupe;
    }
}