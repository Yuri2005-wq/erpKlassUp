package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.Eleve;
import org.erpklassup.erpklassup.models.Parent;
import org.erpklassup.erpklassup.models.ParentEleve;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParentDAO {

    // ========== CREATE ==========
    public boolean insert(Parent p) {
        if (p.getIdParent() == null) {
            p.setIdParent(UUID.randomUUID().toString());
        }
        String sql = """
            INSERT INTO Parent
                (idParent, idEcole, idUtilisateur, nom, prenom, sexe,
                 telephone, email, profession, adresse, photo, estActif)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, p.getIdParent());
            ps.setString(i++, p.getIdEcole());
            ps.setString(i++, p.getIdUtilisateur());
            ps.setString(i++, p.getNom());
            ps.setString(i++, p.getPrenom());
            ps.setString(i++, p.getSexe());
            ps.setString(i++, p.getTelephone());
            ps.setString(i++, p.getEmail());
            ps.setString(i++, p.getProfession());
            ps.setString(i++, p.getAdresse());
            ps.setString(i++, p.getPhoto());
            ps.setBoolean(i, p.isEstActif());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur insert parent : " + e.getMessage());
            return false;
        }
    }

    public boolean lierUtilisateur(String idParent, String idUtilisateur) {
        String sql = "UPDATE Parent SET idUtilisateur = ?, updated_at = NOW(3) WHERE idParent = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            ps.setString(2, idParent);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lierUtilisateur parent : " + e.getMessage());
            return false;
        }
    }

    public boolean lierEnfant(ParentEleve pe) {
        if (pe.getIdParentEleve() == null) {
            pe.setIdParentEleve(UUID.randomUUID().toString());
        }
        String sql = """
            INSERT INTO ParentEleve
                (idParentEleve, idEcole, idParent, idEleve, lienParente,
                 estContactPrincipal, estResponsableFinancier)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, pe.getIdParentEleve());
            ps.setString(i++, pe.getIdEcole());
            ps.setString(i++, pe.getIdParent());
            ps.setString(i++, pe.getIdEleve());
            ps.setString(i++, pe.getLienParente().name());
            ps.setBoolean(i++, pe.isContactPrincipal());
            ps.setBoolean(i, pe.isResponsableFinancier());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lierEnfant parent : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Parent> findById(String idParent) {
        String sql = "SELECT * FROM Parent WHERE idParent = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idParent);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById parent : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Parent> findByUtilisateurId(String idUtilisateur) {
        String sql = "SELECT * FROM Parent WHERE idUtilisateur = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateurId parent : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Parent> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Parent WHERE idEcole = ? AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Parent> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole parent : " + e.getMessage());
        }
        return result;
    }

    public List<Parent> findSansCompte(String idEcole) {
        String sql = "SELECT * FROM Parent WHERE idEcole = ? AND idUtilisateur IS NULL AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Parent> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findSansCompte parent : " + e.getMessage());
        }
        return result;
    }

    public List<Eleve> findEnfantsByParentId(String idParent) {
        String sql = """
            SELECT e.* FROM Eleve e
            INNER JOIN ParentEleve pe ON pe.idEleve = e.idEleve
            WHERE pe.idParent = ? AND pe.deleted_at IS NULL AND e.deleted_at IS NULL
            ORDER BY e.nom, e.prenom
            """;
        List<Eleve> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idParent);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapEleveRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findEnfantsByParentId : " + e.getMessage());
        }
        return result;
    }

    public List<Parent> findParentsByEleveId(String idEleve) {
        String sql = """
            SELECT p.* FROM Parent p
            INNER JOIN ParentEleve pe ON pe.idParent = p.idParent
            WHERE pe.idEleve = ? AND pe.deleted_at IS NULL AND p.deleted_at IS NULL
            ORDER BY pe.estContactPrincipal DESC, p.nom
            """;
        List<Parent> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEleve);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findParentsByEleveId : " + e.getMessage());
        }
        return result;
    }

    // ========== UPDATE ==========
    public boolean update(Parent p) {
        String sql = """
            UPDATE Parent SET
                nom = ?, prenom = ?, sexe = ?, telephone = ?, email = ?,
                profession = ?, adresse = ?, photo = ?, estActif = ?,
                version = version + 1
            WHERE idParent = ? AND version = ?
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, p.getNom());
            ps.setString(i++, p.getPrenom());
            ps.setString(i++, p.getSexe());
            ps.setString(i++, p.getTelephone());
            ps.setString(i++, p.getEmail());
            ps.setString(i++, p.getProfession());
            ps.setString(i++, p.getAdresse());
            ps.setString(i++, p.getPhoto());
            ps.setBoolean(i++, p.isEstActif());
            ps.setString(i++, p.getIdParent());
            ps.setLong(i, p.getVersion());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update parent : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idParent) {
        String sql = "UPDATE Parent SET deleted_at = NOW(3), estActif = 0 WHERE idParent = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idParent);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete parent : " + e.getMessage());
            return false;
        }
    }

    public boolean delierEnfant(String idParent, String idEleve) {
        String sql = "UPDATE ParentEleve SET deleted_at = NOW(3) WHERE idParent = ? AND idEleve = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idParent);
            ps.setString(2, idEleve);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur delierEnfant parent : " + e.getMessage());
            return false;
        }
    }

    // ========== MAPPING ==========
    private Parent mapRow(ResultSet rs) throws SQLException {
        Parent p = new Parent();
        p.setIdParent(rs.getString("idParent"));
        p.setIdEcole(rs.getString("idEcole"));
        p.setIdUtilisateur(rs.getString("idUtilisateur"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setSexe(rs.getString("sexe"));
        p.setTelephone(rs.getString("telephone"));
        p.setEmail(rs.getString("email"));
        p.setProfession(rs.getString("profession"));
        p.setAdresse(rs.getString("adresse"));
        p.setPhoto(rs.getString("photo"));
        p.setEstActif(rs.getBoolean("estActif"));
        p.setVersion(rs.getLong("version"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) p.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());
        return p;
    }

    private Eleve mapEleveRow(ResultSet rs) throws SQLException {
        Eleve e = new Eleve();
        e.setIdEleve(rs.getString("idEleve"));
        e.setIdEcole(rs.getString("idEcole"));
        e.setMatricule(rs.getString("matricule"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        return e;
    }
}