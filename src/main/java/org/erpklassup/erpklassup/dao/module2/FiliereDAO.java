package org.erpklassup.erpklassup.dao.module2;


import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.Filiere;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FiliereDAO {

    // ========== CREATE ==========
    public boolean create(Filiere filiere) {
        String sql = "INSERT INTO Filiere (idFiliere, idEcole, codeFiliere, nomFiliere, version) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, filiere.getIdFiliere());
            stmt.setString(2, filiere.getIdEcole());
            stmt.setString(3, filiere.getCodeFiliere());
            stmt.setString(4, filiere.getNomFiliere());
            stmt.setLong(5, filiere.getVersion() != null ? filiere.getVersion() : 1L);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create filiere : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Filiere> findById(String idFiliere) {
        String sql = "SELECT * FROM Filiere WHERE idFiliere = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idFiliere);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToFiliere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById filiere : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Filiere> findByCode(String codeFiliere, String idEcole) {
        String sql = "SELECT * FROM Filiere WHERE codeFiliere = ? AND idEcole = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codeFiliere);
            stmt.setString(2, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToFiliere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByCode filiere : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Filiere> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Filiere WHERE idEcole = ? AND deleted_at IS NULL ORDER BY nomFiliere ASC";
        List<Filiere> filieres = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    filieres.add(mapToFiliere(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole filiere : " + e.getMessage());
        }
        return filieres;
    }

    // ========== UPDATE ==========
    public boolean update(Filiere filiere) {
        String sql = "UPDATE Filiere SET codeFiliere = ?, nomFiliere = ?, version = version + 1 " +
                "WHERE idFiliere = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, filiere.getCodeFiliere());
            stmt.setString(2, filiere.getNomFiliere());
            stmt.setString(3, filiere.getIdFiliere());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update filiere : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idFiliere) {
        String sql = "UPDATE Filiere SET deleted_at = NOW(3), version = version + 1 WHERE idFiliere = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idFiliere);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete filiere : " + e.getMessage());
            return false;
        }
    }

    // ========== MAPPING HELPER ==========
    private Filiere mapToFiliere(ResultSet rs) throws SQLException {
        Filiere filiere = new Filiere();
        filiere.setIdFiliere(rs.getString("idFiliere"));
        filiere.setIdEcole(rs.getString("idEcole"));
        filiere.setCodeFiliere(rs.getString("codeFiliere"));
        filiere.setNomFiliere(rs.getString("nomFiliere"));
        filiere.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) filiere.setDeletedAt(deletedAt.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) filiere.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) filiere.setUpdatedAt(updatedAt.toLocalDateTime());

        return filiere;
    }
}