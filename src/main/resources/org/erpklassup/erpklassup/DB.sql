-- ==============================================================
-- BASE DE DONNÉES ERP SCOLAIRE - VERSION FINALE COMPLÈTE
-- Compatible avec synchronisation Offline-First (JavaFX <-> Cloud)
-- Engine : InnoDB | Encodage : utf8mb4_unicode_ci
--
-- ✅ Unicité "soft-delete aware" : chaque contrainte UNIQUE sensible au
-- soft-delete repose sur une colonne générée VIRTUAL qui ne porte la
-- valeur métier que lorsque deleted_at IS NULL, et vaut NULL sinon.
-- Deux lignes NULL n'entrent jamais en conflit dans un index UNIQUE :
-- une ligne archivée ne bloque donc plus la recréation d'un même
-- code/matricule/username actif.
-- ==============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================================
-- 0. GROUPE D'ÉCOLES (RÉSEAU DU PROMOTEUR)
-- ==============================================================
CREATE TABLE IF NOT EXISTS GroupeEcole (
                                           idGroupe VARCHAR(50) NOT NULL,
    nomGroupe VARCHAR(150) NOT NULL,
    nomPromoteur VARCHAR(150),
    contactPromoteur VARCHAR(30),
    emailPromoteur VARCHAR(150),
    siegeSocial VARCHAR(150),
    logoGroupe VARCHAR(255),
    is_active TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idGroupe),
    INDEX idx_groupe_sync (updated_at, version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 1. ÉCOLES (MULTI-TENANT)
-- ==============================================================
CREATE TABLE IF NOT EXISTS Ecole (
                                     idEcole VARCHAR(50) NOT NULL,
    idGroupe VARCHAR(50) NULL,
    codeEcole VARCHAR(20) NOT NULL,
    nomEcole VARCHAR(150) NOT NULL,
    numeroAgrement VARCHAR(50),
    numeroArreteOuverture VARCHAR(100),
    nomPromoteur VARCHAR(100),
    inspectionRef VARCHAR(100),
    delegationDept VARCHAR(100),
    region VARCHAR(50),
    sousSysteme VARCHAR(150),
    niveauEnseignement VARCHAR(150),
    adresse TEXT,
    telephone VARCHAR(30),
    email VARCHAR(100),
    devise VARCHAR(150),
    logoPath VARCHAR(255),
    cleSecrete VARCHAR(64) NULL,
    is_active TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codeEcole_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeEcole END) VIRTUAL,
    PRIMARY KEY (idEcole),
    UNIQUE KEY uq_ecole_code_actif (codeEcole_actif),
    INDEX idx_ecole_sync (idEcole, updated_at, version),
    INDEX idx_ecole_groupe (idGroupe),
    CONSTRAINT fk_ecole_groupe FOREIGN KEY (idGroupe) REFERENCES GroupeEcole(idGroupe) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 2. RBAC & UTILISATEURS (SYNCHRONISÉES)
-- ==============================================================
CREATE TABLE IF NOT EXISTS Permission (
                                          idPermission VARCHAR(50) NOT NULL,
    codePermission VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    categorie VARCHAR(50),
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codePermission_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codePermission END) VIRTUAL,
    PRIMARY KEY (idPermission),
    UNIQUE KEY uq_code_permission_actif (codePermission_actif),
    INDEX idx_permission_sync (updated_at, version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Role (
                                    idRole VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idGroupe VARCHAR(50) NULL,
    nomRole VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    nomRole_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nomRole END) VIRTUAL,
    PRIMARY KEY (idRole),
    UNIQUE KEY uq_role_ecole_actif (nomRole_actif, idEcole),
    INDEX idx_role_sync (idEcole, updated_at, version),
    CONSTRAINT fk_role_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_role_groupe FOREIGN KEY (idGroupe) REFERENCES GroupeEcole(idGroupe) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS RolePermission (
    idRolePermission VARCHAR(50) NOT NULL,
    idRole VARCHAR(50) NOT NULL,
    idPermission VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    dateAttribution TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    attribuePar VARCHAR(50) NULL,
    dateDebut DATETIME NULL,
    dateFin DATETIME NULL,
    estActive TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idPermission_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idPermission END) VIRTUAL,
    PRIMARY KEY (idRolePermission),
    UNIQUE KEY uq_role_permission_actif (idRole, idPermission_actif),
    INDEX idx_roleperm_sync (idEcole, updated_at, version),
    CONSTRAINT fk_rp_role FOREIGN KEY (idRole) REFERENCES Role(idRole) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (idPermission) REFERENCES Permission(idPermission) ON DELETE CASCADE,
    CONSTRAINT fk_rp_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ActionPermission (
                                                idActionPermission VARCHAR(50) NOT NULL,
    codeAction VARCHAR(100) NOT NULL,
    libelleAction VARCHAR(150) NOT NULL,
    idPermission VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    estActive TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codeAction_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeAction END) VIRTUAL,
    PRIMARY KEY (idActionPermission),
    UNIQUE KEY uq_action_code_actif (codeAction_actif),
    INDEX idx_ap_sync (updated_at, version),
    CONSTRAINT fk_ap_perm FOREIGN KEY (idPermission) REFERENCES Permission(idPermission) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS RoleHeritage (
                                            idRoleHeritage VARCHAR(50) NOT NULL,
    idRoleParent VARCHAR(50) NOT NULL,
    idRoleEnfant VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NULL,
    dateCreation TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idRoleEnfant_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idRoleEnfant END) VIRTUAL,
    PRIMARY KEY (idRoleHeritage),
    UNIQUE KEY uq_role_heritage_actif (idRoleParent, idRoleEnfant_actif),
    INDEX idx_rh_sync (idEcole, updated_at),
    CONSTRAINT fk_rh_parent FOREIGN KEY (idRoleParent) REFERENCES Role(idRole) ON DELETE CASCADE,
    CONSTRAINT fk_rh_enfant FOREIGN KEY (idRoleEnfant) REFERENCES Role(idRole) ON DELETE CASCADE,
    CONSTRAINT fk_rh_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Utilisateur (
                                           idUtilisateur VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idGroupePrincipal VARCHAR(50) NULL,
    username VARCHAR(50) NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50),
    email VARCHAR(100),
    telephone VARCHAR(30),
    emailVerifie TINYINT(1) DEFAULT 0,
    hashTokenVerificationEmail VARCHAR(64) NULL,
    dateVerificationEmail DATETIME NULL,
    dateChangementMotDePasse DATETIME DEFAULT CURRENT_TIMESTAMP,
    doitChangerMotDePasse TINYINT(1) DEFAULT 0,
    languePreference VARCHAR(10) DEFAULT 'FR',
    fuseauHoraire VARCHAR(50) DEFAULT 'Africa/Douala',
    secret2FA VARCHAR(255) NULL,
    telephone2FA VARCHAR(30) NULL,
    deuxFacteursActive TINYINT(1) DEFAULT 0,
    typeUtilisateur VARCHAR(50) DEFAULT 'PERSONNEL',
    compteVerrouille TINYINT(1) DEFAULT 0,
    verrouilleJusqua DATETIME NULL,
    nombreTentativesEchec INT DEFAULT 0,
    dateDernierEchec DATETIME NULL,
    estActif TINYINT(1) DEFAULT 1,
    doitConfigurer2FA TINYINT(1) DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    username_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN username END) VIRTUAL,
    email_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN email END) VIRTUAL,
    PRIMARY KEY (idUtilisateur),
    UNIQUE KEY uq_user_ecole_actif (username_actif, idEcole),
    UNIQUE KEY uq_user_email_actif (email_actif),
    INDEX idx_user_sync (idEcole, updated_at, version),
    INDEX idx_user_email (email),
    CONSTRAINT fk_user_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_user_groupe FOREIGN KEY (idGroupePrincipal) REFERENCES GroupeEcole(idGroupe) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS UtilisateurRole (
                                               idUtilisateurRole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    idRole VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NULL,
    dateAttribution TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    attribuePar VARCHAR(50) NULL,
    dateDebut DATETIME NULL,
    dateFin DATETIME NULL,
    estActive TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idRole_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idRole END) VIRTUAL,
    PRIMARY KEY (idUtilisateurRole),
    UNIQUE KEY uq_user_role_actif (idUtilisateur, idRole_actif),
    INDEX idx_ur_sync (idEcole, updated_at, version),
    CONSTRAINT fk_ur_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (idRole) REFERENCES Role(idRole) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS UtilisateurPermission (
                                                     idUtilisateurPermission VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    idPermission VARCHAR(50) NOT NULL,
    dateAttribution DATETIME DEFAULT CURRENT_TIMESTAMP,
    attribuePar VARCHAR(50) NULL,
    dateDebut DATETIME NULL,
    dateFin DATETIME NULL,
    motifAttribution VARCHAR(255),
    estActive TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idPermission_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idPermission END) VIRTUAL,
    PRIMARY KEY (idUtilisateurPermission),
    UNIQUE KEY uq_user_perm_actif (idUtilisateur, idPermission_actif),
    INDEX idx_up_sync (updated_at, version),
    CONSTRAINT fk_up_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE CASCADE,
    CONSTRAINT fk_up_perm FOREIGN KEY (idPermission) REFERENCES Permission(idPermission) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 3. TABLES LOCALES DU MODULE AUTHENTIFICATION (NON SYNCHRONISÉES)
-- ==============================================================

CREATE TABLE IF NOT EXISTS TokenReinitialisation (
                                                     idToken VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    tokenHash VARCHAR(64) NOT NULL UNIQUE,
    dateCreation DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateExpiration DATETIME NOT NULL,
    estUtilise TINYINT(1) DEFAULT 0,
    dateUtilisation DATETIME NULL,
    adresseIpDemande VARCHAR(45),
    PRIMARY KEY (idToken),
    INDEX idx_reset_token (tokenHash),
    INDEX idx_reset_user (idUtilisateur),
    CONSTRAINT fk_reset_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SessionUtilisateur (
                                                  idSession VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    idEcoleActive VARCHAR(50) NULL,
    idGroupeActif VARCHAR(50) NULL,
    accessTokenHash VARCHAR(64) NULL,
    refreshTokenHash VARCHAR(64) NOT NULL UNIQUE,
    familleToken VARCHAR(50) NOT NULL,
    typeSession VARCHAR(50),
    adresseIp VARCHAR(45),
    userAgent VARCHAR(255),
    nomAppareil VARCHAR(100),
    typeAppareil VARCHAR(20) DEFAULT 'DESKTOP',
    systemeExploitation VARCHAR(50),
    navigateur VARCHAR(100),
    localisation VARCHAR(150),
    dateCreation DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateDerniereActivite DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateExpirationAccess DATETIME NOT NULL,
    dateExpirationRefresh DATETIME NOT NULL,
    dateDeconnexion DATETIME NULL,
    motifDeconnexion VARCHAR(30) NULL,
    estRevoque TINYINT(1) DEFAULT 0,
    PRIMARY KEY (idSession),
    INDEX idx_sess_refresh (refreshTokenHash),
    INDEX idx_sess_user (idUtilisateur),
    CONSTRAINT fk_sess_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE CASCADE,
    CONSTRAINT fk_sess_ecole FOREIGN KEY (idEcoleActive) REFERENCES Ecole(idEcole) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Code2FA (
                                       idCode VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    codeHash VARCHAR(64) NOT NULL,
    typeCode VARCHAR(20) NOT NULL,
    dateCreation DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateExpiration DATETIME NOT NULL,
    estUtilise TINYINT(1) DEFAULT 0,
    PRIMARY KEY (idCode),
    INDEX idx_c2fa_user (idUtilisateur),
    CONSTRAINT fk_c2fa_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS CodeSecours2FA (
                                              idCodeSecours VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    codeHash VARCHAR(64) NOT NULL,
    estUtilise TINYINT(1) DEFAULT 0,
    dateUtilisation DATETIME NULL,
    PRIMARY KEY (idCodeSecours),
    INDEX idx_secours_user (idUtilisateur),
    CONSTRAINT fk_secours_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS TentativeConnexion (
                                                  idTentative VARCHAR(50) NOT NULL,
    usernameSaisi VARCHAR(100) NOT NULL,
    adresseIp VARCHAR(45) NOT NULL,
    userAgent VARCHAR(255),
    succes TINYINT(1) NOT NULL,
    motifEchec VARCHAR(30) NULL,
    dateTentative DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idTentative),
    INDEX idx_tentative_date (dateTentative),
    INDEX idx_tentative_username (usernameSaisi)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 4. PERSONNEL (SYNCHRONISÉE - REMPLACE ENSEIGNANT)
-- ==============================================================
CREATE TABLE IF NOT EXISTS Personnel (
                                         idPersonnel VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NULL UNIQUE,
    matriculeInterne VARCHAR(50) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    sexe VARCHAR(20),
    dateNaissance DATE,
    telephone VARCHAR(30),
    contactUrgence VARCHAR(30),
    email VARCHAR(150),
    cniOuNiu VARCHAR(50),
    typePersonnel VARCHAR(50) NOT NULL,
    posteOuFonction VARCHAR(100) NOT NULL,
    qualification VARCHAR(100),
    statutContractuel VARCHAR(50) DEFAULT 'PERMANENT',
    dateEmbauche DATE,
    photo VARCHAR(255),
    estActif TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    matriculeInterne_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN matriculeInterne END) VIRTUAL,
    PRIMARY KEY (idPersonnel),
    UNIQUE KEY uq_personnel_matricule_actif (matriculeInterne_actif, idEcole),
    INDEX idx_pers_sync (idEcole, updated_at, version),
    CONSTRAINT fk_pers_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_pers_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 5. STRUCTURE ACADÉMIQUE (SYNCHRONISÉE)
-- ==============================================================
CREATE TABLE IF NOT EXISTS AnneeScolaire (
                                             idAnnee VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    libelle VARCHAR(50) NOT NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    estCourante TINYINT(1) DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    libelle_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN libelle END) VIRTUAL,
    PRIMARY KEY (idAnnee),
    UNIQUE KEY uq_annee_ecole_actif (libelle_actif, idEcole),
    INDEX idx_annee_sync (idEcole, updated_at, version),
    CONSTRAINT fk_annee_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Niveau (
                                      idNiveau VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    nomNiveau VARCHAR(50) NOT NULL,
    ordre INT DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    nomNiveau_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nomNiveau END) VIRTUAL,
    PRIMARY KEY (idNiveau),
    UNIQUE KEY uq_niveau_ecole_actif (nomNiveau_actif, idEcole),
    INDEX idx_niveau_sync (idEcole, updated_at, version),
    CONSTRAINT fk_niveau_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Classe (
                                      idClasse VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idNiveau VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    nomClasse VARCHAR(50) NOT NULL,
    capacite INT DEFAULT 40,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    nomClasse_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nomClasse END) VIRTUAL,
    PRIMARY KEY (idClasse),
    UNIQUE KEY uq_classe_annee_actif (nomClasse_actif, idAnnee, idEcole),
    INDEX idx_classe_sync (idEcole, updated_at, version),
    CONSTRAINT fk_classe_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_classe_niveau FOREIGN KEY (idNiveau) REFERENCES Niveau(idNiveau),
    CONSTRAINT fk_classe_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Matiere (
                                       idMatiere VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    codeMatiere VARCHAR(20) NOT NULL,
    nomMatiere VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codeMatiere_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeMatiere END) VIRTUAL,
    PRIMARY KEY (idMatiere),
    UNIQUE KEY uq_matiere_code_actif (codeMatiere_actif, idEcole),
    INDEX idx_matiere_sync (idEcole, updated_at, version),
    CONSTRAINT fk_mat_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS AffectationMatiere (
                                                  idAffectation VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idClasse VARCHAR(50) NOT NULL,
    idMatiere VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    coefficient DECIMAL(4,2) DEFAULT 1.00,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idMatiere_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idMatiere END) VIRTUAL,
    PRIMARY KEY (idAffectation),
    UNIQUE KEY uq_classe_mat_actif (idClasse, idMatiere_actif),
    INDEX idx_aff_sync (idEcole, updated_at, version),
    CONSTRAINT fk_aff_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_aff_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse),
    CONSTRAINT fk_aff_matiere FOREIGN KEY (idMatiere) REFERENCES Matiere(idMatiere),
    CONSTRAINT fk_aff_personnel FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 6. ÉLÈVES & INSCRIPTIONS (SYNCHRONISÉE)
-- ==============================================================
CREATE TABLE IF NOT EXISTS Eleve (
                                     idEleve VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NULL UNIQUE,
    matricule VARCHAR(50) NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    sexe VARCHAR(10) NOT NULL,
    dateNaissance DATE,
    lieuNaissance VARCHAR(100),
    adresse TEXT,
    nationalite VARCHAR(50) DEFAULT 'Camerounaise',
    nomTuteur VARCHAR(100),
    telephoneTuteur VARCHAR(30),
    emailTuteur VARCHAR(100),
    photoPath VARCHAR(255),
    antecedentsMedicaux TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    matricule_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN matricule END) VIRTUAL,
    PRIMARY KEY (idEleve),
    UNIQUE KEY uq_eleve_matricule_actif (matricule_actif, idEcole),
    INDEX idx_eleve_sync (idEcole, updated_at, version),
    CONSTRAINT fk_eleve_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_eleve_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- MIGRATION : ENTITÉ PARENT + LIAISON PARENT-ÉLÈVE
-- ==============================================================

CREATE TABLE IF NOT EXISTS Parent (
                                      idParent VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    sexe VARCHAR(10),
    telephone VARCHAR(30),
    email VARCHAR(150),
    profession VARCHAR(100),
    adresse TEXT,
    photo VARCHAR(255),
    estActif TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idParent),
    INDEX idx_parent_sync (idEcole, updated_at, version),
    INDEX idx_parent_email (email),
    CONSTRAINT fk_parent_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_parent_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ParentEleve (
                                           idParentEleve VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idParent VARCHAR(50) NOT NULL,
    idEleve VARCHAR(50) NOT NULL,
    lienParente VARCHAR(30) DEFAULT 'PERE',
    estContactPrincipal TINYINT(1) DEFAULT 0,
    estResponsableFinancier TINYINT(1) DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idEleve_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idEleve END) VIRTUAL,
    PRIMARY KEY (idParentEleve),
    UNIQUE KEY uq_parent_eleve_actif (idParent, idEleve_actif),
    INDEX idx_pe_sync (idEcole, updated_at, version),
    INDEX idx_pe_eleve (idEleve),
    CONSTRAINT fk_pe_parent FOREIGN KEY (idParent) REFERENCES Parent(idParent) ON DELETE CASCADE,
    CONSTRAINT fk_pe_eleve FOREIGN KEY (idEleve) REFERENCES Eleve(idEleve) ON DELETE CASCADE,
    CONSTRAINT fk_pe_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Inscription (
                                           idInscription VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idEleve VARCHAR(50) NOT NULL,
    idClasse VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    dateInscription TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    statut VARCHAR(20) DEFAULT 'ACTIF',
    remiseAccordee DECIMAL(15,2) DEFAULT 0.00,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idAnnee_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idAnnee END) VIRTUAL,
    PRIMARY KEY (idInscription),
    UNIQUE KEY uq_eleve_annee_actif (idEleve, idAnnee_actif),
    INDEX idx_insc_sync (idEcole, updated_at, version),
    CONSTRAINT fk_insc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_insc_eleve FOREIGN KEY (idEleve) REFERENCES Eleve(idEleve),
    CONSTRAINT fk_insc_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse),
    CONSTRAINT fk_insc_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 7. PÉDAGOGIE (SYNCHRONISÉE)
-- ==============================================================
CREATE TABLE IF NOT EXISTS Periode (
                                       idPeriode VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    nomPeriode VARCHAR(50) NOT NULL,
    dateDebut DATE,
    dateFin DATE,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idPeriode),
    INDEX idx_periode_sync (idEcole, updated_at, version),
    CONSTRAINT fk_per_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_per_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Evaluation (
                                          idEvaluation VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idClasse VARCHAR(50) NOT NULL,
    idMatiere VARCHAR(50) NOT NULL,
    idPeriode VARCHAR(50) NOT NULL,
    titre VARCHAR(100) NOT NULL,
    typeEvaluation VARCHAR(20) DEFAULT 'DEVOIR',
    noteMaximale DECIMAL(5,2) DEFAULT 20.00,
    coefficient DECIMAL(4,2) DEFAULT 1.00,
    dateEvaluation DATE,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idEvaluation),
    INDEX idx_eval_sync (idEcole, updated_at, version),
    CONSTRAINT fk_eval_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_eval_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse),
    CONSTRAINT fk_eval_matiere FOREIGN KEY (idMatiere) REFERENCES Matiere(idMatiere),
    CONSTRAINT fk_eval_periode FOREIGN KEY (idPeriode) REFERENCES Periode(idPeriode)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Note (
                                    idNote VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idEvaluation VARCHAR(50) NOT NULL,
    idEleve VARCHAR(50) NOT NULL,
    valeurNote DECIMAL(5,2) NOT NULL,
    appreciation VARCHAR(255),
    saisiPar VARCHAR(50),
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idEleve_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idEleve END) VIRTUAL,
    PRIMARY KEY (idNote),
    UNIQUE KEY uq_eval_eleve_actif (idEvaluation, idEleve_actif),
    INDEX idx_note_sync (idEcole, updated_at, version),
    CONSTRAINT fk_note_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_note_eval FOREIGN KEY (idEvaluation) REFERENCES Evaluation(idEvaluation),
    CONSTRAINT fk_note_eleve FOREIGN KEY (idEleve) REFERENCES Eleve(idEleve),
    CONSTRAINT fk_note_user FOREIGN KEY (saisiPar) REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 8. FINANCES (SYNCHRONISÉE)
-- ==============================================================
CREATE TABLE IF NOT EXISTS TypeFrais (
                                         idTypeFrais VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idTypeFrais),
    INDEX idx_tf_sync (idEcole, updated_at, version),
    CONSTRAINT fk_tf_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Tarification (
                                            idTarification VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idNiveau VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    idTypeFrais VARCHAR(50) NOT NULL,
    montant DECIMAL(12,2) NOT NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    idTypeFrais_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idTypeFrais END) VIRTUAL,
    PRIMARY KEY (idTarification),
    UNIQUE KEY uq_tarif_actif (idNiveau, idAnnee, idTypeFrais_actif, idEcole),
    INDEX idx_tar_sync (idEcole, updated_at, version),
    CONSTRAINT fk_tar_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_tar_niveau FOREIGN KEY (idNiveau) REFERENCES Niveau(idNiveau),
    CONSTRAINT fk_tar_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee),
    CONSTRAINT fk_tar_tf FOREIGN KEY (idTypeFrais) REFERENCES TypeFrais(idTypeFrais)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⚠️ Paiement.numeroRecu reste UNIQUE strict, sans variante _actif :
-- l'annulation d'un reçu se fait via statut = 'ANNULE' (traçabilité comptable),
-- jamais via deleted_at. Un numéro de reçu doit rester unique pour toujours.
CREATE TABLE IF NOT EXISTS Paiement (
                                        idPaiement VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idEleve VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    numeroRecu VARCHAR(50) NOT NULL,
    montantTotal DECIMAL(12,2) NOT NULL,
    modePaiement VARCHAR(20) DEFAULT 'ESPECES',
    referencePaiement VARCHAR(100),
    datePaiement TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    encaissePar VARCHAR(50),
    statut VARCHAR(20) DEFAULT 'VALIDE',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idPaiement),
    UNIQUE KEY uq_recu_ecole (numeroRecu, idEcole),
    INDEX idx_paiement_sync (idEcole, updated_at, version),
    CONSTRAINT fk_pay_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_pay_eleve FOREIGN KEY (idEleve) REFERENCES Eleve(idEleve),
    CONSTRAINT fk_pay_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS LignePaiement (
                                             idLignePaiement VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPaiement VARCHAR(50) NOT NULL,
    idTypeFrais VARCHAR(50) NOT NULL,
    montant DECIMAL(12,2) NOT NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idLignePaiement),
    INDEX idx_lp_sync (idEcole, updated_at, version),
    CONSTRAINT fk_lp_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_lp_paiement FOREIGN KEY (idPaiement) REFERENCES Paiement(idPaiement) ON DELETE CASCADE,
    CONSTRAINT fk_lp_tf FOREIGN KEY (idTypeFrais) REFERENCES TypeFrais(idTypeFrais)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 9. ABSENCES (SYNCHRONISÉE)
-- ==============================================================
CREATE TABLE IF NOT EXISTS Absence (
                                       idAbsence VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idEleve VARCHAR(50) NOT NULL,
    idClasse VARCHAR(50) NOT NULL,
    dateAbsence DATE NOT NULL,
    nombreHeures INT DEFAULT 1,
    estJustifiee TINYINT(1) DEFAULT 0,
    motif TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idAbsence),
    INDEX idx_abs_sync (idEcole, updated_at, version),
    CONSTRAINT fk_abs_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_abs_eleve FOREIGN KEY (idEleve) REFERENCES Eleve(idEleve),
    CONSTRAINT fk_abs_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 10. MOTEUR DE SYNCHRONISATION
-- ==============================================================
CREATE TABLE IF NOT EXISTS SyncClient (
                                          idClient VARCHAR(100) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    nomMachine VARCHAR(150),
    derniereSynchro TIMESTAMP(3) NULL,
    dernierSyncLogId BIGINT DEFAULT 0,
    derniereAdresseIP VARCHAR(45),
    estActif TINYINT(1) DEFAULT 1,
    PRIMARY KEY (idClient),
    INDEX idx_sc_ecole (idEcole),
    CONSTRAINT fk_sc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SyncFile (
                                        idFile VARCHAR(50) NOT NULL,
    idClient VARCHAR(100) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    nomTable VARCHAR(50) NOT NULL,
    idEntity VARCHAR(50) NOT NULL,
    actionEnum VARCHAR(10) NOT NULL,
    donneesJSON JSON,
    versionClient BIGINT NOT NULL,
    statut VARCHAR(20) DEFAULT 'EN_ATTENTE',
    messageErreur TEXT NULL,
    dateCreation TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    dateEnvoi TIMESTAMP(3) NULL,
    nombreTentatives INT DEFAULT 0,
    PRIMARY KEY (idFile),
    INDEX idx_file_queue (idClient, statut, dateCreation)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SyncLog (
                                       idSyncLog BIGINT AUTO_INCREMENT NOT NULL,
                                       idEcole VARCHAR(50) NOT NULL,
    nomTable VARCHAR(50) NOT NULL,
    idEntity VARCHAR(50) NOT NULL,
    actionEnum VARCHAR(10) NOT NULL,
    clientSourceId VARCHAR(100) NULL,
    payloadJSON JSON NULL,
    version BIGINT NOT NULL,
    timestampServer TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idSyncLog),
    INDEX idx_synclog_delta (idEcole, idSyncLog),
    INDEX idx_synclog_entity (nomTable, idEntity)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SyncConflit (
                                           idConflit VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    nomTable VARCHAR(50) NOT NULL,
    idEntity VARCHAR(50) NOT NULL,
    clientSourceId VARCHAR(100) NOT NULL,
    versionLocale BIGINT,
    versionCloud BIGINT,
    donneesLocalesJSON JSON,
    donneesCloudJSON JSON,
    statut VARCHAR(20) DEFAULT 'EN_ATTENTE',
    dateDetection TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    resoluPar VARCHAR(50) NULL,
    dateResolution TIMESTAMP(3) NULL,
    PRIMARY KEY (idConflit),
    INDEX idx_conf_sync (idEcole, statut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 11. TABLES D'AUDIT
-- ==============================================================
CREATE TABLE IF NOT EXISTS AuditRBAC (
                                         idAuditRBAC VARCHAR(50) NOT NULL,
    idUtilisateurAction VARCHAR(50) NOT NULL,
    typeAction VARCHAR(30) NOT NULL,
    idUtilisateurCible VARCHAR(50) NULL,
    idRole VARCHAR(50) NULL,
    idPermission VARCHAR(50) NULL,
    idEcoleConcernee VARCHAR(50) NULL,
    details TEXT,
    dateAction DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idAuditRBAC),
    INDEX idx_arbac_date (dateAction),
    INDEX idx_arbac_action (idUtilisateurAction),
    INDEX idx_arbac_cible (idUtilisateurCible),
    CONSTRAINT fk_arbac_act FOREIGN KEY (idUtilisateurAction)
    REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT fk_arbac_cib FOREIGN KEY (idUtilisateurCible)
    REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL,
    CONSTRAINT fk_arbac_ecole FOREIGN KEY (idEcoleConcernee)
    REFERENCES Ecole(idEcole) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS LogsAudit (
                                         idLogAudit VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NOT NULL,
    nomAuteur VARCHAR(150) NOT NULL,
    categorieAction VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT NOT NULL,
    adresseIp VARCHAR(45),
    appareil VARCHAR(255),
    dateAction TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idLogAudit),
    INDEX idx_log_date (dateAction),
    INDEX idx_log_user (idUtilisateur),
    INDEX idx_log_categorie (categorieAction),
    INDEX idx_log_ecole (idEcole),
    CONSTRAINT fk_log_usr FOREIGN KEY (idUtilisateur)
    REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT fk_log_ecole FOREIGN KEY (idEcole)
    REFERENCES Ecole(idEcole) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;