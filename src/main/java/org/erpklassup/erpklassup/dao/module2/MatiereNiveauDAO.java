package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.Filiere;
import org.erpklassup.erpklassup.models.module2.MatiereNiveau;
import org.erpklassup.erpklassup.models.module2.Niveau;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MatiereNiveauDAO {

    // Base SELECT commune : jointures en lecture seule vers Matiere/Niveau/Filiere
    // pour que le controller puisse peupler directement l'accordeon (nom, cycle,
    // systeme, code filiere, nom filiere) sans avoir besoin d'un DAO Niveau/Filiere separe.
    private static final String SELECT_BASE = """
            SELECT mn.idMatiereNiveau, mn.idEcole, mn.idMatiere, mn.idNiveau, mn.idFiliere,
                   mn.coefficient, mn.obligatoire, mn.ordreAffichage,
                   mn.version, mn.deleted_at, mn.created_at, mn.updated_at,
                   m.nomMatiere  AS nomMatiere,
                   n.nomNiveau   AS nomNiveau, n.cycle AS cycle, n.systeme AS systeme,
                   f.nomFiliere  AS nomFiliere,
                   f.codeFiliere AS codeFiliere
            FROM matiereNiveau mn
            INNER JOIN Matiere m ON m.idMatiere = mn.idMatiere
            INNER JOIN Niveau n  ON n.idNiveau = mn.idNiveau
            LEFT JOIN Filiere f  ON f.idFiliere = mn.idFiliere AND f.deleted_at IS NULL
            """;

    // ========== CREATE ==========
    public boolean create(MatiereNiveau mn) {
        if (mn.getIdMatiereNiveau() == null || mn.getIdMatiereNiveau().isBlank()) {
            mn.setIdMatiereNiveau(UUID.randomUUID().toString());
        }
        if (mn.getCoefficient() == null || mn.getCoefficient().compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Erreur create matiereNiveau : coefficient invalide (doit etre > 0)");
            return false;
        }

        String sql = "INSERT INTO matiereNiveau (idMatiereNiveau, idEcole, idMatiere, idNiveau, idFiliere, " +
                "coefficient, obligatoire, ordreAffichage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, mn.getIdMatiereNiveau());
            stmt.setString(2, mn.getIdEcole());
            stmt.setString(3, mn.getIdMatiere());
            stmt.setString(4, mn.getIdNiveau());

            if (mn.getIdFiliere() != null && !mn.getIdFiliere().isBlank()) {
                stmt.setString(5, mn.getIdFiliere());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }

            stmt.setBigDecimal(6, mn.getCoefficient());
            stmt.setBoolean(7, mn.isObligatoire());
            stmt.setInt(8, mn.getOrdreAffichage());

            return stmt.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Erreur create matiereNiveau : affectation deja existante pour cette matiere/niveau - "
                    + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur create matiereNiveau : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<MatiereNiveau> findById(String idMatiereNiveau) {
        String sql = SELECT_BASE + " WHERE mn.idMatiereNiveau = ? AND mn.deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idMatiereNiveau);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToMatiereNiveau(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById matiereNiveau : " + e.getMessage());
        }
        return Optional.empty();
    }

    // Toutes les affectations d'une matiere, tous niveaux confondus.
    public List<MatiereNiveau> findByMatiere(String idMatiere) {
        String sql = SELECT_BASE + " WHERE mn.idMatiere = ? AND mn.deleted_at IS NULL " +
                "ORDER BY n.ordre, mn.ordreAffichage";
        List<MatiereNiveau> resultats = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idMatiere);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultats.add(mapToMatiereNiveau(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByMatiere matiereNiveau : " + e.getMessage());
        }
        return resultats;
    }

    // Toutes les matieres affectees a un niveau donne.
    public List<MatiereNiveau> findByNiveau(String idNiveau) {
        String sql = SELECT_BASE + " WHERE mn.idNiveau = ? AND mn.deleted_at IS NULL " +
                "ORDER BY mn.ordreAffichage, m.nomMatiere";
        List<MatiereNiveau> resultats = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idNiveau);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultats.add(mapToMatiereNiveau(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByNiveau matiereNiveau : " + e.getMessage());
        }
        return resultats;
    }

    // Recherche par cle naturelle (idMatiere, idNiveau, idFiliere).
    // Utilise l'operateur <=> (null-safe) de MySQL : indispensable ici car
    // idFiliere peut etre NULL pour le primaire / premier cycle.
    public Optional<MatiereNiveau> findByMatiereNiveauFiliere(String idMatiere,
                                                              String idNiveau,
                                                              String idFiliere) {
        String sql = SELECT_BASE + " WHERE mn.idMatiere = ? AND mn.idNiveau = ? " +
                "AND mn.idFiliere <=> ? AND mn.deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idMatiere);
            stmt.setString(2, idNiveau);

            if (idFiliere != null && !idFiliere.isBlank()) {
                stmt.setString(3, idFiliere);
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToMatiereNiveau(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByMatiereNiveauFiliere : " + e.getMessage());
        }
        return Optional.empty();
    }

    // ========== UPDATE ==========
    public boolean update(MatiereNiveau mn) {
        if (mn.getCoefficient() == null || mn.getCoefficient().compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Erreur update matiereNiveau : coefficient invalide (doit etre > 0)");
            return false;
        }

        String sql = "UPDATE matiereNiveau SET coefficient = ?, obligatoire = ?, ordreAffichage = ?, " +
                "version = version + 1 WHERE idMatiereNiveau = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, mn.getCoefficient());
            stmt.setBoolean(2, mn.isObligatoire());
            stmt.setInt(3, mn.getOrdreAffichage());
            stmt.setString(4, mn.getIdMatiereNiveau());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update matiereNiveau : " + e.getMessage());
            return false;
        }
    }

    // ========== UPSERT ==========
    public boolean enregistrer(MatiereNiveau mn) {
        Optional<MatiereNiveau> existant = findByMatiereNiveauFiliere(
                mn.getIdMatiere(), mn.getIdNiveau(), mn.getIdFiliere());

        if (existant.isPresent()) {
            mn.setIdMatiereNiveau(existant.get().getIdMatiereNiveau());
            return update(mn);
        }
        return create(mn);
    }

    // Applique le meme coefficient / obligatoire a tous les niveaux actifs de l'ecole.
    public int dupliquerVersTousLesNiveaux(String idEcole, String idMatiere,
                                           BigDecimal coefficient, boolean obligatoire) {
        int compteur = 0;
        String sqlNiveaux = "SELECT idNiveau FROM Niveau WHERE idEcole = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sqlNiveaux)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MatiereNiveau mn = new MatiereNiveau(
                            idMatiere, rs.getString("idNiveau"), coefficient);
                    mn.setIdEcole(idEcole);
                    mn.setObligatoire(obligatoire);
                    if (enregistrer(mn)) compteur++;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur dupliquerVersTousLesNiveaux matiereNiveau : " + e.getMessage());
        }
        return compteur;
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idMatiereNiveau) {
        String sql = "UPDATE matiereNiveau SET deleted_at = NOW(3), version = version + 1 " +
                "WHERE idMatiereNiveau = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idMatiereNiveau);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete matiereNiveau : " + e.getMessage());
            return false;
        }
    }

    // Retrait par cle naturelle.
    public boolean retirer(String idMatiere, String idNiveau, String idFiliere) {
        return findByMatiereNiveauFiliere(idMatiere, idNiveau, idFiliere)
                .map(mn -> softDelete(mn.getIdMatiereNiveau()))
                .orElse(true);
    }

    // ========== HELPER DE MAPPING ==========
    private MatiereNiveau mapToMatiereNiveau(ResultSet rs) throws SQLException {
        MatiereNiveau mn = new MatiereNiveau();
        mn.setIdMatiereNiveau(rs.getString("idMatiereNiveau"));
        mn.setIdEcole(rs.getString("idEcole"));
        mn.setIdMatiere(rs.getString("idMatiere"));
        mn.setIdNiveau(rs.getString("idNiveau"));
        mn.setIdFiliere(rs.getString("idFiliere"));
        mn.setCoefficient(rs.getBigDecimal("coefficient"));
        mn.setObligatoire(rs.getBoolean("obligatoire"));
        mn.setOrdreAffichage(rs.getInt("ordreAffichage"));
        mn.setNomMatiere(rs.getString("nomMatiere"));
        mn.setNomNiveau(rs.getString("nomNiveau"));
        mn.setCycle(rs.getString("cycle"));
        mn.setSysteme(rs.getString("systeme"));
        mn.setNomFiliere(rs.getString("nomFiliere"));
        mn.setCodeFiliere(rs.getString("codeFiliere"));   // ✅ NOUVEAU
        mn.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) mn.setDeletedAt(deletedAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) mn.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) mn.setUpdatedAt(updatedAt.toLocalDateTime());

        return mn;
    }

    /**
     * Renvoie toutes les combinaisons Niveau × Filière possibles dans une école.
     * Chaque MatiereNiveau retourné a :
     *   - idNiveau et idFiliere remplis (idFiliere = null si le niveau n'en a pas)
     *   - nomNiveau, cycle, systeme, nomFiliere, codeFiliere remplis
     *   - coefficient = 1, obligatoire = false, idMatiereNiveau = null
     */
    public List<MatiereNiveau> toutesLesCombinaisonsPossibles(String idEcole) {
        String sqlNiveaux = """
            SELECT idNiveau, nomNiveau, cycle, systeme, ordre
            FROM Niveau
            WHERE idEcole = ? AND deleted_at IS NULL
            ORDER BY ordre, nomNiveau
            """;

        String sqlFilieres = """
            SELECT idFiliere, codeFiliere, nomFiliere
            FROM Filiere
            WHERE idEcole = ? AND deleted_at IS NULL
            ORDER BY codeFiliere
            """;

        List<MatiereNiveau> combinaisons = new ArrayList<>();

        try (Connection conn = Database.getConnexion()) {

            List<Niveau> niveaux = new ArrayList<>();
            try (PreparedStatement st = conn.prepareStatement(sqlNiveaux)) {
                st.setString(1, idEcole);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Niveau n = new Niveau();
                        n.setIdNiveau(rs.getString("idNiveau"));
                        n.setNomNiveau(rs.getString("nomNiveau"));
                        n.setCycle(rs.getString("cycle"));
                        n.setSysteme(rs.getString("systeme"));
                        n.setOrdre(rs.getInt("ordre"));
                        niveaux.add(n);
                    }
                }
            }

            List<Filiere> filieres = new ArrayList<>();
            try (PreparedStatement st = conn.prepareStatement(sqlFilieres)) {
                st.setString(1, idEcole);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Filiere f = new Filiere();
                        f.setIdFiliere(rs.getString("idFiliere"));
                        f.setCodeFiliere(rs.getString("codeFiliere"));
                        f.setNomFiliere(rs.getString("nomFiliere"));
                        filieres.add(f);
                    }
                }
            }

            for (Niveau n : niveaux) {
                if (necessiteFiliere(n)) {
                    for (Filiere f : filieres) {
                        MatiereNiveau mn = new MatiereNiveau();
                        mn.setIdEcole(idEcole);
                        mn.setIdNiveau(n.getIdNiveau());
                        mn.setIdFiliere(f.getIdFiliere());
                        mn.setNomNiveau(n.getNomNiveau());
                        mn.setCycle(n.getCycle());
                        mn.setSysteme(n.getSysteme());
                        mn.setNomFiliere(f.getNomFiliere());
                        mn.setCodeFiliere(f.getCodeFiliere());   // ✅ NOUVEAU
                        mn.setCoefficient(BigDecimal.ONE);
                        mn.setObligatoire(false);
                        combinaisons.add(mn);
                    }
                } else {
                    MatiereNiveau mn = new MatiereNiveau();
                    mn.setIdEcole(idEcole);
                    mn.setIdNiveau(n.getIdNiveau());
                    mn.setIdFiliere(null);
                    mn.setNomNiveau(n.getNomNiveau());
                    mn.setCycle(n.getCycle());
                    mn.setSysteme(n.getSysteme());
                    mn.setNomFiliere(null);
                    mn.setCodeFiliere(null);                     // ✅ NOUVEAU
                    mn.setCoefficient(BigDecimal.ONE);
                    mn.setObligatoire(false);
                    combinaisons.add(mn);
                }
            }

        } catch (SQLException e) {
            System.err.println("Erreur toutesLesCombinaisonsPossibles : " + e.getMessage());
        }

        return combinaisons;
    }

    /**
     * Détermine si un niveau doit générer une ligne par filière.
     * Règle : seuls les niveaux du SECOND_CYCLE (2nde, 1ère, Tle) ont des filières.
     */
    private boolean necessiteFiliere(Niveau n) {
        if (n == null || n.getCycle() == null) return false;
        return "SECOND_CYCLE".equalsIgnoreCase(n.getCycle().trim());
    }
}
//package org.erpklassup.erpklassup.dao.module2;
//
//import org.erpklassup.erpklassup.Database;
//import org.erpklassup.erpklassup.models.module2.Filiere;
//import org.erpklassup.erpklassup.models.module2.MatiereNiveau;
//import org.erpklassup.erpklassup.models.module2.Niveau;
//
//import java.math.BigDecimal;
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//public class MatiereNiveauDAO {
//
//    // Base SELECT commune : jointures en lecture seule vers Matiere/Niveau/Filiere
//    // pour que le controller puisse peupler directement l'accordeon (nom, cycle,
//    // systeme, filiere) sans avoir besoin d'un DAO Niveau/Filiere separe.
//    private static final String SELECT_BASE = """
//            SELECT mn.idMatiereNiveau, mn.idEcole, mn.idMatiere, mn.idNiveau, mn.idFiliere,
//                   mn.coefficient, mn.obligatoire, mn.ordreAffichage,
//                   mn.version, mn.deleted_at, mn.created_at, mn.updated_at,
//                   m.nomMatiere AS nomMatiere,
//                   n.nomNiveau  AS nomNiveau, n.cycle AS cycle, n.systeme AS systeme,
//                   f.nomFiliere AS nomFiliere
//            FROM matiereNiveau mn
//            INNER JOIN Matiere m ON m.idMatiere = mn.idMatiere
//            INNER JOIN Niveau n  ON n.idNiveau = mn.idNiveau
//            LEFT JOIN Filiere f  ON f.idFiliere = mn.idFiliere AND f.deleted_at IS NULL
//            """;
//
//    // ========== CREATE ==========
//    public boolean create(MatiereNiveau mn) {
//        if (mn.getIdMatiereNiveau() == null || mn.getIdMatiereNiveau().isBlank()) {
//            mn.setIdMatiereNiveau(UUID.randomUUID().toString());
//        }
//        if (mn.getCoefficient() == null || mn.getCoefficient().compareTo(BigDecimal.ZERO) <= 0) {
//            System.err.println("Erreur create matiereNiveau : coefficient invalide (doit etre > 0)");
//            return false;
//        }
//
//        String sql = "INSERT INTO matiereNiveau (idMatiereNiveau, idEcole, idMatiere, idNiveau, idFiliere, " +
//                "coefficient, obligatoire, ordreAffichage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, mn.getIdMatiereNiveau());
//            stmt.setString(2, mn.getIdEcole());
//            stmt.setString(3, mn.getIdMatiere());
//            stmt.setString(4, mn.getIdNiveau());
//            stmt.setString(5, mn.getIdFiliere());
//            stmt.setBigDecimal(6, mn.getCoefficient());
//            stmt.setBoolean(7, mn.isObligatoire());
//            stmt.setInt(8, mn.getOrdreAffichage());
//
//            return stmt.executeUpdate() > 0;
//        } catch (SQLIntegrityConstraintViolationException e) {
//            System.err.println("Erreur create matiereNiveau : affectation deja existante pour cette matiere/niveau - " + e.getMessage());
//            return false;
//        } catch (SQLException e) {
//            System.err.println("Erreur create matiereNiveau : " + e.getMessage());
//            return false;
//        }
//    }
//
//    // ========== READ ==========
//    public Optional<MatiereNiveau> findById(String idMatiereNiveau) {
//        String sql = SELECT_BASE + " WHERE mn.idMatiereNiveau = ? AND mn.deleted_at IS NULL";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, idMatiereNiveau);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) return Optional.of(mapToMatiereNiveau(rs));
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur findById matiereNiveau : " + e.getMessage());
//        }
//        return Optional.empty();
//    }
//
//    // Toutes les affectations d'une matiere, tous niveaux confondus : c'est ce
//    // qui alimente l'accordeon (le controller reparti ensuite chaque ligne dans
//    // le bon TitledPane selon le champ "cycle").
//    public List<MatiereNiveau> findByMatiere(String idMatiere) {
//        String sql = SELECT_BASE + " WHERE mn.idMatiere = ? AND mn.deleted_at IS NULL " +
//                "ORDER BY n.ordre, mn.ordreAffichage";
//        List<MatiereNiveau> resultats = new ArrayList<>();
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, idMatiere);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) resultats.add(mapToMatiereNiveau(rs));
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur findByMatiere matiereNiveau : " + e.getMessage());
//        }
//        return resultats;
//    }
//
//    // Toutes les matieres affectees a un niveau donne (utile plus tard pour la
//    // generation du bulletin : liste des matieres + coefficients d'une classe).
//    public List<MatiereNiveau> findByNiveau(String idNiveau) {
//        String sql = SELECT_BASE + " WHERE mn.idNiveau = ? AND mn.deleted_at IS NULL " +
//                "ORDER BY mn.ordreAffichage, m.nomMatiere";
//        List<MatiereNiveau> resultats = new ArrayList<>();
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, idNiveau);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) resultats.add(mapToMatiereNiveau(rs));
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur findByNiveau matiereNiveau : " + e.getMessage());
//        }
//        return resultats;
//    }
//
//    // Recherche par cle naturelle (idMatiere, idNiveau, idFiliere) - utilise par
//    // enregistrer() pour savoir s'il faut inserer ou mettre a jour. <=> est
//    // l'operateur d'egalite null-safe de MySQL : indispensable ici puisque
//    // idFiliere est NULL pour le primaire/maternelle (NULL = NULL vaut FALSE
//    // en SQL standard, <=> le traite correctement comme egal).
//    public Optional<MatiereNiveau> findByMatiereNiveauFiliere(String idMatiere, String idNiveau, String idFiliere) {
//        String sql = SELECT_BASE + " WHERE mn.idMatiere = ? AND mn.idNiveau = ? " +
//                "AND mn.idFiliere <=> ? AND mn.deleted_at IS NULL";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setString(1, idMatiere);
//            stmt.setString(2, idNiveau);
//            stmt.setString(3, idFiliere);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) return Optional.of(mapToMatiereNiveau(rs));
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur findByMatiereNiveauFiliere : " + e.getMessage());
//        }
//        return Optional.empty();
//    }
//
//    // ========== UPDATE ==========
//    public boolean update(MatiereNiveau mn) {
//        if (mn.getCoefficient() == null || mn.getCoefficient().compareTo(BigDecimal.ZERO) <= 0) {
//            System.err.println("Erreur update matiereNiveau : coefficient invalide (doit etre > 0)");
//            return false;
//        }
//
//        String sql = "UPDATE matiereNiveau SET coefficient = ?, obligatoire = ?, ordreAffichage = ?, " +
//                "version = version + 1 WHERE idMatiereNiveau = ?";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setBigDecimal(1, mn.getCoefficient());
//            stmt.setBoolean(2, mn.isObligatoire());
//            stmt.setInt(3, mn.getOrdreAffichage());
//            stmt.setString(4, mn.getIdMatiereNiveau());
//
//            return stmt.executeUpdate() > 0;
//        } catch (SQLException e) {
//            System.err.println("Erreur update matiereNiveau : " + e.getMessage());
//            return false;
//        }
//    }
//
//    // ========== UPSERT (celle que le bouton "Enregistrer" doit appeler) ==========
//    // Le controller n'a pas besoin de savoir si la ligne existe deja : il construit
//    // un MatiereNiveau avec idMatiere/idNiveau/idFiliere/coefficient/obligatoire et
//    // appelle enregistrer(). Insere si absent, met a jour sinon.
//    public boolean enregistrer(MatiereNiveau mn) {
//        Optional<MatiereNiveau> existant = findByMatiereNiveauFiliere(
//                mn.getIdMatiere(), mn.getIdNiveau(), mn.getIdFiliere());
//
//        if (existant.isPresent()) {
//            mn.setIdMatiereNiveau(existant.get().getIdMatiereNiveau());
//            return update(mn);
//        }
//        return create(mn);
//    }
//
//    // Applique le meme coefficient/obligatoire a tous les niveaux actifs de
//    // l'ecole - utilise par la case a cocher "Appliquer a tous les niveaux".
//    // idFiliere reste NULL ici : un coefficient "global" ne s'applique pas a une
//    // filiere en particulier, il faut ensuite l'affiner filiere par filiere si besoin.
//    public int dupliquerVersTousLesNiveaux(String idEcole, String idMatiere,
//                                           BigDecimal coefficient, boolean obligatoire) {
//        int compteur = 0;
//        String sqlNiveaux = "SELECT idNiveau FROM Niveau WHERE idEcole = ? AND deleted_at IS NULL";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sqlNiveaux)) {
//
//            stmt.setString(1, idEcole);
//
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    MatiereNiveau mn = new MatiereNiveau(idMatiere, rs.getString("idNiveau"), coefficient);
//                    mn.setIdEcole(idEcole);
//                    mn.setObligatoire(obligatoire);
//                    if (enregistrer(mn)) compteur++;
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur dupliquerVersTousLesNiveaux matiereNiveau : " + e.getMessage());
//        }
//        return compteur;
//    }
//
//    // ========== DELETE (SOFT) ==========
//    public boolean softDelete(String idMatiereNiveau) {
//        String sql = "UPDATE matiereNiveau SET deleted_at = NOW(3), version = version + 1 " +
//                "WHERE idMatiereNiveau = ?";
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setString(1, idMatiereNiveau);
//            return stmt.executeUpdate() > 0;
//        } catch (SQLException e) {
//            System.err.println("Erreur softDelete matiereNiveau : " + e.getMessage());
//            return false;
//        }
//    }
//
//    // Retrait par cle naturelle - utile quand l'utilisateur decoche une matiere
//    // pour un niveau dans l'accordeon (il n'a pas forcement l'idMatiereNiveau sous la main).
//    public boolean retirer(String idMatiere, String idNiveau, String idFiliere) {
//        return findByMatiereNiveauFiliere(idMatiere, idNiveau, idFiliere)
//                .map(mn -> softDelete(mn.getIdMatiereNiveau()))
//                .orElse(true); // rien a retirer = deja dans l'etat souhaite
//    }
//
//    // ========== HELPER DE MAPPING ==========
//    private MatiereNiveau mapToMatiereNiveau(ResultSet rs) throws SQLException {
//        MatiereNiveau mn = new MatiereNiveau();
//        mn.setIdMatiereNiveau(rs.getString("idMatiereNiveau"));
//        mn.setIdEcole(rs.getString("idEcole"));
//        mn.setIdMatiere(rs.getString("idMatiere"));
//        mn.setIdNiveau(rs.getString("idNiveau"));
//        mn.setIdFiliere(rs.getString("idFiliere"));
//        mn.setCoefficient(rs.getBigDecimal("coefficient"));
//        mn.setObligatoire(rs.getBoolean("obligatoire"));
//        mn.setOrdreAffichage(rs.getInt("ordreAffichage"));
//        mn.setNomMatiere(rs.getString("nomMatiere"));
//        mn.setNomNiveau(rs.getString("nomNiveau"));
//        mn.setCycle(rs.getString("cycle"));
//        mn.setSysteme(rs.getString("systeme"));
//        mn.setNomFiliere(rs.getString("nomFiliere"));
//        mn.setVersion(rs.getLong("version"));
//
//        Timestamp deletedAt = rs.getTimestamp("deleted_at");
//        if (deletedAt != null) mn.setDeletedAt(deletedAt.toLocalDateTime());
//        Timestamp createdAt = rs.getTimestamp("created_at");
//        if (createdAt != null) mn.setCreatedAt(createdAt.toLocalDateTime());
//        Timestamp updatedAt = rs.getTimestamp("updated_at");
//        if (updatedAt != null) mn.setUpdatedAt(updatedAt.toLocalDateTime());
//
//        return mn;
//    }
//
//    /**
//     * Renvoie toutes les combinaisons Niveau × Filière possibles dans une école.
//     * Chaque MatiereNiveau retourné a :
//     *   - idNiveau et idFiliere remplis
//     *   - nomNiveau, cycle, nomFiliere remplis
//     *   - coefficient = 1, obligatoire = false, idMatiereNiveau = null
//     */
//    public List<MatiereNiveau> toutesLesCombinaisonsPossibles(String idEcole) {
//        // 1) Tous les niveaux
//        String sqlNiveaux = """
//        SELECT idNiveau, nomNiveau, cycle, systeme, ordre
//        FROM Niveau
//        WHERE idEcole = ? AND deleted_at IS NULL
//        ORDER BY ordre, nomNiveau
//        """;
//
//        // 2) Toutes les filières
//        String sqlFilieres = """
//        SELECT idFiliere, codeFiliere, nomFiliere
//        FROM Filiere
//        WHERE idEcole = ? AND deleted_at IS NULL
//        ORDER BY codeFiliere
//        """;
//
//        List<MatiereNiveau> combinaisons = new ArrayList<>();
//
//        try (Connection conn = Database.getConnexion()) {
//
//            List<Niveau> niveaux = new ArrayList<>();
//            try (PreparedStatement st = conn.prepareStatement(sqlNiveaux)) {
//                st.setString(1, idEcole);
//                try (ResultSet rs = st.executeQuery()) {
//                    while (rs.next()) {
//                        Niveau n = new Niveau();
//                        n.setIdNiveau(rs.getString("idNiveau"));
//                        n.setNomNiveau(rs.getString("nomNiveau"));
//                        n.setCycle(rs.getString("cycle"));
//                        n.setSysteme(rs.getString("systeme"));
//                        n.setOrdre(rs.getInt("ordre"));
//                        niveaux.add(n);
//                    }
//                }
//            }
//
//            List<Filiere> filieres = new ArrayList<>();
//            try (PreparedStatement st = conn.prepareStatement(sqlFilieres)) {
//                st.setString(1, idEcole);
//                try (ResultSet rs = st.executeQuery()) {
//                    while (rs.next()) {
//                        Filiere f = new Filiere();
//                        f.setIdFiliere(rs.getString("idFiliere"));
//                        f.setCodeFiliere(rs.getString("codeFiliere"));
//                        f.setNomFiliere(rs.getString("nomFiliere"));
//                        filieres.add(f);
//                    }
//                }
//            }
//
//            // 3) Croisement conditionnel
//            for (Niveau n : niveaux) {
//                if (necessiteFiliere(n)) {
//                    for (Filiere f : filieres) {
//                        MatiereNiveau mn = new MatiereNiveau();
//                        mn.setIdEcole(idEcole);
//                        mn.setIdNiveau(n.getIdNiveau());
//                        mn.setIdFiliere(f.getIdFiliere());
//                        mn.setNomNiveau(n.getNomNiveau());
//                        mn.setCycle(n.getCycle());
//                        mn.setSysteme(n.getSysteme());
//                        mn.setNomFiliere(f.getNomFiliere());
//                        mn.setCoefficient(BigDecimal.ONE);
//                        mn.setObligatoire(false);
//                        combinaisons.add(mn);
//                    }
//                } else {
//                    // Une SEULE ligne sans filière
//                    MatiereNiveau mn = new MatiereNiveau();
//                    mn.setIdEcole(idEcole);
//                    mn.setIdNiveau(n.getIdNiveau());
//                    mn.setIdFiliere(null);
//                    mn.setNomNiveau(n.getNomNiveau());
//                    mn.setCycle(n.getCycle());
//                    mn.setSysteme(n.getSysteme());
//                    mn.setNomFiliere(null);
//                    mn.setCoefficient(BigDecimal.ONE);
//                    mn.setObligatoire(false);
//                    combinaisons.add(mn);
//                }
//            }
//
//        } catch (SQLException e) {
//            System.err.println("Erreur toutesLesCombinaisonsPossibles : " + e.getMessage());
//        }
//
//        return combinaisons;
//    }
//
//    /**
//     * Détermine si un niveau doit générer une ligne par filière.
//     * Règle : seuls les niveaux du second cycle terminal (2nde, 1ère, Tle)
//     * ont des filières. Le premier cycle (6ème → 3ème) et le primaire n'en ont pas.
//     */
//    private boolean necessiteFiliere(Niveau n) {
//        if (n == null || n.getCycle() == null) return false;
//        return "SECOND_CYCLE".equalsIgnoreCase(n.getCycle().trim());
//    }
//}