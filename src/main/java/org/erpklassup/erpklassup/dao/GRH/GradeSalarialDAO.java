package org.erpklassup.erpklassup.dao.GRH;


import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.GRH.GradeSalarial;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GradeSalarialDAO {

    public boolean create(GradeSalarial g) {
        String sql = "INSERT INTO GradeSalarial " +
                "(idGrade, idEcole, code, libelle, categorie, echelon, salaireBaseReference) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, g.getIdGrade());
            stmt.setString(2, g.getIdEcole());
            stmt.setString(3, g.getCode());
            stmt.setString(4, g.getLibelle());
            setNullableString(stmt, 5, g.getCategorie());
            setNullableString(stmt, 6, g.getEchelon());
            stmt.setBigDecimal(7, g.getSalaireBaseReference() == null ? BigDecimal.ZERO : g.getSalaireBaseReference());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create GradeSalarial : " + e.getMessage());
            return false;
        }
    }

    public Optional<GradeSalarial> findById(String id) {
        String sql = "SELECT * FROM GradeSalarial WHERE idGrade = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById GradeSalarial : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<GradeSalarial> findByCode(String idEcole, String code) {
        String sql = "SELECT * FROM GradeSalarial WHERE idEcole = ? AND code = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            stmt.setString(2, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByCode GradeSalarial : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<GradeSalarial> findByEcole(String idEcole) {
        String sql = "SELECT * FROM GradeSalarial WHERE idEcole = ? AND deleted_at IS NULL " +
                "ORDER BY categorie, echelon, code";
        List<GradeSalarial> liste = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) liste.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole GradeSalarial : " + e.getMessage());
        }
        return liste;
    }

    public List<GradeSalarial> findByCategorie(String idEcole, String categorie) {
        String sql = "SELECT * FROM GradeSalarial WHERE idEcole = ? AND categorie = ? " +
                "AND deleted_at IS NULL ORDER BY echelon";
        List<GradeSalarial> liste = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            stmt.setString(2, categorie);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) liste.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByCategorie GradeSalarial : " + e.getMessage());
        }
        return liste;
    }

    public boolean update(GradeSalarial g) {
        String sql = "UPDATE GradeSalarial SET code = ?, libelle = ?, categorie = ?, " +
                "echelon = ?, salaireBaseReference = ?, version = version + 1 " +
                "WHERE idGrade = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, g.getCode());
            stmt.setString(2, g.getLibelle());
            setNullableString(stmt, 3, g.getCategorie());
            setNullableString(stmt, 4, g.getEchelon());
            stmt.setBigDecimal(5, g.getSalaireBaseReference() == null ? BigDecimal.ZERO : g.getSalaireBaseReference());
            stmt.setString(6, g.getIdGrade());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update GradeSalarial : " + e.getMessage());
            return false;
        }
    }

    public boolean softDelete(String id) {
        String sql = "UPDATE GradeSalarial SET deleted_at = NOW(3), version = version + 1 " +
                "WHERE idGrade = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete GradeSalarial : " + e.getMessage());
            return false;
        }
    }

    private GradeSalarial map(ResultSet rs) throws SQLException {
        GradeSalarial g = new GradeSalarial();
        g.setIdGrade(rs.getString("idGrade"));
        g.setIdEcole(rs.getString("idEcole"));
        g.setCode(rs.getString("code"));
        g.setLibelle(rs.getString("libelle"));
        g.setCategorie(rs.getString("categorie"));
        g.setEchelon(rs.getString("echelon"));
        g.setSalaireBaseReference(rs.getBigDecimal("salaireBaseReference"));
        g.setVersion(rs.getLong("version"));
        Timestamp d = rs.getTimestamp("deleted_at"); if (d != null) g.setDeletedAt(d.toLocalDateTime());
        Timestamp c = rs.getTimestamp("created_at"); if (c != null) g.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); if (u != null) g.setUpdatedAt(u.toLocalDateTime());
        return g;
    }

    private static void setNullableString(PreparedStatement stmt, int idx, String val) throws SQLException {
        if (val == null || val.isBlank()) stmt.setNull(idx, Types.VARCHAR);
        else stmt.setString(idx, val);
    }
}
