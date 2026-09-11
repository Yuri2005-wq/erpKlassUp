package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.Personnel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PersonnelDAO {

    // ========== CREATE ==========
    public boolean insert(Personnel p) {
        if (p.getIdPersonnel() == null) {
            p.setIdPersonnel(UUID.randomUUID().toString());
        }
        String sql = """
            INSERT INTO Personnel
                (idPersonnel, idEcole, idUtilisateur, matriculeInterne, nom, prenom, sexe,
                 dateNaissance, telephone, contactUrgence, email, cniOuNiu,
                 typePersonnel, posteOuFonction, qualification, statutContractuel,
                 dateEmbauche, photo, estActif)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, p.getIdPersonnel());
            ps.setString(i++, p.getIdEcole());
            ps.setString(i++, p.getIdUtilisateur());
            ps.setString(i++, p.getMatriculeInterne());
            ps.setString(i++, p.getNom());
            ps.setString(i++, p.getPrenom());
            ps.setString(i++, p.getSexe());
            ps.setDate(i++, p.getDateNaissance() != null ? Date.valueOf(p.getDateNaissance()) : null);
            ps.setString(i++, p.getTelephone());
            ps.setString(i++, p.getContactUrgence());
            ps.setString(i++, p.getEmail());
            ps.setString(i++, p.getCniOuNiu());
            ps.setString(i++, p.getTypePersonnel());
            ps.setString(i++, p.getPosteOuFonction());
            ps.setString(i++, p.getQualification());
            ps.setString(i++, p.getStatutContractuel());
            ps.setDate(i++, p.getDateEmbauche() != null ? Date.valueOf(p.getDateEmbauche()) : null);
            ps.setString(i++, p.getPhoto());
            ps.setBoolean(i, p.isEstActif());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur insert personnel : " + e.getMessage());
            return false;
        }
    }

    public boolean lierUtilisateur(String idPersonnel, String idUtilisateur) {
        String sql = "UPDATE Personnel SET idUtilisateur = ?, updated_at = NOW(3) WHERE idPersonnel = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            ps.setString(2, idPersonnel);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lierUtilisateur personnel : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Personnel> findById(String idPersonnel) {
        String sql = "SELECT * FROM Personnel WHERE idPersonnel = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPersonnel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById personnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Personnel> findByUtilisateurId(String idUtilisateur) {
        String sql = "SELECT * FROM Personnel WHERE idUtilisateur = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateurId personnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Personnel> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole personnel : " + e.getMessage());
        }
        return result;
    }

    public List<Personnel> findSansCompte(String idEcole) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND idUtilisateur IS NULL "
                + "AND estActif = 1 AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findSansCompte personnel : " + e.getMessage());
        }
        return result;
    }

    // ========== UPDATE ==========
    public boolean update(Personnel p) {
        String sql = """
            UPDATE Personnel SET
                nom = ?, prenom = ?, sexe = ?, dateNaissance = ?, telephone = ?,
                contactUrgence = ?, email = ?, cniOuNiu = ?, typePersonnel = ?,
                posteOuFonction = ?, qualification = ?, statutContractuel = ?,
                dateEmbauche = ?, photo = ?, estActif = ?,
                version = version + 1
            WHERE idPersonnel = ? AND version = ?
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, p.getNom());
            ps.setString(i++, p.getPrenom());
            ps.setString(i++, p.getSexe());
            ps.setDate(i++, p.getDateNaissance() != null ? Date.valueOf(p.getDateNaissance()) : null);
            ps.setString(i++, p.getTelephone());
            ps.setString(i++, p.getContactUrgence());
            ps.setString(i++, p.getEmail());
            ps.setString(i++, p.getCniOuNiu());
            ps.setString(i++, p.getTypePersonnel());
            ps.setString(i++, p.getPosteOuFonction());
            ps.setString(i++, p.getQualification());
            ps.setString(i++, p.getStatutContractuel());
            ps.setDate(i++, p.getDateEmbauche() != null ? Date.valueOf(p.getDateEmbauche()) : null);
            ps.setString(i++, p.getPhoto());
            ps.setBoolean(i++, p.isEstActif());
            ps.setString(i++, p.getIdPersonnel());
            ps.setLong(i, p.getVersion());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update personnel : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idPersonnel) {
        String sql = "UPDATE Personnel SET deleted_at = NOW(3), estActif = 0 WHERE idPersonnel = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPersonnel);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete personnel : " + e.getMessage());
            return false;
        }
    }

    // ========== MAPPING ==========
    private Personnel mapRow(ResultSet rs) throws SQLException {
        Personnel p = new Personnel();
        p.setIdPersonnel(rs.getString("idPersonnel"));
        p.setIdEcole(rs.getString("idEcole"));
        p.setIdUtilisateur(rs.getString("idUtilisateur"));
        p.setMatriculeInterne(rs.getString("matriculeInterne"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setSexe(rs.getString("sexe"));
        Date dateNaissance = rs.getDate("dateNaissance");
        p.setDateNaissance(dateNaissance != null ? dateNaissance.toLocalDate() : null);
        p.setTelephone(rs.getString("telephone"));
        p.setContactUrgence(rs.getString("contactUrgence"));
        p.setEmail(rs.getString("email"));
        p.setCniOuNiu(rs.getString("cniOuNiu"));
        p.setTypePersonnel(rs.getString("typePersonnel"));
        p.setPosteOuFonction(rs.getString("posteOuFonction"));
        p.setQualification(rs.getString("qualification"));
        p.setStatutContractuel(rs.getString("statutContractuel"));
        Date dateEmbauche = rs.getDate("dateEmbauche");
        p.setDateEmbauche(dateEmbauche != null ? dateEmbauche.toLocalDate() : null);
        p.setPhoto(rs.getString("photo"));
        p.setEstActif(rs.getBoolean("estActif"));
        p.setVersion(rs.getLong("version"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) p.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());
        return p;
    }
}