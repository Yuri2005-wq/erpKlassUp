package org.erpklassup.erpklassup.dao.Academique;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.Academique.Eleve;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EleveDAO {

    // ========== CREATE ==========
    public boolean insert(Eleve e) {
        if (e.getIdEleve() == null || e.getIdEleve().isBlank()) {
            e.setIdEleve(UUID.randomUUID().toString());
        }
        String sql = """
            INSERT INTO Eleve
                (idEleve, idEcole, idUtilisateur, matricule, nom, prenom, sexe,
                 dateNaissance, lieuNaissance, adresse, nationalite,
                 nomTuteur, telephoneTuteur, emailTuteur, photoPath, antecedentsMedicaux)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, e.getIdEleve());
            ps.setString(i++, e.getIdEcole());
            ps.setString(i++, e.getIdUtilisateur());
            ps.setString(i++, e.getMatricule());
            ps.setString(i++, e.getNom());
            ps.setString(i++, e.getPrenom());
            ps.setString(i++, e.getSexe());
            ps.setDate(i++, e.getDateNaissance() != null ? Date.valueOf(e.getDateNaissance()) : null);
            ps.setString(i++, e.getLieuNaissance());
            ps.setString(i++, e.getAdresse());
            ps.setString(i++, e.getNationalite());
            ps.setString(i++, e.getNomTuteur());
            ps.setString(i++, e.getTelephoneTuteur());
            ps.setString(i++, e.getEmailTuteur());
            ps.setString(i++, e.getPhotoPath());
            ps.setString(i, e.getAntecedentsMedicaux());

            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Erreur insert eleve : " + ex.getMessage());
            return false;
        }
    }

    public boolean lierUtilisateur(String idEleve, String idUtilisateur) {
        String sql = """
            UPDATE Eleve
            SET idUtilisateur = ?, version = version + 1, updated_at = NOW(3)
            WHERE idEleve = ? AND deleted_at IS NULL
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            ps.setString(2, idEleve);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lierUtilisateur eleve : " + e.getMessage());
            return false;
        }
    }

    public boolean lierUtilisateur(String idEcole, String idEleve, String idUtilisateur, long version) {
        String sql = """
            UPDATE Eleve
            SET idUtilisateur = ?, version = version + 1, updated_at = NOW(3)
            WHERE idEcole = ? AND idEleve = ? AND version = ? AND deleted_at IS NULL
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            ps.setString(2, idEcole);
            ps.setString(3, idEleve);
            ps.setLong(4, version);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lierUtilisateur eleve : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Eleve> findById(String idEleve) {
        String sql = "SELECT * FROM Eleve WHERE idEleve = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEleve);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById eleve : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Eleve> findById(String idEcole, String idEleve) {
        String sql = "SELECT * FROM Eleve WHERE idEcole = ? AND idEleve = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, idEleve);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById et ecole eleve : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Eleve> findByUtilisateurId(String idUtilisateur) {
        String sql = "SELECT * FROM Eleve WHERE idUtilisateur = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateurId eleve : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Eleve> findByMatricule(String idEcole, String matricule) {
        String sql = """
            SELECT * FROM Eleve
            WHERE idEcole = ? AND matricule = ? AND deleted_at IS NULL
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByMatricule eleve : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Eleve> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Eleve WHERE idEcole = ? AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Eleve> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole eleve : " + e.getMessage());
        }
        return result;
    }

    public List<Eleve> findSansCompte(String idEcole) {
        String sql = "SELECT * FROM Eleve WHERE idEcole = ? AND idUtilisateur IS NULL AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Eleve> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findSansCompte eleve : " + e.getMessage());
        }
        return result;
    }

    public List<Eleve> rechercher(String idEcole, String terme) {
        String sql = """
            SELECT * FROM Eleve
            WHERE idEcole = ? AND deleted_at IS NULL
              AND (nom LIKE ? OR prenom LIKE ? OR matricule LIKE ?)
            ORDER BY nom, prenom
            LIMIT 200
            """;
        List<Eleve> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String motif = "%" + (terme == null ? "" : terme.trim()) + "%";
            ps.setString(1, idEcole);
            ps.setString(2, motif);
            ps.setString(3, motif);
            ps.setString(4, motif);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur rechercher eleves : " + e.getMessage());
        }
        return result;
    }

    public boolean existeMatricule(String idEcole, String matricule, String idExclu) {
        String sql = """
            SELECT COUNT(*) FROM Eleve
            WHERE idEcole = ? AND matricule = ? AND deleted_at IS NULL
            """ + (idExclu != null ? " AND idEleve <> ?" : "");
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, matricule);
            if (idExclu != null) ps.setString(3, idExclu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur existeMatricule eleve : " + e.getMessage());
        }
        return false;
    }

    // ========== UPDATE ==========
    public boolean update(Eleve e) {
        String sql = """
            UPDATE Eleve SET
                matricule = ?, nom = ?, prenom = ?, sexe = ?, dateNaissance = ?,
                lieuNaissance = ?, adresse = ?, nationalite = ?, nomTuteur = ?,
                telephoneTuteur = ?, emailTuteur = ?, photoPath = ?,
                antecedentsMedicaux = ?, version = version + 1, updated_at = NOW(3)
            WHERE idEleve = ? AND idEcole = ? AND version = ? AND deleted_at IS NULL
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, e.getMatricule());
            ps.setString(i++, e.getNom());
            ps.setString(i++, e.getPrenom());
            ps.setString(i++, e.getSexe());
            ps.setDate(i++, e.getDateNaissance() != null ? Date.valueOf(e.getDateNaissance()) : null);
            ps.setString(i++, e.getLieuNaissance());
            ps.setString(i++, e.getAdresse());
            ps.setString(i++, e.getNationalite());
            ps.setString(i++, e.getNomTuteur());
            ps.setString(i++, e.getTelephoneTuteur());
            ps.setString(i++, e.getEmailTuteur());
            ps.setString(i++, e.getPhotoPath());
            ps.setString(i++, e.getAntecedentsMedicaux());
            ps.setString(i++, e.getIdEleve());
            ps.setString(i++, e.getIdEcole());
            ps.setLong(i, e.getVersion() != null ? e.getVersion() : 1L);

            boolean misAJour = ps.executeUpdate() > 0;
            if (misAJour) e.setVersion((e.getVersion() != null ? e.getVersion() : 1L) + 1);
            return misAJour;
        } catch (SQLException ex) {
            System.err.println("Erreur update eleve : " + ex.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idEleve) {
        String sql = """
            UPDATE Eleve
            SET deleted_at = NOW(3), version = version + 1, updated_at = NOW(3)
            WHERE idEleve = ? AND deleted_at IS NULL
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEleve);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete eleve : " + e.getMessage());
            return false;
        }
    }

    public boolean softDelete(String idEcole, String idEleve, long version) {
        String sql = """
            UPDATE Eleve
            SET deleted_at = NOW(3), version = version + 1, updated_at = NOW(3)
            WHERE idEcole = ? AND idEleve = ? AND version = ? AND deleted_at IS NULL
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, idEleve);
            ps.setLong(3, version);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete eleve : " + e.getMessage());
            return false;
        }
    }

    // ========== MAPPING ==========
    private Eleve mapRow(ResultSet rs) throws SQLException {
        Eleve e = new Eleve();
        e.setIdEleve(rs.getString("idEleve"));
        e.setIdEcole(rs.getString("idEcole"));
        e.setIdUtilisateur(rs.getString("idUtilisateur"));
        e.setMatricule(rs.getString("matricule"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        e.setSexe(rs.getString("sexe"));
        Date dateNaissance = rs.getDate("dateNaissance");
        e.setDateNaissance(dateNaissance != null ? dateNaissance.toLocalDate() : null);
        e.setLieuNaissance(rs.getString("lieuNaissance"));
        e.setAdresse(rs.getString("adresse"));
        e.setNationalite(rs.getString("nationalite"));
        e.setNomTuteur(rs.getString("nomTuteur"));
        e.setTelephoneTuteur(rs.getString("telephoneTuteur"));
        e.setEmailTuteur(rs.getString("emailTuteur"));
        e.setPhotoPath(rs.getString("photoPath"));
        e.setAntecedentsMedicaux(rs.getString("antecedentsMedicaux"));
        e.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (deletedAt != null) e.setDeletedAt(deletedAt.toLocalDateTime());
        if (createdAt != null) e.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) e.setUpdatedAt(updatedAt.toLocalDateTime());
        return e;
    }
}