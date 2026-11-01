package org.erpklassup.erpklassup.dao.GRH;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.DepartementLigne;
import org.erpklassup.erpklassup.dto.FiltreDepartement;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.enums.SousSysteme;
import org.erpklassup.erpklassup.enums.StatutDepartement;
import org.erpklassup.erpklassup.enums.TypeDepartement;
import org.erpklassup.erpklassup.models.GRH.Departement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartementDAO implements IDepartementDAO {

    // ========== CREATE ==========
    @Override
    public boolean create(Departement d) {
        String sql = """
            INSERT INTO Departement (
                idDepartement, idEcole, code, libelle, description, type, sousSysteme,
                estConseilEnseignement, idDepartementParent, niveau, chemin, ordre_affichage,
                idResponsable, idResponsableAdjoint, statut, date_debut, date_fin,
                motif_desactivation, code_centre_cout, codeMineduc, budget_annuel,
                effectif_max, email, telephone, localisation
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, d.getIdDepartement());
            stmt.setString(2, d.getIdEcole());
            stmt.setString(3, d.getCode());
            stmt.setString(4, d.getLibelle());
            setNullableString(stmt, 5, d.getDescription());
            stmt.setString(6, d.getType() != null ? d.getType().name() : TypeDepartement.ADMINISTRATIF.name());
            stmt.setString(7, d.getSousSysteme() != null ? d.getSousSysteme().name() : SousSysteme.BILINGUE.name());
            stmt.setBoolean(8, Boolean.TRUE.equals(d.getEstConseilEnseignement()));
            setNullableString(stmt, 9, d.getIdDepartementParent());
            setNullableInteger(stmt, 10, d.getNiveau());
            setNullableString(stmt, 11, d.getChemin());
            setNullableInteger(stmt, 12, d.getOrdreAffichage());
            setNullableString(stmt, 13, d.getIdResponsable());
            setNullableString(stmt, 14, d.getIdResponsableAdjoint());
            stmt.setString(15, d.getStatut() != null ? d.getStatut().name() : StatutDepartement.ACTIF.name());
            setNullableDate(stmt, 16, d.getDateDebut());
            setNullableDate(stmt, 17, d.getDateFin());
            setNullableString(stmt, 18, d.getMotifDesactivation());
            setNullableString(stmt, 19, d.getCodeCentreCout());
            setNullableString(stmt, 20, d.getCodeMineduc());
            setNullableBigDecimal(stmt, 21, d.getBudgetAnnuel());
            setNullableInteger(stmt, 22, d.getEffectifMax());
            setNullableString(stmt, 23, d.getEmail());
            setNullableString(stmt, 24, d.getTelephone());
            setNullableString(stmt, 25, d.getLocalisation());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur create Departement : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    @Override
    public Optional<Departement> findById(String idDepartement) {
        String sql = "SELECT * FROM Departement WHERE idDepartement = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idDepartement);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToDepartement(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById Departement : " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Departement> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Departement WHERE idEcole = ? AND deleted_at IS NULL ORDER BY libelle ASC";
        List<Departement> liste = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) liste.add(mapToDepartement(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByEcole Departement : " + e.getMessage());
        }
        return liste;
    }

    @Override
    public List<Departement> findRacines(String idEcole) {
        return findSousDepartements(idEcole, null);
    }

    @Override
    public List<Departement> findSousDepartements(String idEcole, String idParent) {
        String sql = "SELECT * FROM Departement WHERE idEcole = ? AND deleted_at IS NULL AND " +
                (idParent == null ? "idDepartementParent IS NULL" : "idDepartementParent = ?") +
                " ORDER BY ordre_affichage ASC, libelle ASC";
        List<Departement> liste = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);
            if (idParent != null) stmt.setString(2, idParent);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) liste.add(mapToDepartement(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findSousDepartements : " + e.getMessage());
        }
        return liste;
    }

    // ========== UPDATE ==========
    @Override
    public boolean update(Departement d) {
        String sql = """
            UPDATE Departement SET
                code = ?, libelle = ?, description = ?, type = ?, sousSysteme = ?,
                estConseilEnseignement = ?, idDepartementParent = ?, niveau = ?, chemin = ?,
                ordre_affichage = ?, idResponsable = ?, idResponsableAdjoint = ?, statut = ?,
                date_debut = ?, date_fin = ?, motif_desactivation = ?, code_centre_cout = ?,
                codeMineduc = ?, budget_annuel = ?, effectif_max = ?, email = ?, telephone = ?,
                localisation = ?, version = version + 1
            WHERE idDepartement = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, d.getCode());
            stmt.setString(2, d.getLibelle());
            setNullableString(stmt, 3, d.getDescription());
            stmt.setString(4, d.getType() != null ? d.getType().name() : TypeDepartement.ADMINISTRATIF.name());
            stmt.setString(5, d.getSousSysteme() != null ? d.getSousSysteme().name() : SousSysteme.BILINGUE.name());
            stmt.setBoolean(6, Boolean.TRUE.equals(d.getEstConseilEnseignement()));
            setNullableString(stmt, 7, d.getIdDepartementParent());
            setNullableInteger(stmt, 8, d.getNiveau());
            setNullableString(stmt, 9, d.getChemin());
            setNullableInteger(stmt, 10, d.getOrdreAffichage());
            setNullableString(stmt, 11, d.getIdResponsable());
            setNullableString(stmt, 12, d.getIdResponsableAdjoint());
            stmt.setString(13, d.getStatut() != null ? d.getStatut().name() : StatutDepartement.ACTIF.name());
            setNullableDate(stmt, 14, d.getDateDebut());
            setNullableDate(stmt, 15, d.getDateFin());
            setNullableString(stmt, 16, d.getMotifDesactivation());
            setNullableString(stmt, 17, d.getCodeCentreCout());
            setNullableString(stmt, 18, d.getCodeMineduc());
            setNullableBigDecimal(stmt, 19, d.getBudgetAnnuel());
            setNullableInteger(stmt, 20, d.getEffectifMax());
            setNullableString(stmt, 21, d.getEmail());
            setNullableString(stmt, 22, d.getTelephone());
            setNullableString(stmt, 23, d.getLocalisation());
            stmt.setString(24, d.getIdDepartement());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur update Departement : " + e.getMessage());
            return false;
        }
    }

    // ========== SOFT DELETE ==========
    @Override
    public boolean softDelete(String idDepartement) {
        String sql = "UPDATE Departement SET deleted_at = NOW(3), version = version + 1 WHERE idDepartement = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idDepartement);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur softDelete Departement : " + e.getMessage());
            return false;
        }
    }

    // ========== RECHERCHE PAGINÉE (Pour TableView) ==========
    @Override
    public ResultatPagine<DepartementLigne> rechercherPagine(FiltreDepartement filtre) {
        StringBuilder clause = new StringBuilder("""
             FROM Departement d
             LEFT JOIN Departement dp ON dp.idDepartement = d.idDepartementParent
             LEFT JOIN Personnel resp ON resp.idPersonnel = d.idResponsable
             LEFT JOIN Personnel adj ON adj.idPersonnel = d.idResponsableAdjoint
             WHERE d.deleted_at IS NULL
             AND d.idEcole = ?
            """);

        List<Object> parametres = new ArrayList<>();
        parametres.add(filtre.idEcole());

        if (filtre.terme() != null && !filtre.terme().isBlank()) {
            clause.append(" AND (d.libelle LIKE ? OR d.code LIKE ? OR d.codeMineduc LIKE ?) ");
            String motif = "%" + filtre.terme().trim() + "%";
            parametres.add(motif);
            parametres.add(motif);
            parametres.add(motif);
        }
        if (filtre.type() != null) {
            clause.append(" AND d.type = ? ");
            parametres.add(filtre.type().name());
        }
        if (filtre.idResponsable() != null && !filtre.idResponsable().isBlank()) {
            clause.append(" AND (d.idResponsable = ? OR d.idResponsableAdjoint = ?) ");
            parametres.add(filtre.idResponsable());
            parametres.add(filtre.idResponsable());
        }
        if (filtre.statut() != null) {
            clause.append(" AND d.statut = ? ");
            parametres.add(filtre.statut().name());
        }

        long total = compterQuery("SELECT COUNT(*) " + clause, parametres);

        String sqlPage = """
            SELECT d.idDepartement, d.code, d.libelle, d.type, d.sousSysteme,
                   d.estConseilEnseignement, d.statut, d.effectif_max, d.budget_annuel, d.codeMineduc,
                   dp.libelle AS parentLibelle,
                   CONCAT(IFNULL(resp.nom, ''), ' ', IFNULL(resp.prenom, '')) AS respNomComplet,
                   CONCAT(IFNULL(adj.nom, ''), ' ', IFNULL(adj.prenom, '')) AS adjNomComplet,
                   (SELECT COUNT(*) FROM Personnel p WHERE p.idDepartement = d.idDepartement AND p.statutEmploi = 'ACTIF' AND p.deleted_at IS NULL) AS effectifActuel
            """ + clause + " ORDER BY d.ordre_affichage ASC, d.libelle ASC LIMIT ? OFFSET ?";

        List<DepartementLigne> lignes = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sqlPage)) {

            int index = 1;
            for (Object param : parametres) stmt.setObject(index++, param);
            stmt.setInt(index++, filtre.taillePage());
            stmt.setInt(index, filtre.offset());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lignes.add(mapToLigne(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur rechercherPagine Departement : " + e.getMessage());
        }

        return new ResultatPagine<>(lignes, total, filtre.page(), filtre.taillePage());
    }

    // ========== MÉTHODES MÉTIER D'EXPERTISE ==========

    @Override
    public long compterActifsParEcole(String idEcole) {
        String sql = "SELECT COUNT(*) FROM Departement WHERE idEcole = ? AND statut = 'ACTIF' AND deleted_at IS NULL";
        return compterQuery(sql, List.of(idEcole));
    }

    @Override
    public boolean reassignerResponsable(String idDepartement, String idResponsable, String idResponsableAdjoint) {
        String sql = "UPDATE Departement SET idResponsable = ?, idResponsableAdjoint = ?, version = version + 1 WHERE idDepartement = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableString(stmt, 1, idResponsable);
            setNullableString(stmt, 2, idResponsableAdjoint);
            stmt.setString(3, idDepartement);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur reassignerResponsable : " + e.getMessage());
            return false;
        }
    }

    @Override
    public int compterPersonnelRattache(String idDepartement) {
        String sql = "SELECT COUNT(*) FROM Personnel WHERE idDepartement = ? AND deleted_at IS NULL";
        return (int) compterQuery(sql, List.of(idDepartement));
    }

    @Override
    public boolean existeCode(String idEcole, String code, String idExclu) {
        String sql = "SELECT COUNT(*) FROM Departement WHERE idEcole = ? AND code = ? AND deleted_at IS NULL " +
                (idExclu != null ? "AND idDepartement <> ?" : "");
        List<Object> params = idExclu != null ? List.of(idEcole, code, idExclu) : List.of(idEcole, code);
        return compterQuery(sql, params) > 0;
    }

    @Override
    public boolean existeLibelle(String idEcole, String libelle, String idExclu) {
        String sql = "SELECT COUNT(*) FROM Departement WHERE idEcole = ? AND libelle = ? AND deleted_at IS NULL " +
                (idExclu != null ? "AND idDepartement <> ?" : "");
        List<Object> params = idExclu != null ? List.of(idEcole, libelle, idExclu) : List.of(idEcole, libelle);
        return compterQuery(sql, params) > 0;
    }

    @Override
    public boolean aDesEnfantsActifs(String idDepartement) {
        String sql = "SELECT COUNT(*) FROM Departement WHERE idDepartementParent = ? AND deleted_at IS NULL";
        return compterQuery(sql, List.of(idDepartement)) > 0;
    }

    // ========== HELPERS & MAPPINGS ==========

    private long compterQuery(String sql, List<Object> params) {
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int index = 1;
            for (Object p : params) stmt.setObject(index++, p);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage Departement : " + e.getMessage());
        }
        return 0;
    }

    private Departement mapToDepartement(ResultSet rs) throws SQLException {
        Departement d = new Departement();
        d.setIdDepartement(rs.getString("idDepartement"));
        d.setIdEcole(rs.getString("idEcole"));
        d.setCode(rs.getString("code"));
        d.setLibelle(rs.getString("libelle"));
        d.setDescription(rs.getString("description"));

        String typeStr = rs.getString("type");
        if (typeStr != null) d.setType(TypeDepartement.valueOf(typeStr));

        String sousSystStr = rs.getString("sousSysteme");
        if (sousSystStr != null) d.setSousSysteme(SousSysteme.valueOf(sousSystStr));

        d.setEstConseilEnseignement(rs.getBoolean("estConseilEnseignement"));
        d.setIdDepartementParent(rs.getString("idDepartementParent"));
        d.setNiveau((Integer) rs.getObject("niveau"));
        d.setChemin(rs.getString("chemin"));
        d.setOrdreAffichage((Integer) rs.getObject("ordre_affichage"));
        d.setIdResponsable(rs.getString("idResponsable"));
        d.setIdResponsableAdjoint(rs.getString("idResponsableAdjoint"));

        String statutStr = rs.getString("statut");
        if (statutStr != null) d.setStatut(StatutDepartement.valueOf(statutStr));

        Date dDeb = rs.getDate("date_debut");
        if (dDeb != null) d.setDateDebut(dDeb.toLocalDate());
        Date dFin = rs.getDate("date_fin");
        if (dFin != null) d.setDateFin(dFin.toLocalDate());

        d.setMotifDesactivation(rs.getString("motif_desactivation"));
        d.setCodeCentreCout(rs.getString("code_centre_cout"));
        d.setCodeMineduc(rs.getString("codeMineduc"));
        d.setBudgetAnnuel(rs.getBigDecimal("budget_annuel"));
        d.setEffectifMax((Integer) rs.getObject("effectif_max"));
        d.setEmail(rs.getString("email"));
        d.setTelephone(rs.getString("telephone"));
        d.setLocalisation(rs.getString("localisation"));

        d.setVersion(rs.getLong("version"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) d.setDeletedAt(deletedAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) d.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) d.setUpdatedAt(updatedAt.toLocalDateTime());

        return d;
    }

    private DepartementLigne mapToLigne(ResultSet rs) throws SQLException {
        String typeStr = rs.getString("type");
        String sousSystStr = rs.getString("sousSysteme");
        String statutStr = rs.getString("statut");

        return new DepartementLigne(
                rs.getString("idDepartement"),
                rs.getString("code"),
                rs.getString("libelle"),
                typeStr != null ? TypeDepartement.valueOf(typeStr) : null,
                sousSystStr != null ? SousSysteme.valueOf(sousSystStr) : null,
                rs.getBoolean("estConseilEnseignement"),
                rs.getString("parentLibelle"),
                rs.getString("respNomComplet").trim(),
                rs.getString("adjNomComplet").trim(),
                rs.getInt("effectifActuel"),
                (Integer) rs.getObject("effectif_max"),
                statutStr != null ? StatutDepartement.valueOf(statutStr) : null,
                rs.getBigDecimal("budget_annuel"),
                rs.getString("codeMineduc")
        );
    }

    // ========== UTILS JDBC NULLABLE ==========

    private static void setNullableString(PreparedStatement stmt, int idx, String val) throws SQLException {
        if (val == null || val.isBlank()) stmt.setNull(idx, Types.VARCHAR);
        else stmt.setString(idx, val);
    }

    private static void setNullableInteger(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val == null) stmt.setNull(idx, Types.INTEGER);
        else stmt.setInt(idx, val);
    }

    private static void setNullableBigDecimal(PreparedStatement stmt, int idx, java.math.BigDecimal val) throws SQLException {
        if (val == null) stmt.setNull(idx, Types.DECIMAL);
        else stmt.setBigDecimal(idx, val);
    }

    private static void setNullableDate(PreparedStatement stmt, int idx, java.time.LocalDate val) throws SQLException {
        if (val == null) stmt.setNull(idx, Types.DATE);
        else stmt.setDate(idx, Date.valueOf(val));
    }
}