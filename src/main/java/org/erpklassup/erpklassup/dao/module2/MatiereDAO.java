package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.Matiere;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MatiereDAO {

    // Base SELECT commune a toutes les lectures : jointures en lecture seule pour
    // alimenter nomMatiereParent et nomGroupeMatiere sans jamais toucher aux colonnes
    // reelles de Matiere. Une seule source de verite pour eviter les divergences de mapping.
    private static final String SELECT_BASE = """
            SELECT m.idMatiere, m.idEcole, m.idMatiereParent, m.idGroupeMatiere,
                   m.codeMatiere, m.nomMatiere, m.domaine, m.ordreAffichage, m.description,
                   m.version, m.deleted_at, m.created_at, m.updated_at,
                   p.nomMatiere AS nomMatiereParent,
                   g.nomGroupe  AS nomGroupeMatiere
            FROM Matiere m
            LEFT JOIN Matiere p ON p.idMatiere = m.idMatiereParent AND p.deleted_at IS NULL
            LEFT JOIN GroupeMatiere g ON g.idGroupe = m.idGroupeMatiere AND g.deleted_at IS NULL
            """;

    // ========== CREATE ==========
    public boolean create(Matiere matiere) {
        if (matiere.getIdMatiere() == null || matiere.getIdMatiere().isBlank()) {
            matiere.setIdMatiere(UUID.randomUUID().toString());
        }

        String sql = "INSERT INTO Matiere (idMatiere, idEcole, idMatiereParent, idGroupeMatiere, " +
                "codeMatiere, nomMatiere, domaine, ordreAffichage, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, matiere.getIdMatiere());
            stmt.setString(2, matiere.getIdEcole());
            stmt.setString(3, matiere.getIdMatiereParent());
            stmt.setString(4, matiere.getIdGroupeMatiere());
            stmt.setString(5, matiere.getCodeMatiere());
            stmt.setString(6, matiere.getNomMatiere());
            stmt.setString(7, matiere.getDomaine());
            stmt.setInt(8, matiere.getOrdreAffichage());
            stmt.setString(9, matiere.getDescription());

            return stmt.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Erreur create matiere : code deja utilise pour cette ecole - " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur create matiere : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Matiere> findById(String idMatiere) {
        String sql = SELECT_BASE + " WHERE m.idMatiere = ? AND m.deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idMatiere);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToMatiere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById matiere : " + e.getMessage());
        }
        return Optional.empty();
    }

    // Liste complete d'une ecole, ordonnee pour que chaque matiere apparaisse
    // juste apres sa racine (utile pour construire l'indentation hierarchique
    // dans la ListView du controller). Suppose un seul niveau d'imbrication.
    public List<Matiere> findByEcole(String idEcole) {
        String sql = SELECT_BASE + " WHERE m.idEcole = ? AND m.deleted_at IS NULL " +
                "ORDER BY COALESCE(m.idMatiereParent, m.idMatiere), (m.idMatiereParent IS NULL) DESC, " +
                "m.ordreAffichage, m.nomMatiere";
        List<Matiere> matieres = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) matieres.add(mapToMatiere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole matiere : " + e.getMessage());
        }
        return matieres;
    }

    // Uniquement les matieres racines (pour peupler le ComboBox "matiere parente",
    // qui ne doit pas proposer de sous-matiere comme parent d'une autre).
    public List<Matiere> findRacinesByEcole(String idEcole) {
        String sql = SELECT_BASE + " WHERE m.idEcole = ? AND m.deleted_at IS NULL " +
                "AND m.idMatiereParent IS NULL ORDER BY m.ordreAffichage, m.nomMatiere";
        List<Matiere> matieres = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) matieres.add(mapToMatiere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findRacinesByEcole matiere : " + e.getMessage());
        }
        return matieres;
    }

    public List<Matiere> search(String idEcole, String terme) {
        String sql = SELECT_BASE + " WHERE m.idEcole = ? AND m.deleted_at IS NULL " +
                "AND (m.nomMatiere LIKE ? OR m.codeMatiere LIKE ?) " +
                "ORDER BY m.ordreAffichage, m.nomMatiere";
        List<Matiere> matieres = new ArrayList<>();
        String motif = "%" + terme.trim() + "%";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, motif);
            stmt.setString(3, motif);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) matieres.add(mapToMatiere(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur search matiere : " + e.getMessage());
        }
        return matieres;
    }

    // ========== UPDATE ==========
    public boolean update(Matiere matiere) {
        String sql = "UPDATE Matiere SET idMatiereParent = ?, idGroupeMatiere = ?, " +
                "codeMatiere = ?, nomMatiere = ?, domaine = ?, ordreAffichage = ?, " +
                "description = ?, version = version + 1 WHERE idMatiere = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, matiere.getIdMatiereParent());
            stmt.setString(2, matiere.getIdGroupeMatiere());
            stmt.setString(3, matiere.getCodeMatiere());
            stmt.setString(4, matiere.getNomMatiere());
            stmt.setString(5, matiere.getDomaine());
            stmt.setInt(6, matiere.getOrdreAffichage());
            stmt.setString(7, matiere.getDescription());
            stmt.setString(8, matiere.getIdMatiere());

            return stmt.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Erreur update matiere : code deja utilise pour cette ecole - " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur update matiere : " + e.getMessage());
            return false;
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idMatiere) {
        String sql = "UPDATE Matiere SET deleted_at = NOW(3), version = version + 1 " +
                "WHERE idMatiere = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idMatiere);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete matiere : " + e.getMessage());
            return false;
        }
    }

    // ========== VALIDATIONS METIER ==========

    // A appeler avant create/update pour donner un message clair dans le tiroir
    // plutot que de laisser remonter une SQLIntegrityConstraintViolationException.
    public boolean codeExiste(String idEcole, String codeMatiere, String idMatiereAExclure) {
        String sql = "SELECT 1 FROM Matiere WHERE idEcole = ? AND codeMatiere = ? " +
                "AND deleted_at IS NULL AND idMatiere != ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, codeMatiere);
            // COALESCE cote Java : si aucune exclusion (creation), on compare a une valeur impossible
            stmt.setString(3, idMatiereAExclure != null ? idMatiereAExclure : "");

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erreur codeExiste matiere : " + e.getMessage());
            return false;
        }
    }

    // A appeler avant softDelete d'une matiere racine, pour avertir l'utilisateur
    // que des sous-matieres actives en dependent (fk_mat_parent est en RESTRICT,
    // mais on ne fait que du soft delete donc la FK ne bloquera rien : c'est a l'appli
    // de proteger la coherence fonctionnelle du bulletin).
    public boolean aDesEnfantsActifs(String idMatiere) {
        String sql = "SELECT 1 FROM Matiere WHERE idMatiereParent = ? AND deleted_at IS NULL LIMIT 1";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idMatiere);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erreur aDesEnfantsActifs matiere : " + e.getMessage());
            return false;
        }
    }

    // ========== HELPER DE MAPPING ==========
    private Matiere mapToMatiere(ResultSet rs) throws SQLException {
        Matiere matiere = new Matiere();
        matiere.setIdMatiere(rs.getString("idMatiere"));
        matiere.setIdEcole(rs.getString("idEcole"));
        matiere.setIdMatiereParent(rs.getString("idMatiereParent"));
        matiere.setIdGroupeMatiere(rs.getString("idGroupeMatiere"));
        matiere.setCodeMatiere(rs.getString("codeMatiere"));
        matiere.setNomMatiere(rs.getString("nomMatiere"));
        matiere.setDomaine(rs.getString("domaine"));
        matiere.setOrdreAffichage(rs.getInt("ordreAffichage"));
        matiere.setDescription(rs.getString("description"));
        matiere.setNomMatiereParent(rs.getString("nomMatiereParent"));
        matiere.setNomGroupeMatiere(rs.getString("nomGroupeMatiere"));
        matiere.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) matiere.setDeletedAt(deletedAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) matiere.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) matiere.setUpdatedAt(updatedAt.toLocalDateTime());

        return matiere;
    }
}