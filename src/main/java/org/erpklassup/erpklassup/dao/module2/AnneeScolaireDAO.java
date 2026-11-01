package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour la table AnneeScolaire.
 *
 * <p>Gère les opérations CRUD + logique métier :
 * <ul>
 *   <li>Une seule année courante par école (garanti par contrainte SQL)</li>
 *   <li>Le passage au statut CLOTUREE/ARCHIVEE est irréversible</li>
 *   <li>Suppression bloquée si des classes sont rattachées</li>
 * </ul>
 * </p>
 */
public class AnneeScolaireDAO {

    /* ============================================================
       CREATE
       ============================================================ */
    public boolean create(AnneeScolaire annee) {
        String sql = """
            INSERT INTO AnneeScolaire
                (idAnnee, idEcole, libelle, dateDebut, dateFin, estCourante, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, annee.getId());
            stmt.setString(2, annee.getIdEcole());
            stmt.setString(3, annee.getLibelle());
            stmt.setDate(4, Date.valueOf(annee.getDateDebut()));
            stmt.setDate(5, Date.valueOf(annee.getDateFin()));
            stmt.setBoolean(6, annee.isEstCourante());
            stmt.setString(7, annee.getStatut().name());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur create AnneeScolaire : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       READ
       ============================================================ */
    public Optional<AnneeScolaire> findById(String idAnnee) {
        String sql = "SELECT * FROM AnneeScolaire WHERE idAnnee = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToAnnee(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById AnneeScolaire : " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Récupère l'année courante d'une école.
     * Retourne Optional.empty() si aucune n'est marquée courante.
     */
    public Optional<AnneeScolaire> findCourante(String idEcole) {
        String sql = """
            SELECT * FROM AnneeScolaire
            WHERE idEcole = ? AND estCourante = 1 AND deleted_at IS NULL
            LIMIT 1
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToAnnee(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findCourante : " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Liste toutes les années d'une école, triées par date de début décroissante.
     */
    public List<AnneeScolaire> findByEcole(String idEcole) {
        String sql = """
            SELECT * FROM AnneeScolaire
            WHERE idEcole = ? AND deleted_at IS NULL
            ORDER BY dateDebut DESC
            """;

        List<AnneeScolaire> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToAnnee(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByEcole AnneeScolaire : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Recherche textuelle (libellé).
     */
    public List<AnneeScolaire> rechercher(String idEcole, String terme) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM AnneeScolaire
            WHERE idEcole = ? AND deleted_at IS NULL
            """);

        List<Object> params = new ArrayList<>();
        params.add(idEcole);

        if (terme != null && !terme.isBlank()) {
            sql.append(" AND libelle LIKE ? ");
            params.add("%" + terme.trim() + "%");
        }
        sql.append(" ORDER BY dateDebut DESC");

        List<AnneeScolaire> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            for (Object p : params) stmt.setObject(i++, p);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToAnnee(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur rechercher AnneeScolaire : " + e.getMessage());
        }
        return resultat;
    }

    /* ============================================================
       UPDATE
       ============================================================ */
    public boolean update(AnneeScolaire annee) {
        String sql = """
            UPDATE AnneeScolaire SET
                libelle = ?, dateDebut = ?, dateFin = ?,
                estCourante = ?, statut = ?, version = version + 1
            WHERE idAnnee = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, annee.getLibelle());
            stmt.setDate(2, Date.valueOf(annee.getDateDebut()));
            stmt.setDate(3, Date.valueOf(annee.getDateFin()));
            stmt.setBoolean(4, annee.isEstCourante());
            stmt.setString(5, annee.getStatut().name());
            stmt.setString(6, annee.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur update AnneeScolaire : " + e.getMessage());
            return false;
        }
    }

    /**
     * Définit une année comme courante et retire le flag des autres.
     * Transaction atomique pour garantir l'unicité.
     */
    public boolean definirCourante(String idAnnee, String idEcole) {
        String sqlReset = """
            UPDATE AnneeScolaire SET estCourante = 0, version = version + 1
            WHERE idEcole = ? AND estCourante = 1 AND deleted_at IS NULL
            """;
        String sqlSet = """
            UPDATE AnneeScolaire SET estCourante = 1, version = version + 1
            WHERE idAnnee = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtReset = conn.prepareStatement(sqlReset);
                 PreparedStatement stmtSet = conn.prepareStatement(sqlSet)) {

                stmtReset.setString(1, idEcole);
                stmtReset.executeUpdate();

                stmtSet.setString(1, idAnnee);
                stmtSet.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur definirCourante : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       DELETE (SOFT)
       ============================================================ */
    public boolean softDelete(String idAnnee) {
        String sql = """
            UPDATE AnneeScolaire SET deleted_at = NOW(3), version = version + 1
            WHERE idAnnee = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur softDelete AnneeScolaire : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       MÉTHODES MÉTIER
       ============================================================ */

    /**
     * Compte les classes rattachées à une année.
     * Utilisé avant suppression pour éviter les FK orphelines.
     */
    public long compterClasses(String idAnnee) {
        String sql = "SELECT COUNT(*) FROM Classe WHERE idAnnee = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterClasses : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Vérifie si un libellé existe déjà pour cette école.
     */
    public boolean libelleExiste(String idEcole, String libelle, String idExclu) {
        String sql = """
            SELECT COUNT(*) FROM AnneeScolaire
            WHERE idEcole = ? AND libelle = ? AND deleted_at IS NULL
            """ + (idExclu != null ? " AND idAnnee != ?" : "");

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, libelle);
            if (idExclu != null) stmt.setString(3, idExclu);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur libelleExiste : " + e.getMessage());
        }
        return false;
    }

    /* ============================================================
       MAPPING
       ============================================================ */
    private AnneeScolaire mapToAnnee(ResultSet rs) throws SQLException {
        AnneeScolaire a = new AnneeScolaire();
        a.setId(rs.getString("idAnnee"));
        a.setIdEcole(rs.getString("idEcole"));
        a.setLibelle(rs.getString("libelle"));

        Date debut = rs.getDate("dateDebut");
        if (debut != null) a.setDateDebut(debut.toLocalDate());

        Date fin = rs.getDate("dateFin");
        if (fin != null) a.setDateFin(fin.toLocalDate());

        a.setEstCourante(rs.getBoolean("estCourante"));

        String statut = rs.getString("statut");
        if (statut != null) a.setStatut(AnneeScolaire.Statut.valueOf(statut));

        // Champs BaseEntity
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) a.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) a.setUpdatedAt(updatedAt.toLocalDateTime());

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) a.setDeletedAt(deletedAt.toLocalDateTime());

        return a;
    }
}