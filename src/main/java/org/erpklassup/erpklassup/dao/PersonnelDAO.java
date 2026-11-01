package org.erpklassup.erpklassup.dao;

import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dto.FiltrePersonnel;
import org.erpklassup.erpklassup.dto.PersonnelLigne;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.models.GRH.Personnel;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO aligné sur la table Personnel v2.
 *
 * Règles respectées :
 *  - Multi-tenant : filtre idEcole sur toutes les lectures métier.
 *  - Soft delete : jamais de DELETE physique, uniquement deleted_at = NOW(3) + version + 1.
 *  - Verrou optimiste : l'update vérifie version = ? et incrémente version.
 *  - Colonnes générées (matriculeInterne_actif, idUtilisateur_actif, numCnps_actif)
 *    jamais écrites : MySQL les calcule.
 *  - Les colonnes techniques created_at / updated_at ne sont jamais écrites en insert ;
 *    elles ont un DEFAULT CURRENT_TIMESTAMP(3) et un ON UPDATE.
 */
public class PersonnelDAO {

    // ==================================================================
    // CREATE
    // ==================================================================
    public boolean insert(Personnel p) {
        if (p.getIdPersonnel() == null || p.getIdPersonnel().isBlank()) {
            p.setIdPersonnel(UUID.randomUUID().toString());
        }

        String sql = """
            INSERT INTO Personnel (
                idPersonnel, idEcole, idUtilisateur, idDepartement, idResponsable, idEchelon,
                matriculeInterne, nom, prenom, sexe, dateNaissance, lieuNaissance,
                nationalite, situationMatrimoniale, nombreEnfants,
                numeroCni, niu, numCnps,
                telephone, contactUrgence, contactUrgenceNom, email, adresse, ville, photo,
                typePersonnel, posteOuFonction, qualification, statutContractuel,
                quotaHeuresHebdo, dechargeHeures, dateEmbauche, dateDepart, motifDepart,
                statutEmploi, modePaiement, nomBanque, ibanOuRib, numeroMobileMoney
            ) VALUES (
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?
            )
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, p.getIdPersonnel());
            ps.setString(i++, p.getIdEcole());
            setNullableString(ps, i++, p.getIdUtilisateur());
            setNullableString(ps, i++, p.getIdDepartement());
            setNullableString(ps, i++, p.getIdResponsable());
            setNullableString(ps, i++, p.getIdEchelon());

            ps.setString(i++, p.getMatriculeInterne());
            ps.setString(i++, p.getNom());
            setNullableString(ps, i++, p.getPrenom());
            setNullableString(ps, i++, p.getSexe());
            setNullableDate(ps, i++, p.getDateNaissance());
            setNullableString(ps, i++, p.getLieuNaissance());
            setNullableString(ps, i++, p.getNationalite());
            setNullableString(ps, i++, p.getSituationMatrimoniale());
            ps.setInt(i++, p.getNombreEnfants());
            setNullableString(ps, i++, p.getNumeroCni());
            setNullableString(ps, i++, p.getNiu());
            setNullableString(ps, i++, p.getNumCnps());

            setNullableString(ps, i++, p.getTelephone());
            setNullableString(ps, i++, p.getContactUrgence());
            setNullableString(ps, i++, p.getContactUrgenceNom());
            setNullableString(ps, i++, p.getEmail());
            setNullableString(ps, i++, p.getAdresse());
            setNullableString(ps, i++, p.getVille());
            setNullableString(ps, i++, p.getPhoto());

            ps.setString(i++, p.getTypePersonnel());
            ps.setString(i++, p.getPosteOuFonction());
            setNullableString(ps, i++, p.getQualification());
            ps.setString(i++, p.getStatutContractuel() != null ? p.getStatutContractuel() : "PERMANENT");

            setNullableDecimal(ps, i++, p.getQuotaHeuresHebdo());
            ps.setBigDecimal(i++, p.getDechargeHeures() != null ? p.getDechargeHeures() : BigDecimal.ZERO);
            setNullableDate(ps, i++, p.getDateEmbauche());
            setNullableDate(ps, i++, p.getDateDepart());
            setNullableString(ps, i++, p.getMotifDepart());

            ps.setString(i++, p.getStatutEmploi() != null ? p.getStatutEmploi() : "ACTIF");
            ps.setString(i++, p.getModePaiement() != null ? p.getModePaiement() : "ESPECES");
            setNullableString(ps, i++, p.getNomBanque());
            setNullableString(ps, i++, p.getIbanOuRib());
            setNullableString(ps, i++, p.getNumeroMobileMoney());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur insert Personnel : " + e.getMessage());
            return false;
        }
    }

    /** Lie un Personnel à un Utilisateur (compte de connexion). */
    public boolean lierUtilisateur(String idPersonnel, String idUtilisateur) {
        String sql = "UPDATE Personnel SET idUtilisateur = ?, updated_at = NOW(3), version = version + 1 " +
                "WHERE idPersonnel = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableString(ps, 1, idUtilisateur);
            ps.setString(2, idPersonnel);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lierUtilisateur Personnel : " + e.getMessage());
            return false;
        }
    }

    // ==================================================================
    // READ
    // ==================================================================
    public Optional<Personnel> findById(String idPersonnel) {
        String sql = "SELECT * FROM Personnel WHERE idPersonnel = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPersonnel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById Personnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Personnel> findByUtilisateurId(String idUtilisateur) {
        String sql = "SELECT * FROM Personnel WHERE idUtilisateur = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByUtilisateurId Personnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Personnel> findByMatricule(String idEcole, String matricule) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND matriculeInterne = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByMatricule Personnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Personnel> findByNumCnps(String idEcole, String numCnps) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND numCnps = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, numCnps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByNumCnps Personnel : " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Personnel> findByEcole(String idEcole) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND deleted_at IS NULL " +
                "ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcole Personnel : " + e.getMessage());
        }
        return result;
    }

    public List<Personnel> findByEcoleEtStatut(String idEcole, String statutEmploi) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND statutEmploi = ? " +
                "AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, statutEmploi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByEcoleEtStatut Personnel : " + e.getMessage());
        }
        return result;
    }

    public List<Personnel> findByDepartement(String idEcole, String idDepartement) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND idDepartement = ? " +
                "AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, idDepartement);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByDepartement Personnel : " + e.getMessage());
        }
        return result;
    }

    public List<Personnel> findByResponsable(String idResponsable) {
        String sql = "SELECT * FROM Personnel WHERE idResponsable = ? AND deleted_at IS NULL " +
                "ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idResponsable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByResponsable Personnel : " + e.getMessage());
        }
        return result;
    }

    public List<Personnel> findSansCompte(String idEcole) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND idUtilisateur IS NULL " +
                "AND estActif = 1 AND deleted_at IS NULL ORDER BY nom, prenom";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findSansCompte Personnel : " + e.getMessage());
        }
        return result;
    }

    /** Recherche multi-critères (nom, prénom, matricule, CNI, CNPS, téléphone). */
    public List<Personnel> rechercher(String idEcole, String terme) {
        String sql = "SELECT * FROM Personnel WHERE idEcole = ? AND deleted_at IS NULL " +
                "AND (nom LIKE ? OR prenom LIKE ? OR matriculeInterne LIKE ? " +
                "     OR numeroCni LIKE ? OR numCnps LIKE ? OR telephone LIKE ?) " +
                "ORDER BY nom, prenom LIMIT 200";
        List<Personnel> result = new ArrayList<>();
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String motif = "%" + (terme == null ? "" : terme.trim()) + "%";
            ps.setString(1, idEcole);
            for (int k = 0; k < 6; k++) ps.setString(2 + k, motif);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur rechercher Personnel : " + e.getMessage());
        }
        return result;
    }

    /** Détecte un éventuel doublon de matricule avant insert / update. */
    public boolean existeMatricule(String idEcole, String matricule, String idExclu) {
        String sql = "SELECT COUNT(*) FROM Personnel WHERE idEcole = ? AND matriculeInterne = ? " +
                "AND deleted_at IS NULL" + (idExclu != null ? " AND idPersonnel <> ?" : "");
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, matricule);
            if (idExclu != null) ps.setString(3, idExclu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur existeMatricule Personnel : " + e.getMessage());
        }
        return false;
    }

    public boolean existeNumCnps(String idEcole, String numCnps, String idExclu) {
        String sql = "SELECT COUNT(*) FROM Personnel WHERE idEcole = ? AND numCnps = ? " +
                "AND deleted_at IS NULL" + (idExclu != null ? " AND idPersonnel <> ?" : "");
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEcole);
            ps.setString(2, numCnps);
            if (idExclu != null) ps.setString(3, idExclu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur existeNumCnps Personnel : " + e.getMessage());
        }
        return false;
    }

    // ==================================================================
    // UPDATE — verrou optimiste sur version
    // ==================================================================
    public boolean update(Personnel p) {
        String sql = """
            UPDATE Personnel SET
                idUtilisateur = ?, idDepartement = ?, idResponsable = ?, idEchelon = ?,
                matriculeInterne = ?, nom = ?, prenom = ?, sexe = ?, dateNaissance = ?,
                lieuNaissance = ?, nationalite = ?, situationMatrimoniale = ?, nombreEnfants = ?,
                numeroCni = ?, niu = ?, numCnps = ?,
                telephone = ?, contactUrgence = ?, contactUrgenceNom = ?, email = ?,
                adresse = ?, ville = ?, photo = ?,
                typePersonnel = ?, posteOuFonction = ?, qualification = ?, statutContractuel = ?,
                quotaHeuresHebdo = ?, dechargeHeures = ?, dateEmbauche = ?, dateDepart = ?,
                motifDepart = ?, statutEmploi = ?,
                modePaiement = ?, nomBanque = ?, ibanOuRib = ?, numeroMobileMoney = ?,
                version = version + 1,
                updated_at = NOW(3)
            WHERE idPersonnel = ? AND version = ? AND deleted_at IS NULL
            """;

        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            setNullableString(ps, i++, p.getIdUtilisateur());
            setNullableString(ps, i++, p.getIdDepartement());
            setNullableString(ps, i++, p.getIdResponsable());
            setNullableString(ps, i++, p.getIdEchelon());

            ps.setString(i++, p.getMatriculeInterne());
            ps.setString(i++, p.getNom());
            setNullableString(ps, i++, p.getPrenom());
            setNullableString(ps, i++, p.getSexe());
            setNullableDate(ps, i++, p.getDateNaissance());
            setNullableString(ps, i++, p.getLieuNaissance());
            setNullableString(ps, i++, p.getNationalite());
            setNullableString(ps, i++, p.getSituationMatrimoniale());
            ps.setInt(i++, p.getNombreEnfants());
            setNullableString(ps, i++, p.getNumeroCni());
            setNullableString(ps, i++, p.getNiu());
            setNullableString(ps, i++, p.getNumCnps());

            setNullableString(ps, i++, p.getTelephone());
            setNullableString(ps, i++, p.getContactUrgence());
            setNullableString(ps, i++, p.getContactUrgenceNom());
            setNullableString(ps, i++, p.getEmail());
            setNullableString(ps, i++, p.getAdresse());
            setNullableString(ps, i++, p.getVille());
            setNullableString(ps, i++, p.getPhoto());

            ps.setString(i++, p.getTypePersonnel());
            ps.setString(i++, p.getPosteOuFonction());
            setNullableString(ps, i++, p.getQualification());
            ps.setString(i++, p.getStatutContractuel() != null ? p.getStatutContractuel() : "PERMANENT");

            setNullableDecimal(ps, i++, p.getQuotaHeuresHebdo());
            ps.setBigDecimal(i++, p.getDechargeHeures() != null ? p.getDechargeHeures() : BigDecimal.ZERO);
            setNullableDate(ps, i++, p.getDateEmbauche());
            setNullableDate(ps, i++, p.getDateDepart());
            setNullableString(ps, i++, p.getMotifDepart());

            ps.setString(i++, p.getStatutEmploi() != null ? p.getStatutEmploi() : "ACTIF");
            ps.setString(i++, p.getModePaiement() != null ? p.getModePaiement() : "ESPECES");
            setNullableString(ps, i++, p.getNomBanque());
            setNullableString(ps, i++, p.getIbanOuRib());
            setNullableString(ps, i++, p.getNumeroMobileMoney());

            ps.setString(i++, p.getIdPersonnel());
            ps.setLong(i, p.getVersion());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.err.println("Update Personnel : version conflictuelle ou enregistrement supprimé " +
                        "(id=" + p.getIdPersonnel() + ", version=" + p.getVersion() + ")");
            } else {
                p.setVersion(p.getVersion() + 1);
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur update Personnel : " + e.getMessage());
            return false;
        }
    }

    // ==================================================================
    // DELETE (SOFT)
    // ==================================================================
    public boolean softDelete(String idPersonnel) {
        String sql = "UPDATE Personnel SET deleted_at = NOW(3), statutEmploi = 'SORTI', " +
                "version = version + 1 WHERE idPersonnel = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPersonnel);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur softDelete Personnel : " + e.getMessage());
            return false;
        }
    }

    // ==================================================================
    // MAPPING ResultSet -> Personnel
    // ==================================================================
    private Personnel mapRow(ResultSet rs) throws SQLException {
        Personnel p = new Personnel();

        // --- Identifiants & liens ---
        p.setIdPersonnel(rs.getString("idPersonnel"));
        p.setIdEcole(rs.getString("idEcole"));
        p.setIdUtilisateur(rs.getString("idUtilisateur"));
        p.setIdDepartement(rs.getString("idDepartement"));
        p.setIdResponsable(rs.getString("idResponsable"));
//        p.setIdEchelon(rs.getString("idEchelon"));

        // --- Identification & état civil ---
        p.setMatriculeInterne(rs.getString("matriculeInterne"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setSexe(rs.getString("sexe"));
        Date dateNaissance = rs.getDate("dateNaissance");
        p.setDateNaissance(dateNaissance != null ? dateNaissance.toLocalDate() : null);
        p.setLieuNaissance(rs.getString("lieuNaissance"));
        p.setNationalite(rs.getString("nationalite"));
        p.setSituationMatrimoniale(rs.getString("situationMatrimoniale"));
        p.setNombreEnfants(rs.getInt("nombreEnfants"));
        p.setNumeroCni(rs.getString("numeroCni"));
        p.setNiu(rs.getString("niu"));
        p.setNumCnps(rs.getString("numCnps"));

        // --- Contacts & adresse ---
        p.setTelephone(rs.getString("telephone"));
        p.setContactUrgence(rs.getString("contactUrgence"));
        p.setContactUrgenceNom(rs.getString("contactUrgenceNom"));
        p.setEmail(rs.getString("email"));
        p.setAdresse(rs.getString("adresse"));
        p.setVille(rs.getString("ville"));
        p.setPhoto(rs.getString("photo"));

        // --- Poste & horaires ---
        p.setTypePersonnel(rs.getString("typePersonnel"));
        p.setPosteOuFonction(rs.getString("posteOuFonction"));
        p.setQualification(rs.getString("qualification"));
        p.setStatutContractuel(rs.getString("statutContractuel"));
        p.setQuotaHeuresHebdo(rs.getBigDecimal("quotaHeuresHebdo"));
        p.setDechargeHeures(rs.getBigDecimal("dechargeHeures"));
        Date dateEmbauche = rs.getDate("dateEmbauche");
        p.setDateEmbauche(dateEmbauche != null ? dateEmbauche.toLocalDate() : null);
        Date dateDepart = rs.getDate("dateDepart");
        p.setDateDepart(dateDepart != null ? dateDepart.toLocalDate() : null);
        p.setMotifDepart(rs.getString("motifDepart"));
        p.setStatutEmploi(rs.getString("statutEmploi"));

        // --- Paie ---
        p.setModePaiement(rs.getString("modePaiement"));
        p.setNomBanque(rs.getString("nomBanque"));
        p.setIbanOuRib(rs.getString("ibanOuRib"));
        p.setNumeroMobileMoney(rs.getString("numeroMobileMoney"));

        // --- État ---
        String statutEmploi = rs.getString("statutEmploi");
        p.setStatutEmploi(statutEmploi);
        p.setEstActif("ACTIF".equals(statutEmploi)); // Si votre modèle possède ce setter

        // --- Champs techniques hérités de BaseEntity ---
        p.setVersion(rs.getLong("version"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (createdAt != null) p.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());
        if (deletedAt != null) p.setDeletedAt(deletedAt.toLocalDateTime());

        return p;
    }

    // ==================================================================
    // UTILITAIRES DE MAPPING JDBC
    // ==================================================================

    /**
     * setString, mais écrit NULL SQL si la valeur est null ou blanche.
     * Évite d'insérer la chaîne vide "" là où la base attend NULL.
     */
    private static void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, value);
        }
    }

    /**
     * setDate, mais écrit NULL SQL si la valeur est null.
     * Convertit LocalDate -> java.sql.Date.
     */
    private static void setNullableDate(PreparedStatement ps, int idx, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.DATE);
        } else {
            ps.setDate(idx, Date.valueOf(value));
        }
    }

    /**
     * setBigDecimal, mais écrit NULL SQL si la valeur est null.
     * Utile pour quotaHeuresHebdo qui est nullable en base.
     */
    private static void setNullableDecimal(PreparedStatement ps, int idx, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.DECIMAL);
        } else {
            ps.setBigDecimal(idx, value);
        }
    }


    /**
     * Recherche paginée pour la liste du personnel.
     * Fait une jointure avec Departement pour enrichir le nom.
     */
    public ResultatPagine<PersonnelLigne> rechercherPagine(FiltrePersonnel filtre) {
        StringBuilder clause = new StringBuilder("""
        FROM Personnel p
        LEFT JOIN Departement d ON d.idDepartement = p.idDepartement AND d.deleted_at IS NULL
        WHERE p.deleted_at IS NULL
          AND p.idEcole = ?
        """);

        List<Object> parametres = new ArrayList<>();
        parametres.add(filtre.idEcole());

        if (filtre.idDepartement() != null && !filtre.idDepartement().isBlank()) {
            clause.append(" AND p.idDepartement = ? ");
            parametres.add(filtre.idDepartement());
        }
        if (filtre.typePersonnel() != null && !filtre.typePersonnel().isBlank()) {
            clause.append(" AND p.typePersonnel = ? ");
            parametres.add(filtre.typePersonnel());
        }
        if (filtre.statutEmploi() != null) {
            clause.append(" AND p.statutEmploi = ? ");
            parametres.add(filtre.statutEmploi().name()); // Assurez-vous de passer le nom de l'enum
        }
        if (filtre.terme() != null && !filtre.terme().isBlank()) {
            clause.append("""
             AND (p.nom LIKE ? OR p.prenom LIKE ? OR p.matriculeInterne LIKE ?
                  OR p.email LIKE ? OR p.telephone LIKE ?)
            """);
            String motif = "%" + filtre.terme().trim() + "%";
            for (int i = 0; i < 5; i++) parametres.add(motif);
        }

        long total = compterPersonnel(clause.toString(), parametres);

        // CORRECTION : Remplacement de p.estActif par p.statutEmploi
        String sqlPage = """
        SELECT p.idPersonnel, p.matriculeInterne, p.nom, p.prenom,
               p.posteOuFonction, p.statutEmploi, p.photo, p.email, p.telephone,
               d.libelle AS nomDepartement
        """ + clause + " ORDER BY p.nom ASC, p.prenom ASC LIMIT ? OFFSET ?";

        List<PersonnelLigne> lignes = new ArrayList<>();

        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sqlPage)) {

            int i = 1;
            for (Object param : parametres) ps.setObject(i++, param);
            ps.setInt(i++, filtre.taillePage());
            ps.setInt(i, filtre.offset());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lignes.add(mapToLigne(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur rechercherPagine Personnel : " + e.getMessage());
        }

        return new ResultatPagine<>(lignes, total, filtre.page(), filtre.taillePage());
    }
//    public ResultatPagine<PersonnelLigne> rechercherPagine(FiltrePersonnel filtre) {
//        StringBuilder clause = new StringBuilder("""
//        FROM Personnel p
//        LEFT JOIN Departement d ON d.idDepartement = p.idDepartement AND d.deleted_at IS NULL
//        WHERE p.deleted_at IS NULL
//          AND p.idEcole = ?
//        """);
//
//        List<Object> parametres = new ArrayList<>();
//        parametres.add(filtre.idEcole());
//
//        if (filtre.idDepartement() != null && !filtre.idDepartement().isBlank()) {
//            clause.append(" AND p.idDepartement = ? ");
//            parametres.add(filtre.idDepartement());
//        }
//        if (filtre.typePersonnel() != null && !filtre.typePersonnel().isBlank()) {
//            clause.append(" AND p.typePersonnel = ? ");
//            parametres.add(filtre.typePersonnel());
//        }
//        if (filtre.statutEmploi() != null) {
//            clause.append(" AND p.statutEmploi = ? ");
//            parametres.add(filtre.statutEmploi());
//        }
//        if (filtre.terme() != null && !filtre.terme().isBlank()) {
//            clause.append("""
//             AND (p.nom LIKE ? OR p.prenom LIKE ? OR p.matriculeInterne LIKE ?
//                  OR p.email LIKE ? OR p.telephone LIKE ?)
//            """);
//            String motif = "%" + filtre.terme().trim() + "%";
//            for (int i = 0; i < 5; i++) parametres.add(motif);
//        }
//
//        long total = compterPersonnel(clause.toString(), parametres);
//
//        String sqlPage = """
//        SELECT p.idPersonnel, p.matriculeInterne, p.nom, p.prenom,
//               p.posteOuFonction, p.statutEmploi, p.estActif, p.photo,
//               d.libelle AS nomDepartement
//        """ + clause + " ORDER BY p.nom ASC, p.prenom ASC LIMIT ? OFFSET ?";
//
//        List<PersonnelLigne> lignes = new ArrayList<>();
//
//        try (Connection conn = Database.getConnexion();
//             PreparedStatement ps = conn.prepareStatement(sqlPage)) {
//
//            int i = 1;
//            for (Object param : parametres) ps.setObject(i++, param);
//            ps.setInt(i++, filtre.taillePage());
//            ps.setInt(i, filtre.offset());
//
//            try (ResultSet rs = ps.executeQuery()) {
//                while (rs.next()) lignes.add(mapToLigne(rs));
//            }
//        } catch (SQLException e) {
//            System.err.println("Erreur rechercherPagine Personnel : " + e.getMessage());
//        }
//
//        return new ResultatPagine<>(lignes, total, filtre.page(), filtre.taillePage());
//    }

    private long compterPersonnel(String clauseWhere, List<Object> parametres) {
        String sql = "SELECT COUNT(*) AS total " + clauseWhere;
        try (Connection conn = Database.getConnexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object param : parametres) ps.setObject(i++, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total");
            }
        } catch (SQLException e) {
            System.err.println("Erreur comptage personnel : " + e.getMessage());
        }
        return 0;
    }

    private PersonnelLigne mapToLigne(ResultSet rs) throws SQLException {
        String nom = rs.getString("nom");
        String prenom = rs.getString("prenom");
        String nomComplet = (prenom != null && !prenom.isBlank())
                ? nom + " " + prenom : nom;

        String statutEmploi = rs.getString("statutEmploi");
        boolean estActif = "actif".equals(statutEmploi);

        StringBuilder init = new StringBuilder();
        if (nom != null && !nom.isBlank()) init.append(Character.toUpperCase(nom.charAt(0)));
        if (prenom != null && !prenom.isBlank()) init.append(Character.toUpperCase(prenom.charAt(0)));

        return new PersonnelLigne(
                rs.getString("idPersonnel"),         // 1  idPersonnel
                rs.getString("matriculeInterne"),    // 2  matricule
                nom,                                 // 3  nom          ← AJOUTÉ
                prenom,                              // 4  prenom       ← AJOUTÉ
                nomComplet,                          // 5  nomComplet
                init.toString(),                     // 6  initiales
                rs.getString("posteOuFonction"),     // 7  poste
                rs.getString("nomDepartement"),      // 8  nomDepartement
                rs.getString("email"),               // 9  email
                rs.getString("telephone"),           // 10 telephone
                rs.getString("photo"),               // 11 photoPath
                rs.getString("statutEmploi"),
                estActif// 12 statutEmploi
        );
    }
}