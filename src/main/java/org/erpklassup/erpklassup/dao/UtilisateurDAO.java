package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.FiltreUtilisateur;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.dto.UtilisateurLigne;
import org.erpklassup.erpklassup.models.Utilisateur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UtilisateurDAO {

    // ========== CREATE ==========
    public boolean create(Utilisateur user) {
        String sql = "INSERT INTO Utilisateur (idUtilisateur, idEcole, idGroupePrincipal, " +
                "username, passwordHash, nom, prenom, email, telephone, " +
                "languePreference, fuseauHoraire, typeUtilisateur, estActif) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getIdUtilisateur());
            stmt.setString(2, user.getIdEcole());
            stmt.setString(3, user.getIdGroupePrincipal());
            stmt.setString(4, user.getUsername());
            stmt.setString(5, user.getPasswordHash());
            stmt.setString(6, user.getNom());
            stmt.setString(7, user.getPrenom());
            stmt.setString(8, user.getEmail());
            stmt.setString(9, user.getTelephone());
            stmt.setString(10, user.getLanguePreference());
            stmt.setString(11, user.getFuseauHoraire());
            stmt.setString(12, user.getTypeUtilisateur());
            stmt.setBoolean(13, user.isEstActif());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur create utilisateur : " + e.getMessage());
            return false;
        }
    }

    // ========== READ ==========
    public Optional<Utilisateur> findById(String idUtilisateur) {
        String sql = "SELECT * FROM Utilisateur WHERE idUtilisateur = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idUtilisateur);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToUtilisateur(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Utilisateur> findByUsername(String username, String idEcole) {
        String sql = "SELECT * FROM Utilisateur WHERE username = ? AND idEcole = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToUtilisateur(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUsername : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Utilisateur> findByEmail(String email) {
        String sql = "SELECT * FROM Utilisateur WHERE email = ? AND deleted_at IS NULL";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapToUtilisateur(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEmail : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Utilisateur> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Utilisateur WHERE idEcole = ? AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Utilisateur> utilisateurs = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idEcole);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) utilisateurs.add(mapToUtilisateur(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole : " + e.getMessage());
        }
        return utilisateurs;
    }

    // ========== UPDATE ==========
    public boolean update(Utilisateur user) {
        String sql = "UPDATE Utilisateur SET idGroupePrincipal = ?, " +
                "nom = ?, prenom = ?, email = ?, telephone = ?, " +
                "languePreference = ?, fuseauHoraire = ?, typeUtilisateur = ?, " +
                "estActif = ?, version = version + 1 WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getIdGroupePrincipal());
            stmt.setString(2, user.getNom());
            stmt.setString(3, user.getPrenom());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getTelephone());
            stmt.setString(6, user.getLanguePreference());
            stmt.setString(7, user.getFuseauHoraire());
            stmt.setString(8, user.getTypeUtilisateur());
            stmt.setBoolean(9, user.isEstActif());
            stmt.setString(10, user.getIdUtilisateur());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update : " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(String idUtilisateur, String nouveauHash) {
        String sql = "UPDATE Utilisateur SET passwordHash = ?, " +
                "dateChangementMotDePasse = NOW(3), doitChangerMotDePasse = 0, " +
                "version = version + 1 WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nouveauHash);
            stmt.setString(2, idUtilisateur);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur updatePassword : " + e.getMessage());
            return false;
        }
    }

    public void incrementerTentativesEchec(String idUtilisateur) {
        String sql = "UPDATE Utilisateur SET nombreTentativesEchec = nombreTentativesEchec + 1, " +
                "dateDernierEchec = NOW(3) WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur incrementerTentativesEchec : " + e.getMessage());
        }
    }

    public void reinitialiserTentativesEchec(String idUtilisateur) {
        String sql = "UPDATE Utilisateur SET nombreTentativesEchec = 0, dateDernierEchec = NULL " +
                "WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur reinitialiserTentativesEchec : " + e.getMessage());
        }
    }

    public void verrouillerCompte(String idUtilisateur, int dureeMinutes) {
        String sql = "UPDATE Utilisateur SET compteVerrouille = 1, " +
                "verrouilleJusqua = DATE_ADD(NOW(3), INTERVAL ? MINUTE) WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dureeMinutes);
            stmt.setString(2, idUtilisateur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur verrouillerCompte : " + e.getMessage());
        }
    }

    public void deverrouillerCompte(String idUtilisateur) {
        String sql = "UPDATE Utilisateur SET compteVerrouille = 0, verrouilleJusqua = NULL, " +
                "nombreTentativesEchec = 0 WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur deverrouillerCompte : " + e.getMessage());
        }
    }

    // ========== DELETE (SOFT) ==========
    public boolean softDelete(String idUtilisateur) {
        String sql = "UPDATE Utilisateur SET deleted_at = NOW(3), version = version + 1 " +
                "WHERE idUtilisateur = ?";

        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idUtilisateur);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete : " + e.getMessage());
            return false;
        }
    }

    // ========== RECHERCHE PAGINÉE (pour UtilisateursView) ==========
    public ResultatPagine<UtilisateurLigne> rechercherPagine(FiltreUtilisateur filtre) {
        StringBuilder clause = new StringBuilder("""
             FROM Utilisateur u
             WHERE u.deleted_at IS NULL
             AND u.idEcole = ?
            """);

        List<Object> parametres = new ArrayList<>();
        parametres.add(filtre.idEcole());

        if (filtre.idRole() != null && !filtre.idRole().isBlank()) {
            clause.append("""
                 AND EXISTS (
                     SELECT 1 FROM UtilisateurRole ur
                     WHERE ur.idUtilisateur = u.idUtilisateur AND ur.idRole = ? AND ur.estActive = 1
                 )
                """);
            parametres.add(filtre.idRole());
        }
        if (filtre.terme() != null && !filtre.terme().isBlank()) {
            clause.append(" AND (u.nom LIKE ? OR u.prenom LIKE ? OR u.email LIKE ? OR u.username LIKE ?) ");
            String motif = "%" + filtre.terme().trim() + "%";
            for (int i = 0; i < 4; i++) parametres.add(motif);
        }
        if (filtre.statut() != null) {
            clause.append(switch (filtre.statut()) {
                case ACTIF -> " AND u.estActif = 1 AND u.compteVerrouille = 0 ";
                case VERROUILLE -> " AND u.compteVerrouille = 1 ";
                case INACTIF -> " AND u.estActif = 0 ";
            });
        }

        long total = compterUtilisateurs(clause.toString(), parametres);

        String sqlPage = """
            SELECT u.idUtilisateur, u.username, u.nom, u.prenom, u.email, u.telephone,
                   u.estActif, u.compteVerrouille, u.deuxFacteursActive,
                   (SELECT GROUP_CONCAT(DISTINCT r.nomRole ORDER BY r.nomRole SEPARATOR ', ')
                      FROM UtilisateurRole ur INNER JOIN Role r ON r.idRole = ur.idRole
                      WHERE ur.idUtilisateur = u.idUtilisateur AND ur.estActive = 1) AS roles,
                   (SELECT MAX(s.dateDerniereActivite) FROM SessionUtilisateur s
                      WHERE s.idUtilisateur = u.idUtilisateur) AS derniereConnexion,
                   COALESCE(
                       (SELECT p.photo FROM Personnel p WHERE p.idUtilisateur = u.idUtilisateur AND p.deleted_at IS NULL LIMIT 1),
                       (SELECT el.photoPath FROM Eleve el WHERE el.idUtilisateur = u.idUtilisateur AND el.deleted_at IS NULL LIMIT 1)
                   ) AS photoPath
            """ + clause + " ORDER BY u.nom, u.prenom LIMIT ? OFFSET ?";

        List<UtilisateurLigne> lignes = new ArrayList<>();

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
            System.err.println("Erreur rechercherPagine : " + e.getMessage());
        }

        return new ResultatPagine<>(lignes, total, filtre.page(), filtre.taillePage());
    }

    private long compterUtilisateurs(String clauseWhere, List<Object> parametres) {
        String sql = "SELECT COUNT(*) AS total " + clauseWhere;
        try (Connection conn = Database.getConnexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int index = 1;
            for (Object param : parametres) stmt.setObject(index++, param);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (SQLException e) {
            System.err.println("Erreur comptage utilisateurs : " + e.getMessage());
        }
        return 0;
    }

    // ========== HELPERS DE MAPPING ==========
    private Utilisateur mapToUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur user = new Utilisateur();
        user.setIdUtilisateur(rs.getString("idUtilisateur"));
        user.setIdEcole(rs.getString("idEcole"));
        user.setIdGroupePrincipal(rs.getString("idGroupePrincipal"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("passwordHash"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setTelephone(rs.getString("telephone"));
        user.setEmailVerifie(rs.getBoolean("emailVerifie"));
        user.setHashTokenVerificationEmail(rs.getString("hashTokenVerificationEmail"));
        user.setLanguePreference(rs.getString("languePreference"));
        user.setFuseauHoraire(rs.getString("fuseauHoraire"));
        user.setSecret2FA(rs.getString("secret2FA"));
        user.setTelephone2FA(rs.getString("telephone2FA"));
        user.setDeuxFacteursActive(rs.getBoolean("deuxFacteursActive"));
        user.setTypeUtilisateur(rs.getString("typeUtilisateur"));
        user.setCompteVerrouille(rs.getBoolean("compteVerrouille"));
        user.setNombreTentativesEchec(rs.getInt("nombreTentativesEchec"));
        user.setEstActif(rs.getBoolean("estActif"));
        user.setVersion(rs.getLong("version"));

        Timestamp dateVerif = rs.getTimestamp("dateVerificationEmail");
        if (dateVerif != null) user.setDateVerificationEmail(dateVerif.toLocalDateTime());
        Timestamp dateChangement = rs.getTimestamp("dateChangementMotDePasse");
        if (dateChangement != null) user.setDateChangementMotDePasse(dateChangement.toLocalDateTime());
        Timestamp verrouilleJusqua = rs.getTimestamp("verrouilleJusqua");
        if (verrouilleJusqua != null) user.setVerrouilleJusqua(verrouilleJusqua.toLocalDateTime());
        Timestamp dateDernierEchec = rs.getTimestamp("dateDernierEchec");
        if (dateDernierEchec != null) user.setDateDernierEchec(dateDernierEchec.toLocalDateTime());
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) user.setDeletedAt(deletedAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toLocalDateTime());

        return user;
    }

    private UtilisateurLigne mapToLigne(ResultSet rs) throws SQLException {
        String prenom = rs.getString("prenom");
        String nom = rs.getString("nom");
        String nomComplet = (prenom != null && !prenom.isBlank()) ? prenom + " " + nom : nom;
        Timestamp derniereConnexionTs = rs.getTimestamp("derniereConnexion");

        return new UtilisateurLigne(
                rs.getString("idUtilisateur"),
                rs.getString("username"),
                nom,
                prenom,
                nomComplet,
                rs.getString("email"),
                rs.getString("telephone"),
                rs.getString("roles"),
                rs.getString("photoPath"),
                rs.getBoolean("estActif"),
                rs.getBoolean("compteVerrouille"),
                rs.getBoolean("deuxFacteursActive"),
                derniereConnexionTs != null ? derniereConnexionTs.toLocalDateTime() : null
        );
    }
}