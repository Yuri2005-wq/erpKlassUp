package org.erpklassup.erpklassup.dao.GRH;


import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.GRH.DossierPersonnel;

import java.sql.*;
import java.util.Optional;

public class DossierPersonnelDAO {

    public boolean create(DossierPersonnel d) {
        String sql = "INSERT INTO DossierPersonnel " +
                "(idDossier, idEcole, idPersonnel, idDepartement, idSuperieur, idGrade, " +
                " numeroCNPS, nombreEnfantsCharge, modePaiementPrefere, banque, " +
                " numeroCompte, numeroMobileMoney) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, d.getIdDossier());
            stmt.setString(2, d.getIdEcole());
            stmt.setString(3, d.getIdPersonnel());
            setNullableString(stmt, 4, d.getIdDepartement());
            setNullableString(stmt, 5, d.getIdSuperieur());
            setNullableString(stmt, 6, d.getIdGrade());
            setNullableString(stmt, 7, d.getNumeroCNPS());
            stmt.setInt(8, d.getNombreEnfantsCharge());
            setNullableString(stmt, 9, d.getModePaiementPrefere());
            setNullableString(stmt, 10, d.getBanque());
            setNullableString(stmt, 11, d.getNumeroCompte());
            setNullableString(stmt, 12, d.getNumeroMobileMoney());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create DossierPersonnel : " + e.getMessage());
            return false;
        }
    }

    public Optional<DossierPersonnel> findById(String id) {
        String sql = "SELECT * FROM DossierPersonnel WHERE idDossier = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById DossierPersonnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<DossierPersonnel> findByPersonnel(String idPersonnel) {
        String sql = "SELECT * FROM DossierPersonnel WHERE idPersonnel = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idPersonnel);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByPersonnel DossierPersonnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<DossierPersonnel> findByNumeroCNPS(String idEcole, String numeroCNPS) {
        String sql = "SELECT * FROM DossierPersonnel WHERE idEcole = ? AND numeroCNPS = ? " +
                "AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            stmt.setString(2, numeroCNPS);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByNumeroCNPS : " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean update(DossierPersonnel d) {
        String sql = "UPDATE DossierPersonnel SET idDepartement = ?, idSuperieur = ?, " +
                "idGrade = ?, numeroCNPS = ?, nombreEnfantsCharge = ?, " +
                "modePaiementPrefere = ?, banque = ?, numeroCompte = ?, " +
                "numeroMobileMoney = ?, version = version + 1 " +
                "WHERE idDossier = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableString(stmt, 1, d.getIdDepartement());
            setNullableString(stmt, 2, d.getIdSuperieur());
            setNullableString(stmt, 3, d.getIdGrade());
            setNullableString(stmt, 4, d.getNumeroCNPS());
            stmt.setInt(5, d.getNombreEnfantsCharge());
            setNullableString(stmt, 6, d.getModePaiementPrefere());
            setNullableString(stmt, 7, d.getBanque());
            setNullableString(stmt, 8, d.getNumeroCompte());
            setNullableString(stmt, 9, d.getNumeroMobileMoney());
            stmt.setString(10, d.getIdDossier());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update DossierPersonnel : " + e.getMessage());
            return false;
        }
    }

    public boolean softDelete(String id) {
        String sql = "UPDATE DossierPersonnel SET deleted_at = NOW(3), version = version + 1 " +
                "WHERE idDossier = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete DossierPersonnel : " + e.getMessage());
            return false;
        }
    }

    private DossierPersonnel map(ResultSet rs) throws SQLException {
        DossierPersonnel d = new DossierPersonnel();
        d.setIdDossier(rs.getString("idDossier"));
        d.setIdEcole(rs.getString("idEcole"));
        d.setIdPersonnel(rs.getString("idPersonnel"));
        d.setIdDepartement(rs.getString("idDepartement"));
        d.setIdSuperieur(rs.getString("idSuperieur"));
        d.setIdGrade(rs.getString("idGrade"));
        d.setNumeroCNPS(rs.getString("numeroCNPS"));
        d.setNombreEnfantsCharge(rs.getInt("nombreEnfantsCharge"));
        d.setModePaiementPrefere(rs.getString("modePaiementPrefere"));
        d.setBanque(rs.getString("banque"));
        d.setNumeroCompte(rs.getString("numeroCompte"));
        d.setNumeroMobileMoney(rs.getString("numeroMobileMoney"));
        d.setVersion(rs.getLong("version"));
        Timestamp da = rs.getTimestamp("deleted_at"); if (da != null) d.setDeletedAt(da.toLocalDateTime());
        Timestamp c  = rs.getTimestamp("created_at"); if (c  != null) d.setCreatedAt(c.toLocalDateTime());
        Timestamp u  = rs.getTimestamp("updated_at"); if (u  != null) d.setUpdatedAt(u.toLocalDateTime());
        return d;
    }

    private static void setNullableString(PreparedStatement stmt, int idx, String val) throws SQLException {
        if (val == null || val.isBlank()) stmt.setNull(idx, Types.VARCHAR);
        else stmt.setString(idx, val);
    }
}
