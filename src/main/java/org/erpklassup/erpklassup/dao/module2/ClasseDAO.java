package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.Classe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour la table Classe.
 *
 * <p>Une classe est l'instance annuelle d'un niveau dans une école.
 * Elle combine Niveau + Année + Filière (optionnel) + Vacation.</p>
 *
 * <p>Contient une méthode <code>findAllAvecEffectif()</code> qui effectue
 * un JOIN avec Inscription pour calculer l'effectif actuel en une seule requête.</p>
 */
public class ClasseDAO {

    /* ============================================================
       CREATE
       ============================================================ */
    public boolean create(Classe classe) {
        String sql = """
            INSERT INTO Classe
                (idClasse, idEcole, idNiveau, idFiliere, idAnnee,
                 idEnseignantTitulaire, idSalle,
                 codeClasse, nomClasse, vacation, capacite)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, classe.getId());
            stmt.setString(2, classe.getIdEcole());
            stmt.setString(3, classe.getIdNiveau());
            stmt.setString(4, classe.getIdFiliere());
            stmt.setString(5, classe.getIdAnnee());
            stmt.setString(6, classe.getIdEnseignantTitulaire());
            stmt.setString(7, classe.getIdSalle());
            stmt.setString(8, classe.getCodeClasse());
            stmt.setString(9, classe.getNomClasse());
            stmt.setString(10, classe.getVacation().name());
            stmt.setInt(11, classe.getCapacite());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur create Classe : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       READ
       ============================================================ */
    public Optional<Classe> findById(String idClasse) {
        String sql = """
        SELECT c.*, n.nomNiveau, f.codeFiliere, a.libelle AS libelleAnnee,
               s.nomSalle AS nomSalle
        FROM Classe c
        LEFT JOIN Niveau n ON n.idNiveau = c.idNiveau
        LEFT JOIN Filiere f ON f.idFiliere = c.idFiliere
        LEFT JOIN AnneeScolaire a ON a.idAnnee = c.idAnnee
        LEFT JOIN Salle s ON s.idSalle = c.idSalle AND s.deleted_at IS NULL
        WHERE c.idClasse = ? AND c.deleted_at IS NULL
        """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idClasse);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToClasse(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById Classe : " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Liste les classes d'une année avec leur effectif calculé en une seule requête.
     * Utilise LEFT JOIN Inscription pour compter les élèves ACTIF.
     */

    public List<Classe> findByAnnee(String idAnnee) {
        String sql = """
        SELECT c.*, n.nomNiveau, f.codeFiliere, a.libelle AS libelleAnnee,
               s.nomSalle AS nomSalle,
               (SELECT COUNT(*) FROM Inscription i
                WHERE i.idClasse = c.idClasse
                  AND i.statut = 'ACTIF'
                  AND i.deleted_at IS NULL) AS effectifActuel
        FROM Classe c
        LEFT JOIN Niveau n ON n.idNiveau = c.idNiveau
        LEFT JOIN Filiere f ON f.idFiliere = c.idFiliere
        LEFT JOIN AnneeScolaire a ON a.idAnnee = c.idAnnee
        LEFT JOIN Salle s ON s.idSalle = c.idSalle AND s.deleted_at IS NULL
        WHERE c.idAnnee = ? AND c.deleted_at IS NULL
        ORDER BY n.ordre ASC, c.nomClasse ASC
        """;

        List<Classe> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnnee);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToClasse(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByAnnee Classe : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Liste filtrée par année + niveau (utilisé dans l'onglet Classes).
     */
    public List<Classe> rechercher(String idEcole, String idAnnee,
                                   String idNiveau, String terme) {
        StringBuilder sql = new StringBuilder("""
        SELECT c.*, n.nomNiveau, f.codeFiliere, a.libelle AS libelleAnnee,
               s.nomSalle AS nomSalle,
               (SELECT COUNT(*) FROM Inscription i
                WHERE i.idClasse = c.idClasse
                  AND i.statut = 'ACTIF'
                  AND i.deleted_at IS NULL) AS effectifActuel
        FROM Classe c
        LEFT JOIN Niveau n ON n.idNiveau = c.idNiveau
        LEFT JOIN Filiere f ON f.idFiliere = c.idFiliere
        LEFT JOIN AnneeScolaire a ON a.idAnnee = c.idAnnee
        LEFT JOIN Salle s ON s.idSalle = c.idSalle AND s.deleted_at IS NULL
        WHERE c.deleted_at IS NULL AND c.idEcole = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(idEcole);

        if (idAnnee != null && !idAnnee.isBlank()) {
            sql.append(" AND c.idAnnee = ? ");
            params.add(idAnnee);
        }
        if (idNiveau != null && !idNiveau.isBlank()) {
            sql.append(" AND c.idNiveau = ? ");
            params.add(idNiveau);
        }
        if (terme != null && !terme.isBlank()) {
            sql.append(" AND (c.nomClasse LIKE ? OR c.codeClasse LIKE ?) ");
            String motif = "%" + terme.trim() + "%";
            params.add(motif);
            params.add(motif);
        }
        sql.append(" ORDER BY n.ordre ASC, c.nomClasse ASC ");

        List<Classe> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            for (Object p : params) stmt.setObject(i++, p);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToClasse(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur rechercher Classe : " + e.getMessage());
        }
        return resultat;
    }

    /* ============================================================
       UPDATE
       ============================================================ */
    public boolean update(Classe classe) {
        String sql = """
            UPDATE Classe SET
                idNiveau = ?, idFiliere = ?, idAnnee = ?,
                idEnseignantTitulaire = ?, idSalle = ?,
                codeClasse = ?, nomClasse = ?, vacation = ?, capacite = ?,
                version = version + 1
            WHERE idClasse = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, classe.getIdNiveau());
            stmt.setString(2, classe.getIdFiliere());
            stmt.setString(3, classe.getIdAnnee());
            stmt.setString(4, classe.getIdEnseignantTitulaire());
            stmt.setString(5, classe.getIdSalle());
            stmt.setString(6, classe.getCodeClasse());
            stmt.setString(7, classe.getNomClasse());
            stmt.setString(8, classe.getVacation().name());
            stmt.setInt(9, classe.getCapacite());
            stmt.setString(10, classe.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur update Classe : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       DELETE (SOFT)
       ============================================================ */
    public boolean softDelete(String idClasse) {
        String sql = """
            UPDATE Classe SET deleted_at = NOW(3), version = version + 1
            WHERE idClasse = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idClasse);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur softDelete Classe : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       MÉTHODES MÉTIER
       ============================================================ */

    /**
     * Retourne l'effectif actuel (inscriptions ACTIF) d'une classe.
     */
    public int getEffectifActuel(String idClasse) {
        String sql = """
            SELECT COUNT(*) FROM Inscription
            WHERE idClasse = ? AND statut = 'ACTIF' AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idClasse);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getEffectifActuel : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Vérifie si une classe avec ce nom existe déjà dans l'année
     * (avec la même vacation).
     */
    public boolean nomClasseExiste(String idEcole, String idAnnee,
                                   String nomClasse, String vacation,
                                   String idExclu) {
        String sql = """
            SELECT COUNT(*) FROM Classe
            WHERE idEcole = ? AND idAnnee = ? AND nomClasse = ?
              AND vacation = ? AND deleted_at IS NULL
            """ + (idExclu != null ? " AND idClasse != ?" : "");

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, idAnnee);
            stmt.setString(3, nomClasse);
            stmt.setString(4, vacation);
            if (idExclu != null) stmt.setString(5, idExclu);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur nomClasseExiste : " + e.getMessage());
        }
        return false;
    }

    /**
     * Duplique toutes les classes d'une année vers une autre.
     * Utilisé au début de chaque nouvelle année scolaire.
     *
     * @return nombre de classes créées
     */
    public int dupliquerDepuisAnnee(String idEcole, String idAnneeSource,
                                    String idAnneeCible) {
        String sql = """
            INSERT INTO Classe
                (idClasse, idEcole, idNiveau, idFiliere, idAnnee,
                 idEnseignantTitulaire, idSalle,
                 codeClasse, nomClasse, vacation, capacite)
            SELECT
                UUID(), c.idEcole, c.idNiveau, c.idFiliere, ?,
                NULL, c.idSalle,
                c.codeClasse, c.nomClasse, c.vacation, c.capacite
            FROM Classe c
            WHERE c.idEcole = ? AND c.idAnnee = ? AND c.deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idAnneeCible);
            stmt.setString(2, idEcole);
            stmt.setString(3, idAnneeSource);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur dupliquerDepuisAnnee : " + e.getMessage());
            return 0;
        }
    }


    /**
     * Compte le nombre de classes actives qui utilisent une salle
     * (peu importe la vacation). Utilisé pour recalculer `occupee`.
     */
    public long compterClassesDansSalle(String idSalle) {
        if (idSalle == null || idSalle.isBlank()) return 0;

        String sql = "SELECT COUNT(*) FROM Classe WHERE idSalle = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterClassesDansSalle : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Vérifie si une autre classe utilise déjà cette salle pour cette vacation.
     * (Retourne false si la salle est libre pour cette vacation.)
     */
    public boolean conflitSalleVacation(String idSalle, String vacation, String idClasseExclue) {
        if (idSalle == null || idSalle.isBlank()) return false;
        if (vacation == null || vacation.isBlank()) return false;

        String sql = """
        SELECT COUNT(*) FROM Classe
        WHERE idSalle = ? AND vacation = ? AND deleted_at IS NULL
        """ + (idClasseExclue != null ? " AND idClasse != ?" : "");

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            stmt.setString(2, vacation);
            if (idClasseExclue != null) stmt.setString(3, idClasseExclue);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur conflitSalleVacation : " + e.getMessage());
        }
        return false;
    }


    /* ============================================================
       MAPPING
       ============================================================ */
    private Classe mapToClasse(ResultSet rs) throws SQLException {
        Classe c = new Classe();
        c.setId(rs.getString("idClasse"));
        c.setIdEcole(rs.getString("idEcole"));
        c.setIdNiveau(rs.getString("idNiveau"));
        c.setIdFiliere(rs.getString("idFiliere"));
        c.setIdAnnee(rs.getString("idAnnee"));
        c.setIdEnseignantTitulaire(rs.getString("idEnseignantTitulaire"));
        c.setIdSalle(rs.getString("idSalle"));
        c.setCodeClasse(rs.getString("codeClasse"));
        c.setNomClasse(rs.getString("nomClasse"));

        String vacation = rs.getString("vacation");
        if (vacation != null) c.setVacation(Classe.Vacation.valueOf(vacation));

        c.setCapacite(rs.getInt("capacite"));

        // Champs transitoires (issus des JOIN)
        try { c.setNomNiveau(rs.getString("nomNiveau")); } catch (SQLException ignored) {}
        try { c.setCodeFiliere(rs.getString("codeFiliere")); } catch (SQLException ignored) {}
        try { c.setLibelleAnnee(rs.getString("libelleAnnee")); } catch (SQLException ignored) {}
        try { c.setNomSalle(rs.getString("nomSalle")); } catch (SQLException ignored) {}  // ← AJOUTER
        try { c.setEffectifActuel(rs.getInt("effectifActuel")); } catch (SQLException ignored) {}

        // BaseEntity
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) c.setUpdatedAt(updatedAt.toLocalDateTime());

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) c.setDeletedAt(deletedAt.toLocalDateTime());

        return c;
    }
}