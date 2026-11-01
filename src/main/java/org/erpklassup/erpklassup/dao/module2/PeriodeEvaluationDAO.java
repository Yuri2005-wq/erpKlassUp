package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.PeriodeEvaluation;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO pour la table PeriodeEvaluation.
 *
 * <p>Gère l'arborescence des périodes :
 * <pre>
 *   Trimestre 1
 *   ├── Séquence 1
 *   └── Séquence 2
 * </pre>
 * </p>
 */
public class PeriodeEvaluationDAO {

    /* ============================================================
       CREATE
       ============================================================ */
    public boolean create(PeriodeEvaluation periode) {
        String sql = """
            INSERT INTO PeriodeEvaluation
                (idPeriode, idEcole, idAnnee, idPeriodeParent,
                 codePeriode, libelle, typePeriode, numeroOrdre,
                 poids, dateDebut, dateFin, estCloturee)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            remplirParametresCreation(stmt, periode);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur create PeriodeEvaluation : " + e.getMessage());
            return false;
        }
    }

    /**
     * Insertion en lot, dans une seule transaction — utilisée par la
     * génération automatique de la structure standard (ex. 3 trimestres
     * + 6 séquences en une fois). Tout échoue ou tout réussit ; aucune
     * ligne partielle ne peut rester en base.
     */
    public boolean creerLot(List<PeriodeEvaluation> periodes) {
        if (periodes == null || periodes.isEmpty()) return true;

        String sql = """
            INSERT INTO PeriodeEvaluation
                (idPeriode, idEcole, idAnnee, idPeriodeParent,
                 codePeriode, libelle, typePeriode, numeroOrdre,
                 poids, dateDebut, dateFin, estCloturee)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Connection conn = null;
        try {
            conn = Database.getConnexion();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (PeriodeEvaluation periode : periodes) {
                    remplirParametresCreation(stmt, periode);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Erreur creerLot PeriodeEvaluation, rollback : " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    System.err.println("❌ Erreur rollback creerLot : " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("❌ Erreur fermeture connexion creerLot : " + e.getMessage());
                }
            }
        }
    }

    private void remplirParametresCreation(PreparedStatement stmt, PeriodeEvaluation periode) throws SQLException {
        stmt.setString(1, periode.getId());
        stmt.setString(2, periode.getIdEcole());
        stmt.setString(3, periode.getIdAnnee());
        stmt.setString(4, periode.getIdPeriodeParent());
        stmt.setString(5, periode.getCodePeriode());
        stmt.setString(6, periode.getLibelle());
        stmt.setString(7, periode.getTypePeriode());
        stmt.setInt(8, periode.getNumeroOrdre());
        stmt.setBigDecimal(9, periode.getPoids());
        stmt.setDate(10, Date.valueOf(periode.getDateDebut()));
        stmt.setDate(11, Date.valueOf(periode.getDateFin()));
        stmt.setBoolean(12, periode.isEstCloturee());
    }

    /* ============================================================
       READ
       ============================================================ */
    public Optional<PeriodeEvaluation> findById(String idPeriode) {
        String sql = """
            SELECT * FROM PeriodeEvaluation
            WHERE idPeriode = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToPeriode(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById PeriodeEvaluation : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<PeriodeEvaluation> findByAnnee(String idAnnee) {
        String sql = """
            SELECT * FROM PeriodeEvaluation
            WHERE idAnnee = ? AND deleted_at IS NULL
            ORDER BY numeroOrdre ASC
            """;

        List<PeriodeEvaluation> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToPeriode(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByAnnee PeriodeEvaluation : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Retourne l'arborescence hiérarchisée des périodes d'une année.
     * Chaque période racine contient sa liste de sous-périodes, triées
     * explicitement par numeroOrdre — le ORDER BY de findByAnnee() ne
     * suffit pas car il trie TOUTES les lignes (trimestres et séquences)
     * ensemble, et numeroOrdre repart à 1 à chaque niveau.
     */
    public List<PeriodeEvaluation> findArborescence(String idAnnee) {
        List<PeriodeEvaluation> toutes = findByAnnee(idAnnee);

        Map<String, PeriodeEvaluation> parId = new LinkedHashMap<>();
        for (PeriodeEvaluation p : toutes) parId.put(p.getId(), p);

        List<PeriodeEvaluation> racines = new ArrayList<>();

        for (PeriodeEvaluation p : toutes) {
            if (p.getIdPeriodeParent() == null) {
                racines.add(p);
            } else {
                PeriodeEvaluation parent = parId.get(p.getIdPeriodeParent());
                if (parent != null) {
                    parent.getSousPeriodes().add(p);
                }
            }
        }

        Comparator<PeriodeEvaluation> parOrdre = Comparator.comparingInt(PeriodeEvaluation::getNumeroOrdre);
        racines.sort(parOrdre);
        for (PeriodeEvaluation racine : racines) {
            racine.getSousPeriodes().sort(parOrdre);
        }

        return racines;
    }

    public List<PeriodeEvaluation> findRacines(String idAnnee) {
        String sql = """
            SELECT * FROM PeriodeEvaluation
            WHERE idAnnee = ? AND idPeriodeParent IS NULL AND deleted_at IS NULL
            ORDER BY numeroOrdre ASC
            """;

        List<PeriodeEvaluation> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToPeriode(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findRacines : " + e.getMessage());
        }
        return resultat;
    }

    public List<PeriodeEvaluation> findSousPeriodes(String idPeriodeParent) {
        String sql = """
            SELECT * FROM PeriodeEvaluation
            WHERE idPeriodeParent = ? AND deleted_at IS NULL
            ORDER BY numeroOrdre ASC
            """;

        List<PeriodeEvaluation> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriodeParent);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToPeriode(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findSousPeriodes : " + e.getMessage());
        }
        return resultat;
    }

    /* ============================================================
       UPDATE
       ============================================================ */

    /**
     * Met à jour une période avec verrou de concurrence optimiste :
     * la clause WHERE inclut désormais "version = ?". Si la ligne a été
     * modifiée par quelqu'un d'autre entre le chargement côté client et
     * cet appel, 0 ligne est affectée et la méthode retourne false —
     * au Service de traiter ça comme un conflit plutôt qu'un échec muet
     * (conforme au principe "pas de last-write-wins silencieux").
     */
    public boolean update(PeriodeEvaluation periode) {
        String sql = """
            UPDATE PeriodeEvaluation SET
                idPeriodeParent = ?, codePeriode = ?, libelle = ?,
                typePeriode = ?, numeroOrdre = ?, poids = ?,
                dateDebut = ?, dateFin = ?,
                version = version + 1
            WHERE idPeriode = ? AND deleted_at IS NULL AND version = ?
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, periode.getIdPeriodeParent());
            stmt.setString(2, periode.getCodePeriode());
            stmt.setString(3, periode.getLibelle());
            stmt.setString(4, periode.getTypePeriode());
            stmt.setInt(5, periode.getNumeroOrdre());
            stmt.setBigDecimal(6, periode.getPoids());
            stmt.setDate(7, Date.valueOf(periode.getDateDebut()));
            stmt.setDate(8, Date.valueOf(periode.getDateFin()));
            stmt.setString(9, periode.getId());
            stmt.setLong(10, periode.getVersion());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur update PeriodeEvaluation : " + e.getMessage());
            return false;
        }
    }

    /**
     * Clôture une période (verrouillage des notes).
     * Ne vérifie PAS ici que les enfants sont eux-mêmes clôturés —
     * c'est une règle métier, voir PeriodeEvaluationDAO#peutCloturer()
     * et PeriodeEvaluationService, pas le rôle du DAO de l'appliquer.
     */
    public boolean cloturer(String idPeriode, long versionAttendue) {
        String sql = """
            UPDATE PeriodeEvaluation
            SET estCloturee = 1, version = version + 1
            WHERE idPeriode = ? AND deleted_at IS NULL AND version = ?
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriode);
            stmt.setLong(2, versionAttendue);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur cloturer : " + e.getMessage());
            return false;
        }
    }

    public boolean reouvrir(String idPeriode, long versionAttendue) {
        String sql = """
            UPDATE PeriodeEvaluation
            SET estCloturee = 0, version = version + 1
            WHERE idPeriode = ? AND deleted_at IS NULL AND version = ?
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriode);
            stmt.setLong(2, versionAttendue);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur reouvrir : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       DELETE (SOFT)
       ============================================================ */
    public boolean softDelete(String idPeriode) {
        String sql = """
            UPDATE PeriodeEvaluation
            SET deleted_at = NOW(3), version = version + 1
            WHERE idPeriode = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriode);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur softDelete PeriodeEvaluation : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       MÉTHODES MÉTIER
       ============================================================ */

    public boolean estCloturee(String idPeriode) {
        String sql = """
            SELECT estCloturee FROM PeriodeEvaluation
            WHERE idPeriode = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur estCloturee : " + e.getMessage());
        }
        return false;
    }

    /**
     * Nombre d'enfants actifs (non supprimés) d'une période.
     * Utilisée avant suppression (bloquer si > 0) et avant clôture
     * (voir peutCloturer, qui va plus loin).
     */
    public int compterEnfants(String idPeriodeParent) {
        String sql = """
            SELECT COUNT(*) FROM PeriodeEvaluation
            WHERE idPeriodeParent = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriodeParent);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterEnfants : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Une période sans enfants peut toujours être clôturée (c'est une
     * feuille — une séquence). Une période avec enfants (trimestre,
     * semestre, année) ne peut être clôturée que si tous ses enfants
     * actifs le sont déjà.
     */
    public boolean peutCloturer(String idPeriode) {
        String sql = """
            SELECT COUNT(*) FROM PeriodeEvaluation
            WHERE idPeriodeParent = ? AND deleted_at IS NULL AND estCloturee = 0
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idPeriode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur peutCloturer : " + e.getMessage());
        }
        return false;
    }

    public boolean codeExiste(String idAnnee, String code, String idExclu) {
        String sql = """
            SELECT COUNT(*) FROM PeriodeEvaluation
            WHERE idAnnee = ? AND codePeriode = ? AND deleted_at IS NULL
            """ + (idExclu != null ? " AND idPeriode != ?" : "");

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);
            stmt.setString(2, code);
            if (idExclu != null) stmt.setString(3, idExclu);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur codeExiste Periode : " + e.getMessage());
        }
        return false;
    }

    /* ============================================================
       MAPPING
       ============================================================ */
    private PeriodeEvaluation mapToPeriode(ResultSet rs) throws SQLException {
        PeriodeEvaluation p = new PeriodeEvaluation();
        p.setId(rs.getString("idPeriode"));
        p.setIdEcole(rs.getString("idEcole"));
        p.setIdAnnee(rs.getString("idAnnee"));
        p.setIdPeriodeParent(rs.getString("idPeriodeParent"));
        p.setCodePeriode(rs.getString("codePeriode"));
        p.setLibelle(rs.getString("libelle"));
        p.setTypePeriode(rs.getString("typePeriode"));
        p.setNumeroOrdre(rs.getInt("numeroOrdre"));
        p.setPoids(rs.getBigDecimal("poids"));

        Date debut = rs.getDate("dateDebut");
        if (debut != null) p.setDateDebut(debut.toLocalDate());

        Date fin = rs.getDate("dateFin");
        if (fin != null) p.setDateFin(fin.toLocalDate());

        p.setEstCloturee(rs.getBoolean("estCloturee"));
        p.setVersion(rs.getLong("version"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) p.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) p.setDeletedAt(deletedAt.toLocalDateTime());

        return p;
    }
}