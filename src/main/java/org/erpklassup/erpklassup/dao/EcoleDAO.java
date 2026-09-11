package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.Ecole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EcoleDAO {

    // ========== CREATE ==========
    public boolean create(Ecole ecole) {
        String sql = "INSERT INTO Ecole (idEcole, idGroupe, codeEcole, nomEcole, " +
                "numeroAgrement, numeroArreteOuverture, nomPromoteur, inspectionRef, " +
                "delegationDept, region, sousSysteme, niveauEnseignement, adresse, " +
                "telephone, email, devise, logoPath, cleSecrete, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ecole.getIdEcole());
            stmt.setString(2, ecole.getIdGroupe());
            stmt.setString(3, ecole.getCodeEcole());
            stmt.setString(4, ecole.getNomEcole());
            stmt.setString(5, ecole.getNumeroAgrement());
            stmt.setString(6, ecole.getNumeroArreteOuverture());
            stmt.setString(7, ecole.getNomPromoteur());
            stmt.setString(8, ecole.getInspectionRef());
            stmt.setString(9, ecole.getDelegationDept());
            stmt.setString(10, ecole.getRegion());
            stmt.setString(11, ecole.getSousSysteme());
            stmt.setString(12, ecole.getNiveauEnseignement());
            stmt.setString(13, ecole.getAdresse());
            stmt.setString(14, ecole.getTelephone());
            stmt.setString(15, ecole.getEmail());
            stmt.setString(16, ecole.getDevise());
            stmt.setString(17, ecole.getLogoPath());
            stmt.setString(18, ecole.getCleSecrete());
            stmt.setBoolean(19, ecole.isActive());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur create école : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========

    /**
     * Récupère la PREMIÈRE école active (une seule école en BD)
     */
    public Optional<Ecole> findPremiereEcoleActive() {
        String sql = "SELECT * FROM Ecole WHERE is_active = 1 AND deleted_at IS NULL " +
                "ORDER BY created_at ASC LIMIT 1";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return Optional.of(mapToEcole(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findPremiereEcoleActive : " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Récupère une école par son ID
     */
    public Optional<Ecole> findById(String idEcole) {
        String sql = "SELECT * FROM Ecole WHERE idEcole = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToEcole(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findById école : " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Récupère toutes les écoles actives
     */
    public List<Ecole> findAllActives() {
        String sql = "SELECT * FROM Ecole WHERE is_active = 1 AND deleted_at IS NULL ORDER BY nomEcole";
        List<Ecole> ecoles = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ecoles.add(mapToEcole(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findAllActives : " + e.getMessage());
        }

        return ecoles;
    }

    // ========== UPDATE ==========
    public boolean update(Ecole ecole) {
        String sql = "UPDATE Ecole SET idGroupe = ?, codeEcole = ?, nomEcole = ?, " +
                "numeroAgrement = ?, numeroArreteOuverture = ?, nomPromoteur = ?, " +
                "inspectionRef = ?, delegationDept = ?, region = ?, sousSysteme = ?, " +
                "niveauEnseignement = ?, adresse = ?, telephone = ?, email = ?, " +
                "devise = ?, logoPath = ?, cleSecrete = ?, is_active = ?, " +
                "version = version + 1 WHERE idEcole = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ecole.getIdGroupe());
            stmt.setString(2, ecole.getCodeEcole());
            stmt.setString(3, ecole.getNomEcole());
            stmt.setString(4, ecole.getNumeroAgrement());
            stmt.setString(5, ecole.getNumeroArreteOuverture());
            stmt.setString(6, ecole.getNomPromoteur());
            stmt.setString(7, ecole.getInspectionRef());
            stmt.setString(8, ecole.getDelegationDept());
            stmt.setString(9, ecole.getRegion());
            stmt.setString(10, ecole.getSousSysteme());
            stmt.setString(11, ecole.getNiveauEnseignement());
            stmt.setString(12, ecole.getAdresse());
            stmt.setString(13, ecole.getTelephone());
            stmt.setString(14, ecole.getEmail());
            stmt.setString(15, ecole.getDevise());
            stmt.setString(16, ecole.getLogoPath());
            stmt.setString(17, ecole.getCleSecrete());
            stmt.setBoolean(18, ecole.isActive());
            stmt.setString(19, ecole.getIdEcole());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur update école : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idEcole) {
        String sql = "UPDATE Ecole SET deleted_at = NOW(3), version = version + 1 WHERE idEcole = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur softDelete école : " + e.getMessage());
            return false;
        }
    }

    // ========== HELPER ==========
    private Ecole mapToEcole(ResultSet rs) throws SQLException {
        Ecole ecole = new Ecole();
        ecole.setIdEcole(rs.getString("idEcole"));
        ecole.setIdGroupe(rs.getString("idGroupe"));
        ecole.setCodeEcole(rs.getString("codeEcole"));
        ecole.setNomEcole(rs.getString("nomEcole"));
        ecole.setNumeroAgrement(rs.getString("numeroAgrement"));
        ecole.setNumeroArreteOuverture(rs.getString("numeroArreteOuverture"));
        ecole.setNomPromoteur(rs.getString("nomPromoteur"));
        ecole.setInspectionRef(rs.getString("inspectionRef"));
        ecole.setDelegationDept(rs.getString("delegationDept"));
        ecole.setRegion(rs.getString("region"));
        ecole.setSousSysteme(rs.getString("sousSysteme"));
        ecole.setNiveauEnseignement(rs.getString("niveauEnseignement"));
        ecole.setAdresse(rs.getString("adresse"));
        ecole.setTelephone(rs.getString("telephone"));
        ecole.setEmail(rs.getString("email"));
        ecole.setDevise(rs.getString("devise"));
        ecole.setLogoPath(rs.getString("logoPath"));
        ecole.setCleSecrete(rs.getString("cleSecrete"));
        ecole.setActive(rs.getBoolean("is_active"));
        ecole.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) ecole.setDeletedAt(deletedAt.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) ecole.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) ecole.setUpdatedAt(updatedAt.toLocalDateTime());

        return ecole;
    }
}