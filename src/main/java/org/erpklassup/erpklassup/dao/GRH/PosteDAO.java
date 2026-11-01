package org.erpklassup.erpklassup.dao.GRH;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.FiltrePoste;
import org.erpklassup.erpklassup.dto.PosteLigne;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.models.GRH.Poste;
import org.erpklassup.erpklassup.util.grh.SqlFiltreHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PosteDAO {

    public boolean create(Poste p) {
        String sql = "INSERT INTO Poste " +
                "(idPoste, idEcole, idDepartement, intitule, categorie, description, estActif) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getIdPoste());
            stmt.setString(2, p.getIdEcole());
            setNullableString(stmt, 3, p.getIdDepartement());
            stmt.setString(4, p.getIntitule());
            setNullableString(stmt, 5, p.getCategorie());
            setNullableString(stmt, 6, p.getDescription());
            stmt.setBoolean(7, p.isEstActif());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create Poste : " + e.getMessage());
            return false;
        }
    }

    public Optional<Poste> findById(String id) {
        String sql = "SELECT * FROM Poste WHERE idPoste = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById Poste : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Poste> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Poste WHERE idEcole = ? AND deleted_at IS NULL ORDER BY intitule";
        List<Poste> liste = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) liste.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole Poste : " + e.getMessage());
        }
        return liste;
    }

    public List<Poste> findByCategorie(String idEcole, String categorie) {
        String sql = "SELECT * FROM Poste WHERE idEcole = ? AND categorie = ? " +
                "AND deleted_at IS NULL ORDER BY intitule";
        List<Poste> liste = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            stmt.setString(2, categorie);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) liste.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByCategorie Poste : " + e.getMessage());
        }
        return liste;
    }

    public boolean existeIntitule(String idEcole, String intitule, String idExclu) {
        String sql = "SELECT COUNT(*) FROM Poste WHERE idEcole = ? AND intitule = ? " +
                "AND deleted_at IS NULL " + (idExclu != null ? "AND idPoste <> ? " : "");
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            stmt.setString(2, intitule);
            if (idExclu != null) stmt.setString(3, idExclu);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur existeIntitule Poste : " + e.getMessage());
        }
        return false;
    }

    public boolean update(Poste p) {
        String sql = "UPDATE Poste SET idDepartement = ?, intitule = ?, categorie = ?, description = ?, " +
                "estActif = ?, version = version + 1 " +
                "WHERE idPoste = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableString(stmt, 1, p.getIdDepartement());
            stmt.setString(2, p.getIntitule());
            setNullableString(stmt, 3, p.getCategorie());
            setNullableString(stmt, 4, p.getDescription());
            stmt.setBoolean(5, p.isEstActif());
            stmt.setString(6, p.getIdPoste());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update Poste : " + e.getMessage());
            return false;
        }
    }

    public boolean softDelete(String id) {
        String sql = "UPDATE Poste SET deleted_at = NOW(3), version = version + 1 WHERE idPoste = ?";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete Poste : " + e.getMessage());
            return false;
        }
    }

    private Poste map(ResultSet rs) throws SQLException {
        Poste p = new Poste();
        p.setIdPoste(rs.getString("idPoste"));
        p.setIdEcole(rs.getString("idEcole"));
        p.setIdDepartement(rs.getString("idDepartement"));
        p.setIntitule(rs.getString("intitule"));
        p.setCategorie(rs.getString("categorie"));
        p.setDescription(rs.getString("description"));
        p.setEstActif(rs.getBoolean("estActif"));
        p.setVersion(rs.getLong("version"));
        Timestamp d = rs.getTimestamp("deleted_at"); if (d != null) p.setDeletedAt(d.toLocalDateTime());
        Timestamp c = rs.getTimestamp("created_at"); if (c != null) p.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); if (u != null) p.setUpdatedAt(u.toLocalDateTime());
        return p;
    }

    private static void setNullableString(PreparedStatement stmt, int idx, String val) throws SQLException {
        if (val == null || val.isBlank()) stmt.setNull(idx, Types.VARCHAR);
        else stmt.setString(idx, val);
    }

    public ResultatPagine<PosteLigne> rechercherPagine(FiltrePoste filtre) {
        SqlFiltreHelper helper = new SqlFiltreHelper();
        helper.ajouterCondition(filtre.idEcole() != null, "p.idEcole = ?", filtre.idEcole());
        helper.ajouterCondition(filtre.idDepartement() != null && !filtre.idDepartement().isBlank(), "p.idDepartement = ?", filtre.idDepartement());
        helper.ajouterCondition(filtre.categorie() != null && !filtre.categorie().isBlank(), "p.categorie = ?", filtre.categorie());
        helper.ajouterCondition(filtre.estActif() != null, "p.estActif = ?", filtre.estActif());
        helper.ajouterConditionLike(filtre.terme(), "p.intitule", "p.description");

        String sqlCount = "SELECT COUNT(*) FROM Poste p " + helper.getClauseWhere() + " AND p.deleted_at IS NULL";

        String sqlData = "SELECT p.*, d.libelle as departement_libelle, " +
                "(SELECT COUNT(*) FROM Personnel pers WHERE pers.idPoste = p.idPoste AND pers.deleted_at IS NULL) as effectif " +
                "FROM Poste p " +
                "LEFT JOIN Departement d ON p.idDepartement = d.idDepartement " +
                helper.getClauseWhere() + " AND p.deleted_at IS NULL " +
                "ORDER BY p.intitule LIMIT ? OFFSET ?";

        List<PosteLigne> elements = new ArrayList<>();
        long total = 0;

        try (Connection conn = Database.getConnexion()) {
            try (PreparedStatement stmtCount = conn.prepareStatement(sqlCount)) {
                helper.injecterParametres(stmtCount);
                try (ResultSet rs = stmtCount.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }

            try (PreparedStatement stmtData = conn.prepareStatement(sqlData)) {
                helper.injecterParametres(stmtData);
                int paramCount = stmtData.getParameterMetaData().getParameterCount();
                stmtData.setInt(paramCount - 1, filtre.taillePage());
                stmtData.setInt(paramCount, filtre.offset());

                try (ResultSet rs = stmtData.executeQuery()) {
                    while (rs.next()) {
                        elements.add(new PosteLigne(
                                rs.getString("idPoste"),
                                rs.getString("intitule"),
                                rs.getString("categorie"),
                                rs.getString("description"),
                                rs.getString("idDepartement"),
                                rs.getString("departement_libelle"),
                                rs.getInt("effectif"),
                                rs.getBoolean("estActif")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur rechercherPagine Poste : " + e.getMessage());
        }

        return new ResultatPagine<>(elements, total, filtre.page(), filtre.taillePage());
    }

    public long compterActifsParEcole(String idEcole) {
        String sql = "SELECT COUNT(*) FROM Poste WHERE idEcole = ? AND estActif = 1 AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEcole);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur compterActifsParEcole Poste : " + e.getMessage());
        }
        return 0;
    }

    public int compterPersonnelRattache(String idPoste) {
        String sql = "SELECT COUNT(*) FROM Personnel WHERE idPoste = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idPoste);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }
}