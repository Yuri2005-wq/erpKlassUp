package org.erpklassup.erpklassup.sync;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * EntityApplier — équivalent local du SYNC_REGISTRY Django : associe un
 * nomTable à la logique d'UPSERT locale, pour que SyncOrchestrator reste
 * générique et ne connaisse aucune table métier par son nom en dur.
 */
public final class EntityApplier {

    @FunctionalInterface
    public interface TableApplier {
        void apply(Connection cx, String idEntity, String action, JsonNode payload, long version) throws SQLException;
    }

    private static final Map<String, TableApplier> REGISTRY = new HashMap<>();

    public static void register(String nomTable, TableApplier applier) {
        REGISTRY.put(nomTable, applier);
    }

    public static void apply(Connection cx, JsonNode change) throws SQLException {
        String nomTable = change.get("nomTable").asText();
        TableApplier applier = REGISTRY.get(nomTable);
        if (applier == null) {
            throw new SQLException("Aucun applier local enregistré pour la table '" + nomTable + "'");
        }

        JsonNode payload = change.get("payloadJSON");
        if (payload == null || payload.isNull()) {
            payload = change.get("donneesJSON");
        }

        applier.apply(
                cx,
                change.get("idEntity").asText(),
                change.get("actionEnum").asText(),
                payload,
                change.get("version").asLong()
        );
    }

    /**
     * Initialise et enregistre automatiquement les appliers pour le module Auth/RBAC.
     */
    public static void initDefaults() {

        // 1. GroupeEcole
        register("GroupeEcole", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "GroupeEcole", "idGroupe", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO GroupeEcole (idGroupe, nomGroupe, nomPromoteur, contactPromoteur, emailPromoteur,
                                         siegeSocial, logoGroupe, is_active, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    nomGroupe = VALUES(nomGroupe), nomPromoteur = VALUES(nomPromoteur),
                    contactPromoteur = VALUES(contactPromoteur), emailPromoteur = VALUES(emailPromoteur),
                    siegeSocial = VALUES(siegeSocial), logoGroupe = VALUES(logoGroupe),
                    is_active = VALUES(is_active), version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "nomGroupe"));
                ps.setString(3, getTextOrNull(payload, "nomPromoteur"));
                ps.setString(4, getTextOrNull(payload, "contactPromoteur"));
                ps.setString(5, getTextOrNull(payload, "emailPromoteur"));
                ps.setString(6, getTextOrNull(payload, "siegeSocial"));
                ps.setString(7, getTextOrNull(payload, "logoGroupe"));
                ps.setBoolean(8, payload.path("is_active").asBoolean(true));
                ps.setLong(9, version);
                ps.executeUpdate();
            }
        });

        // 2. Ecole
        register("Ecole", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "Ecole", "idEcole", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO Ecole (idEcole, idGroupe, codeEcole, nomEcole, numeroAgrement, numeroArreteOuverture,
                                   nomPromoteur, inspectionRef, delegationDept, region, sousSysteme, niveauEnseignement,
                                   adresse, telephone, email, devise, logoPath, cleSecrete, is_active, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idGroupe = VALUES(idGroupe), codeEcole = VALUES(codeEcole), nomEcole = VALUES(nomEcole),
                    numeroAgrement = VALUES(numeroAgrement), numeroArreteOuverture = VALUES(numeroArreteOuverture),
                    nomPromoteur = VALUES(nomPromoteur), inspectionRef = VALUES(inspectionRef),
                    delegationDept = VALUES(delegationDept), region = VALUES(region), sousSysteme = VALUES(sousSysteme),
                    niveauEnseignement = VALUES(niveauEnseignement), adresse = VALUES(adresse), telephone = VALUES(telephone),
                    email = VALUES(email), devise = VALUES(devise), logoPath = VALUES(logoPath), cleSecrete = VALUES(cleSecrete),
                    is_active = VALUES(is_active), version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idGroupe"));
                ps.setString(3, getTextOrNull(payload, "codeEcole"));
                ps.setString(4, getTextOrNull(payload, "nomEcole"));
                ps.setString(5, getTextOrNull(payload, "numeroAgrement"));
                ps.setString(6, getTextOrNull(payload, "numeroArreteOuverture"));
                ps.setString(7, getTextOrNull(payload, "nomPromoteur"));
                ps.setString(8, getTextOrNull(payload, "inspectionRef"));
                ps.setString(9, getTextOrNull(payload, "delegationDept"));
                ps.setString(10, getTextOrNull(payload, "region"));
                ps.setString(11, getTextOrNull(payload, "sousSysteme"));
                ps.setString(12, getTextOrNull(payload, "niveauEnseignement"));
                ps.setString(13, getTextOrNull(payload, "adresse"));
                ps.setString(14, getTextOrNull(payload, "telephone"));
                ps.setString(15, getTextOrNull(payload, "email"));
                ps.setString(16, getTextOrNull(payload, "devise"));
                ps.setString(17, getTextOrNull(payload, "logoPath"));
                ps.setString(18, getTextOrNull(payload, "cleSecrete"));
                ps.setBoolean(19, payload.path("is_active").asBoolean(true));
                ps.setLong(20, version);
                ps.executeUpdate();
            }
        });

        // 3. Permission
        register("Permission", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "Permission", "idPermission", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO Permission (idPermission, codePermission, libelle, categorie, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    codePermission = VALUES(codePermission), libelle = VALUES(libelle),
                    categorie = VALUES(categorie), version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "codePermission"));
                ps.setString(3, getTextOrNull(payload, "libelle"));
                ps.setString(4, getTextOrNull(payload, "categorie"));
                ps.setLong(5, version);
                ps.executeUpdate();
            }
        });

        // 4. Role
        register("Role", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "Role", "idRole", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO Role (idRole, idEcole, idGroupe, nomRole, description, is_active, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idEcole = VALUES(idEcole), idGroupe = VALUES(idGroupe), nomRole = VALUES(nomRole),
                    description = VALUES(description), is_active = VALUES(is_active),
                    version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idEcole"));
                ps.setString(3, getTextOrNull(payload, "idGroupe"));
                ps.setString(4, getTextOrNull(payload, "nomRole"));
                ps.setString(5, getTextOrNull(payload, "description"));
                ps.setBoolean(6, payload.path("is_active").asBoolean(true));
                ps.setLong(7, version);
                ps.executeUpdate();
            }
        });

        // 5. RolePermission
        register("RolePermission", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "RolePermission", "idRolePermission", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO RolePermission (idRolePermission, idRole, idPermission, idEcole, dateAttribution,
                                            attribuePar, dateDebut, dateFin, estActive, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idRole = VALUES(idRole), idPermission = VALUES(idPermission), idEcole = VALUES(idEcole),
                    dateAttribution = VALUES(dateAttribution), attribuePar = VALUES(attribuePar),
                    dateDebut = VALUES(dateDebut), dateFin = VALUES(dateFin), estActive = VALUES(estActive),
                    version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idRole"));
                ps.setString(3, getTextOrNull(payload, "idPermission"));
                ps.setString(4, getTextOrNull(payload, "idEcole"));
                setTimestampOrNow(ps, 5, payload, "dateAttribution");
                ps.setString(6, getTextOrNull(payload, "attribuePar"));
                setTimestampOrNull(ps, 7, payload, "dateDebut");
                setTimestampOrNull(ps, 8, payload, "dateFin");
                ps.setBoolean(9, payload.path("estActive").asBoolean(true));
                ps.setLong(10, version);
                ps.executeUpdate();
            }
        });

        // 6. ActionPermission
        register("ActionPermission", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "ActionPermission", "idActionPermission", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO ActionPermission (idActionPermission, codeAction, libelleAction, idPermission,
                                               module, estActive, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    codeAction = VALUES(codeAction), libelleAction = VALUES(libelleAction),
                    idPermission = VALUES(idPermission), module = VALUES(module),
                    estActive = VALUES(estActive), version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "codeAction"));
                ps.setString(3, getTextOrNull(payload, "libelleAction"));
                ps.setString(4, getTextOrNull(payload, "idPermission"));
                ps.setString(5, getTextOrNull(payload, "module"));
                ps.setBoolean(6, payload.path("estActive").asBoolean(true));
                ps.setLong(7, version);
                ps.executeUpdate();
            }
        });

        // 7. RoleHeritage
        register("RoleHeritage", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "RoleHeritage", "idRoleHeritage", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO RoleHeritage (idRoleHeritage, idRoleParent, idRoleEnfant, idEcole,
                                           dateCreation, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idRoleParent = VALUES(idRoleParent), idRoleEnfant = VALUES(idRoleEnfant),
                    idEcole = VALUES(idEcole), dateCreation = VALUES(dateCreation),
                    version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idRoleParent"));
                ps.setString(3, getTextOrNull(payload, "idRoleEnfant"));
                ps.setString(4, getTextOrNull(payload, "idEcole"));
                setTimestampOrNow(ps, 5, payload, "dateCreation");
                ps.setLong(6, version);
                ps.executeUpdate();
            }
        });

        // 8. Utilisateur
        register("Utilisateur", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "Utilisateur", "idUtilisateur", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO Utilisateur (idUtilisateur, idEcole, idGroupePrincipal, username, passwordHash, nom, prenom,
                                         email, telephone, emailVerifie, hashTokenVerificationEmail, dateVerificationEmail,
                                         dateChangementMotDePasse, doitChangerMotDePasse, languePreference, fuseauHoraire,
                                         secret2FA, telephone2FA, deuxFacteursActive, typeUtilisateur, compteVerrouille,
                                         verrouilleJusqua, nombreTentativesEchec, dateDernierEchec, estActif, doitConfigurer2FA,
                                         version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idEcole = VALUES(idEcole), idGroupePrincipal = VALUES(idGroupePrincipal),
                    username = VALUES(username), passwordHash = VALUES(passwordHash), nom = VALUES(nom),
                    prenom = VALUES(prenom), email = VALUES(email), telephone = VALUES(telephone),
                    emailVerifie = VALUES(emailVerifie), hashTokenVerificationEmail = VALUES(hashTokenVerificationEmail),
                    dateVerificationEmail = VALUES(dateVerificationEmail), dateChangementMotDePasse = VALUES(dateChangementMotDePasse),
                    doitChangerMotDePasse = VALUES(doitChangerMotDePasse), languePreference = VALUES(languePreference),
                    fuseauHoraire = VALUES(fuseauHoraire), secret2FA = VALUES(secret2FA), telephone2FA = VALUES(telephone2FA),
                    deuxFacteursActive = VALUES(deuxFacteursActive), typeUtilisateur = VALUES(typeUtilisateur),
                    compteVerrouille = VALUES(compteVerrouille), verrouilleJusqua = VALUES(verrouilleJusqua),
                    nombreTentativesEchec = VALUES(nombreTentativesEchec), dateDernierEchec = VALUES(dateDernierEchec),
                    estActif = VALUES(estActif), doitConfigurer2FA = VALUES(doitConfigurer2FA),
                    version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idEcole"));
                ps.setString(3, getTextOrNull(payload, "idGroupePrincipal"));
                ps.setString(4, getTextOrNull(payload, "username"));
                ps.setString(5, getTextOrNull(payload, "passwordHash"));
                ps.setString(6, getTextOrNull(payload, "nom"));
                ps.setString(7, getTextOrNull(payload, "prenom"));
                ps.setString(8, getTextOrNull(payload, "email"));
                ps.setString(9, getTextOrNull(payload, "telephone"));
                ps.setBoolean(10, payload.path("emailVerifie").asBoolean(false));
                ps.setString(11, getTextOrNull(payload, "hashTokenVerificationEmail"));
                setTimestampOrNull(ps, 12, payload, "dateVerificationEmail");
                setTimestampOrNow(ps, 13, payload, "dateChangementMotDePasse");
                ps.setBoolean(14, payload.path("doitChangerMotDePasse").asBoolean(false));
                ps.setString(15, payload.path("languePreference").asText("FR"));
                ps.setString(16, payload.path("fuseauHoraire").asText("Africa/Douala"));
                ps.setString(17, getTextOrNull(payload, "secret2FA"));
                ps.setString(18, getTextOrNull(payload, "telephone2FA"));
                ps.setBoolean(19, payload.path("deuxFacteursActive").asBoolean(false));
                ps.setString(20, payload.path("typeUtilisateur").asText("PERSONNEL"));
                ps.setBoolean(21, payload.path("compteVerrouille").asBoolean(false));
                setTimestampOrNull(ps, 22, payload, "verrouilleJusqua");
                ps.setInt(23, payload.path("nombreTentativesEchec").asInt(0));
                setTimestampOrNull(ps, 24, payload, "dateDernierEchec");
                ps.setBoolean(25, payload.path("estActif").asBoolean(true));
                ps.setBoolean(26, payload.path("doitConfigurer2FA").asBoolean(false));
                ps.setLong(27, version);
                ps.executeUpdate();
            }
        });

        // 9. UtilisateurRole
        register("UtilisateurRole", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "UtilisateurRole", "idUtilisateurRole", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO UtilisateurRole (idUtilisateurRole, idUtilisateur, idRole, idEcole, dateAttribution,
                                             attribuePar, dateDebut, dateFin, estActive, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idUtilisateur = VALUES(idUtilisateur), idRole = VALUES(idRole), idEcole = VALUES(idEcole),
                    dateAttribution = VALUES(dateAttribution), attribuePar = VALUES(attribuePar),
                    dateDebut = VALUES(dateDebut), dateFin = VALUES(dateFin), estActive = VALUES(estActive),
                    version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idUtilisateur"));
                ps.setString(3, getTextOrNull(payload, "idRole"));
                ps.setString(4, getTextOrNull(payload, "idEcole"));
                setTimestampOrNow(ps, 5, payload, "dateAttribution");
                ps.setString(6, getTextOrNull(payload, "attribuePar"));
                setTimestampOrNull(ps, 7, payload, "dateDebut");
                setTimestampOrNull(ps, 8, payload, "dateFin");
                ps.setBoolean(9, payload.path("estActive").asBoolean(true));
                ps.setLong(10, version);
                ps.executeUpdate();
            }
        });

        // 10. UtilisateurPermission
        register("UtilisateurPermission", (cx, idEntity, action, payload, version) -> {
            if ("DELETE".equalsIgnoreCase(action)) {
                executeSoftDelete(cx, "UtilisateurPermission", "idUtilisateurPermission", idEntity, version);
                return;
            }
            String sql = """
                INSERT INTO UtilisateurPermission (idUtilisateurPermission, idUtilisateur, idPermission, dateAttribution,
                                                   attribuePar, dateDebut, dateFin, motifAttribution, estActive, version, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                ON DUPLICATE KEY UPDATE
                    idUtilisateur = VALUES(idUtilisateur), idPermission = VALUES(idPermission),
                    dateAttribution = VALUES(dateAttribution), attribuePar = VALUES(attribuePar),
                    dateDebut = VALUES(dateDebut), dateFin = VALUES(dateFin), motifAttribution = VALUES(motifAttribution),
                    estActive = VALUES(estActive), version = VALUES(version), deleted_at = NULL
                """;
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, idEntity);
                ps.setString(2, getTextOrNull(payload, "idUtilisateur"));
                ps.setString(3, getTextOrNull(payload, "idPermission"));
                setTimestampOrNow(ps, 4, payload, "dateAttribution");
                ps.setString(5, getTextOrNull(payload, "attribuePar"));
                setTimestampOrNull(ps, 6, payload, "dateDebut");
                setTimestampOrNull(ps, 7, payload, "dateFin");
                ps.setString(8, getTextOrNull(payload, "motifAttribution"));
                ps.setBoolean(9, payload.path("estActive").asBoolean(true));
                ps.setLong(10, version);
                ps.executeUpdate();
            }
        });
    }

    // --- Helpers de gestion SQL ---

    private static void executeSoftDelete(Connection cx, String tableName, String pkColumn, String idEntity, long version) throws SQLException {
        String sql = String.format("UPDATE %s SET deleted_at = ?, version = ? WHERE %s = ?", tableName, pkColumn);
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setLong(2, version);
            ps.setString(3, idEntity);
            ps.executeUpdate();
        }
    }

    private static String getTextOrNull(JsonNode payload, String fieldName) {
        if (payload == null) return null;
        JsonNode node = payload.get(fieldName);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    private static void setTimestampOrNull(PreparedStatement ps, int paramIndex, JsonNode payload, String fieldName) throws SQLException {
        String value = getTextOrNull(payload, fieldName);
        if (value == null || value.isBlank()) {
            ps.setNull(paramIndex, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(paramIndex, Timestamp.valueOf(value.replace("T", " ").replace("Z", "")));
        }
    }

    private static void setTimestampOrNow(PreparedStatement ps, int paramIndex, JsonNode payload, String fieldName) throws SQLException {
        String value = getTextOrNull(payload, fieldName);
        if (value == null || value.isBlank()) {
            ps.setTimestamp(paramIndex, Timestamp.from(Instant.now()));
        } else {
            ps.setTimestamp(paramIndex, Timestamp.valueOf(value.replace("T", " ").replace("Z", "")));
        }
    }

    private EntityApplier() {}
}