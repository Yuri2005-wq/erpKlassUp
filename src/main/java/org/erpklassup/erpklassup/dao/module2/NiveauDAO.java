package org.erpklassup.erpklassup.dao.module2;


import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.Niveau;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NiveauDAO {

    // ========== CREATE ==========
    public boolean create(Niveau niveau) {
        String sql = "INSERT INTO Niveau (idNiveau, idEcole, nomNiveau, systeme, cycle, ordre, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, niveau.getIdNiveau());
            stmt.setString(2, niveau.getIdEcole());
            stmt.setString(3, niveau.getNomNiveau());
            stmt.setString(4, niveau.getSysteme());
            stmt.setString(5, niveau.getCycle());
            stmt.setInt(6, niveau.getOrdre());
            stmt.setLong(7, niveau.getVersion() != null ? niveau.getVersion() : 1L);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create niveau : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Niveau> findById(String idNiveau) {
        String sql = "SELECT * FROM Niveau WHERE idNiveau = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idNiveau);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToNiveau(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById niveau : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Niveau> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Niveau WHERE idEcole = ? AND deleted_at IS NULL ORDER BY ordre ASC, nomNiveau ASC";
        List<Niveau> niveaux = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    niveaux.add(mapToNiveau(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole niveau : " + e.getMessage());
        }
        return niveaux;
    }

    public List<Niveau> findByEcoleAndSysteme(String idEcole, String systeme) {
        String sql = "SELECT * FROM Niveau WHERE idEcole = ? AND systeme = ? AND deleted_at IS NULL ORDER BY ordre ASC";
        List<Niveau> niveaux = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, systeme);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    niveaux.add(mapToNiveau(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcoleAndSysteme niveau : " + e.getMessage());
        }
        return niveaux;
    }

    // ========== UPDATE ==========
    public boolean update(Niveau niveau) {
        String sql = "UPDATE Niveau SET nomNiveau = ?, systeme = ?, cycle = ?, ordre = ?, version = version + 1 " +
                "WHERE idNiveau = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, niveau.getNomNiveau());
            stmt.setString(2, niveau.getSysteme());
            stmt.setString(3, niveau.getCycle());
            stmt.setInt(4, niveau.getOrdre());
            stmt.setString(5, niveau.getIdNiveau());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update niveau : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idNiveau) {
        String sql = "UPDATE Niveau SET deleted_at = NOW(3), version = version + 1 WHERE idNiveau = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idNiveau);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete niveau : " + e.getMessage());
            return false;
        }
    }

    // ========== MAPPING HELPER ==========
    private Niveau mapToNiveau(ResultSet rs) throws SQLException {
        Niveau niveau = new Niveau();
        niveau.setIdNiveau(rs.getString("idNiveau"));
        niveau.setIdEcole(rs.getString("idEcole"));
        niveau.setNomNiveau(rs.getString("nomNiveau"));
        niveau.setSysteme(rs.getString("systeme"));
        niveau.setCycle(rs.getString("cycle"));
        niveau.setOrdre(rs.getInt("ordre"));
        niveau.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) niveau.setDeletedAt(deletedAt.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) niveau.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) niveau.setUpdatedAt(updatedAt.toLocalDateTime());

        return niveau;
    }
}