package org.erpklassup.erpklassup.dao.module2;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.models.module2.Salle;
import org.erpklassup.erpklassup.models.module2.Salle.TypeSalle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour la table Salle.
 *
 * <p>Gère les opérations CRUD + logique métier :
 * <ul>
 *   <li>Le code salle est unique par école</li>
 *   <li>Filtrage par type, bâtiment et statut d'occupation</li>
 *   <li>Blocage de suppression si la salle est utilisée par une classe</li>
 *   <li>Génération automatique de code salle</li>
 * </ul>
 * </p>
 */
public class SalleDAO {

    /* ============================================================
       CREATE
       ============================================================ */
    public boolean create(Salle salle) {
        String sql = """
            INSERT INTO Salle
                (idSalle, idEcole, codeSalle, nomSalle, batiment,
                 capaciteMax, typeSalle, occupee)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, salle.getId());
            stmt.setString(2, salle.getIdEcole());
            stmt.setString(3, salle.getCodeSalle());
            stmt.setString(4, salle.getNomSalle());
            stmt.setString(5, salle.getBatiment());
            stmt.setInt(6, salle.getCapaciteMax());
            stmt.setString(7, salle.getTypeSalle().name());
            stmt.setBoolean(8, salle.isOccupee());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur create Salle : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       READ
       ============================================================ */
    public Optional<Salle> findById(String idSalle) {
        String sql = "SELECT * FROM Salle WHERE idSalle = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToSalle(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById Salle : " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Liste toutes les salles d'une école, triées par bâtiment puis par code.
     */
    public List<Salle> findByEcole(String idEcole) {
        String sql = """
            SELECT * FROM Salle
            WHERE idEcole = ? AND deleted_at IS NULL
            ORDER BY batiment ASC, codeSalle ASC
            """;

        List<Salle> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToSalle(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByEcole Salle : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Recherche avec filtres combinés (terme + type + bâtiment + occupation).
     */
    public List<Salle> rechercher(String idEcole,
                                  String terme,
                                  TypeSalle typeFiltre,
                                  String batimentFiltre,
                                  Boolean occupeeFiltre) {

        StringBuilder sql = new StringBuilder("""
            SELECT * FROM Salle
            WHERE idEcole = ? AND deleted_at IS NULL
            """);

        List<Object> params = new ArrayList<>();
        params.add(idEcole);

        if (terme != null && !terme.isBlank()) {
            sql.append(" AND (codeSalle LIKE ? OR nomSalle LIKE ? OR batiment LIKE ?) ");
            String motif = "%" + terme.trim() + "%";
            params.add(motif);
            params.add(motif);
            params.add(motif);
        }

        if (typeFiltre != null) {
            sql.append(" AND typeSalle = ? ");
            params.add(typeFiltre.name());
        }

        if (batimentFiltre != null && !batimentFiltre.isBlank()) {
            sql.append(" AND batiment = ? ");
            params.add(batimentFiltre);
        }

        if (occupeeFiltre != null) {
            sql.append(" AND occupee = ? ");
            params.add(occupeeFiltre ? 1 : 0);
        }

        sql.append(" ORDER BY batiment ASC, codeSalle ASC");

        List<Salle> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            for (Object p : params) stmt.setObject(i++, p);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToSalle(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur rechercher Salle : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Version simplifiée : recherche par terme uniquement.
     */
    public List<Salle> rechercher(String idEcole, String terme) {
        return rechercher(idEcole, terme, null, null, null);
    }

    /**
     * Liste les salles d'un type donné.
     */
    public List<Salle> findByType(String idEcole, TypeSalle type) {
        String sql = """
            SELECT * FROM Salle
            WHERE idEcole = ? AND typeSalle = ? AND deleted_at IS NULL
            ORDER BY codeSalle ASC
            """;

        List<Salle> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, type.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToSalle(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByType Salle : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Liste uniquement les salles libres d'une école.
     */
    public List<Salle> listerSallesLibres(String idEcole) {
        String sql = """
            SELECT * FROM Salle
            WHERE idEcole = ? AND occupee = 0 AND deleted_at IS NULL
            ORDER BY batiment ASC, codeSalle ASC
            """;

        List<Salle> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) resultat.add(mapToSalle(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur listerSallesLibres : " + e.getMessage());
        }
        return resultat;
    }

    /**
     * Liste distincte des bâtiments d'une école (pour le filtre ComboBox).
     */
    public List<String> listerBatiments(String idEcole) {
        String sql = """
            SELECT DISTINCT batiment FROM Salle
            WHERE idEcole = ? AND deleted_at IS NULL AND batiment IS NOT NULL
            ORDER BY batiment ASC
            """;

        List<String> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String b = rs.getString("batiment");
                    if (b != null && !b.isBlank()) resultat.add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur listerBatiments : " + e.getMessage());
        }
        return resultat;
    }

    /* ============================================================
       UPDATE
       ============================================================ */
    public boolean update(Salle salle) {
        String sql = """
            UPDATE Salle SET
                codeSalle = ?, nomSalle = ?, batiment = ?,
                capaciteMax = ?, typeSalle = ?, occupee = ?,
                version = version + 1
            WHERE idSalle = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, salle.getCodeSalle());
            stmt.setString(2, salle.getNomSalle());
            stmt.setString(3, salle.getBatiment());
            stmt.setInt(4, salle.getCapaciteMax());
            stmt.setString(5, salle.getTypeSalle().name());
            stmt.setBoolean(6, salle.isOccupee());
            stmt.setString(7, salle.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur update Salle : " + e.getMessage());
            return false;
        }
    }

    /**
     * Bascule le statut d'occupation (occupée ↔ libre).
     */
    public boolean basculerOccupation(String idSalle) {
        String sql = """
            UPDATE Salle 
            SET occupee = NOT occupee, version = version + 1
            WHERE idSalle = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur basculerOccupation : " + e.getMessage());
            return false;
        }
    }

    /**
     * Définit explicitement le statut d'occupation.
     */
    public boolean setOccupation(String idSalle, boolean occupee) {
        String sql = """
            UPDATE Salle 
            SET occupee = ?, version = version + 1
            WHERE idSalle = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, occupee);
            stmt.setString(2, idSalle);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur setOccupation : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       DELETE (SOFT)
       ============================================================ */
    public boolean softDelete(String idSalle) {
        String sql = """
            UPDATE Salle SET deleted_at = NOW(3), version = version + 1
            WHERE idSalle = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur softDelete Salle : " + e.getMessage());
            return false;
        }
    }

    /* ============================================================
       MÉTHODES MÉTIER
       ============================================================ */

    /**
     * Vérifie si un code salle existe déjà pour cette école.
     */
    public boolean codeExiste(String idEcole, String codeSalle, String idExclu) {
        String sql = """
            SELECT COUNT(*) FROM Salle
            WHERE idEcole = ? AND codeSalle = ? AND deleted_at IS NULL
            """ + (idExclu != null ? " AND idSalle != ?" : "");

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            stmt.setString(2, codeSalle);
            if (idExclu != null) stmt.setString(3, idExclu);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur codeExiste Salle : " + e.getMessage());
        }
        return false;
    }

    /**
     * Compte les classes rattachées à cette salle.
     * Utilisé avant suppression pour éviter les FK orphelines.
     */
    public long compterClasses(String idSalle) {
        String sql = "SELECT COUNT(*) FROM Classe WHERE idSalle = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterClasses Salle : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Compte le nombre total de salles d'une école.
     */
    public long compterParEcole(String idEcole) {
        String sql = "SELECT COUNT(*) FROM Salle WHERE idEcole = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterParEcole Salle : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Compte les salles occupées d'une école.
     */
    public long compterSallesOccupees(String idEcole) {
        String sql = """
            SELECT COUNT(*) FROM Salle
            WHERE idEcole = ? AND occupee = 1 AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur compterSallesOccupees : " + e.getMessage());
        }
        return 0;
    }

    /**
     * Génère un code salle automatique (ex: SALLE-001, SALLE-002...).
     */
    public String genererCodeSalle(String idEcole) {
        String sql = """
            SELECT codeSalle FROM Salle
            WHERE idEcole = ? AND codeSalle LIKE 'SALLE-%' AND deleted_at IS NULL
            ORDER BY codeSalle DESC LIMIT 1
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dernier = rs.getString("codeSalle");
                    if (dernier != null && dernier.matches("SALLE-\\d+")) {
                        int num = Integer.parseInt(dernier.substring(6)) + 1;
                        return String.format("SALLE-%03d", num);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur genererCodeSalle : " + e.getMessage());
        }
        return "SALLE-001";
    }

    /* ============================================================
       MAPPING
       ============================================================ */
    private Salle mapToSalle(ResultSet rs) throws SQLException {
        Salle s = new Salle();
        s.setId(rs.getString("idSalle"));
        s.setIdEcole(rs.getString("idEcole"));
        s.setCodeSalle(rs.getString("codeSalle"));
        s.setNomSalle(rs.getString("nomSalle"));
        s.setBatiment(rs.getString("batiment"));
        s.setCapaciteMax(rs.getInt("capaciteMax"));

        String typeStr = rs.getString("typeSalle");
        s.setTypeSalle(TypeSalle.fromString(typeStr));

        s.setOccupee(rs.getBoolean("occupee"));

        // BaseEntity
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) s.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) s.setUpdatedAt(updatedAt.toLocalDateTime());

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) s.setDeletedAt(deletedAt.toLocalDateTime());

        return s;
    }

    /* ============================================================
   MÉTHODES DE DISPONIBILITÉ PAR VACATION
   ============================================================ */

    /**
     * Vérifie si une salle est disponible pour une vacation donnée.
     * Une salle est disponible si AUCUNE autre classe (hors idClasseExclue)
     * ne l'utilise avec cette vacation.
     *
     * @param idSalle       identifiant de la salle
     * @param vacation      vacation souhaitée (MATIN, SOIR, JOURNEE_CONTINUE...)
     * @param idClasseExclue id de la classe à exclure du test (pour modification), null sinon
     */
    public boolean estDisponiblePour(String idSalle, String vacation, String idClasseExclue) {
        if (idSalle == null || idSalle.isBlank()) return true;
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
                if (rs.next()) return rs.getLong(1) == 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur estDisponiblePour Salle : " + e.getMessage());
        }
        return false;
    }

    /**
     * Compte le nombre de classes actives qui utilisent cette salle
     * (peu importe la vacation).
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
     * Retourne la liste des vacations déjà utilisées pour une salle.
     * Ex: ["MATIN", "SOIR"]
     */
    public List<String> vacationsUtilisees(String idSalle) {
        String sql = """
        SELECT DISTINCT vacation FROM Classe
        WHERE idSalle = ? AND deleted_at IS NULL
        ORDER BY vacation
        """;

        List<String> resultat = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String v = rs.getString("vacation");
                    if (v != null && !v.isBlank()) resultat.add(v);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vacationsUtilisees : " + e.getMessage());
        }
        return resultat;
    }


    /**
     * Retourne la liste des noms des classes actives qui utilisent cette salle.
     * Utilisé pour bloquer la libération d'une salle occupée.
     */
    public List<String> listerNomsClassesDansSalle(String idSalle) {
        if (idSalle == null || idSalle.isBlank()) return List.of();

        String sql = """
        SELECT nomClasse FROM Classe
        WHERE idSalle = ? AND deleted_at IS NULL
        ORDER BY nomClasse
        """;

        List<String> noms = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idSalle);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nom = rs.getString("nomClasse");
                    if (nom != null && !nom.isBlank()) noms.add(nom);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur listerNomsClassesDansSalle : " + e.getMessage());
        }
        return noms;
    }
}