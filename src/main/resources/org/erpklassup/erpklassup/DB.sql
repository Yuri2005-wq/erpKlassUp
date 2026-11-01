-- ==============================================================
-- 0. GROUPE D'ÉCOLES
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
-- 1. ÉCOLES
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
    devise VARCHAR(150) DEFAULT 'FCFA',
    logoPath VARCHAR(255),
    cleSecrete VARCHAR(64) NULL,
    niuEmployeur VARCHAR(30) NULL,
    numEmployeurCnps VARCHAR(30) NULL,
    numContribuableDgi VARCHAR(30) NULL,
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
-- 2. RBAC
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

-- ==============================================================
-- 3. UTILISATEURS
-- ==============================================================

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
-- 4. AUTH LOCAL
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
    idUtilisateur_session_active VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN estRevoque = 0 AND dateDeconnexion IS NULL THEN idUtilisateur END) VIRTUAL,
    PRIMARY KEY (idSession),
    INDEX idx_sess_refresh (refreshTokenHash),
    INDEX idx_sess_user (idUtilisateur),
    UNIQUE KEY uq_session_active_par_utilisateur (idUtilisateur_session_active),
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
-- 5. STRUCTURE ACADÉMIQUE DE BASE
-- ==============================================================

CREATE TABLE IF NOT EXISTS AnneeScolaire (
                                             idAnnee VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    libelle VARCHAR(50) NOT NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    estCourante TINYINT(1) DEFAULT 0,
    statut ENUM('PREPARATION','EN_COURS','CLOTUREE','ARCHIVEE') NOT NULL DEFAULT 'PREPARATION',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    libelle_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN libelle END) VIRTUAL,
    courante_flag TINYINT GENERATED ALWAYS AS (CASE WHEN estCourante = 1 AND deleted_at IS NULL THEN 1 END) VIRTUAL,
    PRIMARY KEY (idAnnee),
    UNIQUE KEY uq_annee_ecole_actif (libelle_actif, idEcole),
    UNIQUE KEY uq_annee_courante_unique (idEcole, courante_flag),
    INDEX idx_annee_sync (idEcole, updated_at, version),
    CONSTRAINT fk_annee_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT chk_annee_dates CHECK (dateFin > dateDebut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Filiere (
                                       idFiliere VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    codeFiliere VARCHAR(20) NOT NULL,
    nomFiliere VARCHAR(100) NOT NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codeFiliere_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeFiliere END) VIRTUAL,
    PRIMARY KEY (idFiliere),
    UNIQUE KEY uq_filiere_code_actif (codeFiliere_actif, idEcole),
    INDEX idx_filiere_sync (idEcole, updated_at, version),
    CONSTRAINT fk_filiere_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Niveau (
                                      idNiveau VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    nomNiveau VARCHAR(50) NOT NULL,
    systeme ENUM('FRANCOPHONE','ANGLOPHONE') NOT NULL DEFAULT 'FRANCOPHONE',
    cycle ENUM('MATERNELLE','PRIMAIRE','PREMIER_CYCLE','SECOND_CYCLE') NOT NULL,
    ordre INT DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    nomNiveau_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nomNiveau END) VIRTUAL,
    PRIMARY KEY (idNiveau),
    UNIQUE KEY uq_niveau_ecole_actif (nomNiveau_actif, idEcole, systeme),
    INDEX idx_niveau_sync (idEcole, updated_at, version),
    CONSTRAINT fk_niveau_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Salle (
                                     idSalle VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    codeSalle VARCHAR(20) NOT NULL,
    nomSalle VARCHAR(100) NOT NULL,
    batiment VARCHAR(50),
    capaciteMax INT DEFAULT 50,
    typeSalle ENUM('SALLE','LABO','AMPHI','ATELIER','SALLE_INFO','BIBLIOTHEQUE','COUR') NOT NULL DEFAULT 'SALLE',
    occupee TINYINT(1) DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codeSalle_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeSalle END) VIRTUAL,
    PRIMARY KEY (idSalle),
    UNIQUE KEY uq_salle_code_actif (codeSalle_actif, idEcole),
    INDEX idx_salle_sync (idEcole, updated_at, version),
    CONSTRAINT fk_salle_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS GroupeMatiere (
                                             idGroupe VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    codeGroupe VARCHAR(20) NOT NULL,
    nomGroupe VARCHAR(100) NOT NULL,
    ordreAffichage INT DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (IF(deleted_at IS NULL, codeGroupe, NULL)) VIRTUAL,
    PRIMARY KEY (idGroupe),
    UNIQUE KEY uq_groupe_actif (idEcole, code_actif),
    INDEX idx_groupematiere_sync (idEcole, updated_at, version),
    CONSTRAINT fk_groupemat_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Matiere (
                                       idMatiere VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idMatiereParent VARCHAR(50) NULL,
    codeMatiere VARCHAR(20) NOT NULL,
    nomMatiere VARCHAR(100) NOT NULL,
    domaine VARCHAR(50) NULL,
    idGroupeMatiere VARCHAR(50) NULL,
    ordreAffichage INT DEFAULT 0,
    description VARCHAR(255),
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    codeMatiere_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeMatiere END) VIRTUAL,
    PRIMARY KEY (idMatiere),
    UNIQUE KEY uq_matiere_code_actif (codeMatiere_actif, idEcole),
    INDEX idx_matiere_sync (idEcole, updated_at, version),
    INDEX idx_matiere_groupe (idGroupeMatiere),
    CONSTRAINT fk_mat_groupe FOREIGN KEY (idGroupeMatiere) REFERENCES GroupeMatiere(idGroupe),
    CONSTRAINT fk_mat_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_mat_parent FOREIGN KEY (idMatiereParent) REFERENCES Matiere(idMatiere)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 6. GRH — DÉPARTEMENT (FK responsable différée)
-- ==============================================================

CREATE TABLE IF NOT EXISTS Departement (
    idDepartement           VARCHAR(50) NOT NULL,
    idEcole                 VARCHAR(50) NOT NULL,
    code                    VARCHAR(20) NOT NULL,
    libelle                 VARCHAR(100) NOT NULL,
    description             VARCHAR(500) NULL,
    type                    ENUM('ADMINISTRATIF','PEDAGOGIQUE','TECHNIQUE','SUPPORT')
    NOT NULL DEFAULT 'ADMINISTRATIF',
    sousSysteme             ENUM('FRANCOPHONE','ANGLOPHONE','BILINGUE') NOT NULL DEFAULT 'BILINGUE',
    estConseilEnseignement TINYINT(1) DEFAULT 0,
    idDepartementParent     VARCHAR(50) NULL,
    niveau                  TINYINT NULL,
    chemin                  VARCHAR(255) NULL,
    ordre_affichage         INT NULL,
    idResponsable           VARCHAR(50) NULL,
    idResponsableAdjoint    VARCHAR(50) NULL,
    statut                  ENUM('PLANIFIE','ACTIF','INACTIF','ARCHIVE')
    NOT NULL DEFAULT 'ACTIF',
    date_debut              DATE NULL,
    date_fin                DATE NULL,
    motif_desactivation     VARCHAR(255) NULL,
    code_centre_cout        VARCHAR(30) NULL,
    codeMineduc             VARCHAR(50) NULL,
    budget_annuel           DECIMAL(15,2) NULL,
    effectif_max            INT NULL,
    email                   VARCHAR(150) NULL,
    telephone               VARCHAR(30) NULL,
    localisation            VARCHAR(255) NULL,
    version                 BIGINT DEFAULT 1,
    deleted_at              TIMESTAMP(3) NULL DEFAULT NULL,
    created_at              TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),

    -- ============================================================
    -- COLONNES GÉNÉRÉES (unicité sur actifs)
    -- ============================================================
    code_actif              VARCHAR(20) GENERATED ALWAYS AS
(CASE WHEN deleted_at IS NULL THEN code END) VIRTUAL,
    libelle_actif           VARCHAR(100) GENERATED ALWAYS AS
(CASE WHEN deleted_at IS NULL THEN libelle END) VIRTUAL,
    PRIMARY KEY (idDepartement),

    UNIQUE KEY uq_departement_code_actif     (code_actif, idEcole),
    UNIQUE KEY uq_departement_libelle_actif  (libelle_actif, idEcole),

    INDEX idx_departement_ecole       (idEcole),
    INDEX idx_departement_parent      (idDepartementParent),
    INDEX idx_departement_type        (type),
    INDEX idx_departement_statut      (statut),
    INDEX idx_departement_responsable (idResponsable),
    INDEX idx_departement_sync        (idEcole, updated_at, version),
    CONSTRAINT fk_grh_dep_ecole
    FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),

    CONSTRAINT fk_grh_dep_parent
    FOREIGN KEY (idDepartementParent) REFERENCES Departement(idDepartement),

    CONSTRAINT chk_dep_budget
    CHECK (budget_annuel IS NULL OR budget_annuel >= 0),

    CONSTRAINT chk_dep_effectif
    CHECK (effectif_max IS NULL OR effectif_max > 0),

    CONSTRAINT chk_dep_dates
    CHECK (date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut),

    CONSTRAINT chk_dep_pas_auto_parent
    CHECK (idDepartementParent IS NULL OR idDepartementParent != idDepartement)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- ==============================================================
-- 7. GRH — POSTE / GRADE
-- ==============================================================

CREATE TABLE IF NOT EXISTS Poste (
    idPoste VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idDepartement VARCHAR(50) NULL, -- Rattaché au département (NULL si le poste est transversal ou non assigné)
    intitule VARCHAR(100) NOT NULL,
    categorie VARCHAR(50) NULL,
    description TEXT,
    estActif TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    intitule_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN intitule END) VIRTUAL,
    PRIMARY KEY (idPoste),
    UNIQUE KEY uq_poste_intitule_actif (intitule_actif, idEcole),
    INDEX idx_poste_sync (idEcole, updated_at, version),
    INDEX idx_poste_departement (idDepartement),
    CONSTRAINT fk_grh_poste_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_poste_departement FOREIGN KEY (idDepartement) REFERENCES Departement(idDepartement)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS GradeSalarial (
    idGrade VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    categorie VARCHAR(20) NULL,
    echelon VARCHAR(10) NULL,
    salaireBaseReference DECIMAL(12,2) DEFAULT 0.00,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN code END) VIRTUAL,
    PRIMARY KEY (idGrade),
    UNIQUE KEY uq_grade_code_actif (code_actif, idEcole),
    INDEX idx_grade_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_grade_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 8. GRH — PERSONNEL
-- ==============================================================

CREATE TABLE IF NOT EXISTS Personnel (
    idPersonnel VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NULL,
    idDepartement VARCHAR(50) NULL,
    idPoste VARCHAR(50) NULL,
    idResponsable VARCHAR(50) NULL,
    matriculeInterne VARCHAR(50) NOT NULL,
    matriculeFonctionPublique VARCHAR(30) NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    sexe VARCHAR(20),
    dateNaissance DATE,
    lieuNaissance VARCHAR(100),
    nationalite VARCHAR(50) DEFAULT 'Camerounaise',
    situationMatrimoniale VARCHAR(30),
    nombreEnfants INT NOT NULL DEFAULT 0,
    numeroCni VARCHAR(50),
    niu VARCHAR(50),
    numCnps VARCHAR(50),
    telephone VARCHAR(30),
    contactUrgence VARCHAR(30),
    contactUrgenceNom VARCHAR(150),
    email VARCHAR(150),
    adresse VARCHAR(255),
    ville VARCHAR(100),
    photo VARCHAR(255),
    typePersonnel VARCHAR(50) NOT NULL,
    posteOuFonction VARCHAR(100) NOT NULL,
    qualification VARCHAR(100),
    statutContractuel VARCHAR(50) DEFAULT 'PERMANENT',
    quotaHeuresHebdo DECIMAL(4,2) NULL,
    dechargeHeures DECIMAL(4,2) DEFAULT 0.00,
    dateEmbauche DATE,
    dateDepart DATE NULL,
    motifDepart VARCHAR(30) NULL,
    statutEmploi VARCHAR(30) NOT NULL DEFAULT 'actif',
    modePaiement VARCHAR(30) DEFAULT 'ESPECES',
    nomBanque VARCHAR(100),
    ibanOuRib VARCHAR(50),
    numeroMobileMoney VARCHAR(30),

    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    matriculeInterne_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN matriculeInterne END) VIRTUAL,
    idUtilisateur_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idUtilisateur END) VIRTUAL,
    numCnps_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN numCnps END) VIRTUAL,
    PRIMARY KEY (idPersonnel),
    UNIQUE KEY uq_personnel_matricule_actif (matriculeInterne_actif, idEcole),
    UNIQUE KEY uq_personnel_user_actif (idUtilisateur_actif),
    UNIQUE KEY uq_personnel_cnps_actif (numCnps_actif, idEcole),
    INDEX idx_pers_sync (idEcole, updated_at, version),
    INDEX idx_pers_nom (nom, prenom),
    INDEX idx_pers_filtre (typePersonnel, statutEmploi),
    CONSTRAINT chk_pers_enfants CHECK (nombreEnfants >= 0),
    CONSTRAINT chk_pers_dates CHECK (dateDepart IS NULL OR dateEmbauche IS NULL OR dateDepart >= dateEmbauche),
    CONSTRAINT chk_pers_statut_emploi CHECK (statutEmploi IN ('ACTIF', 'SUSPENDU', 'SORTI')),
    CONSTRAINT chk_pers_paiement CHECK (modePaiement IS NULL OR modePaiement IN ('ESPECES', 'VIREMENT', 'MOBILE_MONEY', 'CHEQUE')),
    CONSTRAINT chk_pers_sexe CHECK (sexe IS NULL OR sexe IN ('M', 'F', 'MASCULIN', 'FEMININ', 'AUTRE')),
    CONSTRAINT chk_pers_type CHECK (typePersonnel IN ('ADMINISTRATIF', 'ENSEIGNANT', 'SUPPORT', 'DIRECTION', 'AUTRE')),
    CONSTRAINT chk_pers_contrat CHECK (statutContractuel IS NULL OR statutContractuel IN ('PERMANENT', 'CDD', 'VACATAIRE', 'FONCTIONNAIRE_DETACHE', 'STAGIAIRE', 'AUTRE')),
    CONSTRAINT fk_pers_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_pers_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT fk_pers_dept FOREIGN KEY (idDepartement) REFERENCES Departement(idDepartement) ON DELETE SET NULL,
    CONSTRAINT fk_pers_poste FOREIGN KEY (idPoste) REFERENCES Poste(idPoste) ON DELETE SET NULL,
    CONSTRAINT fk_pers_resp FOREIGN KEY (idResponsable) REFERENCES Personnel(idPersonnel) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 9. GRH — CONTRATS
-- ==============================================================

CREATE TABLE IF NOT EXISTS ContratPersonnel (
                                                idContrat VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idPoste VARCHAR(50) NULL,
    idGrade VARCHAR(50) NULL,
    numeroContrat VARCHAR(40) NULL,
    typeContrat VARCHAR(30) NOT NULL,
    dateSignature DATE NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NULL,
    periodeEssaiFin DATE NULL,
    salaireBase DECIMAL(12,2) DEFAULT 0.00,
    tauxHoraire DECIMAL(10,2) DEFAULT 0.00,
    volumeHoraireMensuel INT DEFAULT 0,
    statut VARCHAR(20) DEFAULT 'ACTIF',
    motifFin VARCHAR(255) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    numeroContrat_actif VARCHAR(40) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN numeroContrat END) VIRTUAL,
    PRIMARY KEY (idContrat),
    UNIQUE KEY uq_contrat_numero_actif (numeroContrat_actif, idEcole),
    INDEX idx_contrat_periode (idPersonnel, dateDebut, dateFin),
    INDEX idx_contrat_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_contrat_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_contrat_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_contrat_poste FOREIGN KEY (idPoste) REFERENCES Poste(idPoste),
    CONSTRAINT fk_grh_contrat_grade FOREIGN KEY (idGrade) REFERENCES GradeSalarial(idGrade),
    CONSTRAINT chk_grh_contrat_dates CHECK (dateFin IS NULL OR dateFin >= dateDebut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS AvenantContrat (
                                              idAvenant VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idContrat VARCHAR(50) NOT NULL,
    typeAvenant VARCHAR(30) NOT NULL,
    dateEffet DATE NOT NULL,
    ancienSalaireBase DECIMAL(12,2) NULL,
    nouveauSalaireBase DECIMAL(12,2) NULL,
    ancienTauxHoraire DECIMAL(10,2) NULL,
    nouveauTauxHoraire DECIMAL(10,2) NULL,
    nouvelleDateFin DATE NULL,
    motif TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idAvenant),
    INDEX idx_avenant_contrat (idContrat, dateEffet),
    INDEX idx_avenant_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_avenant_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_avenant_contrat FOREIGN KEY (idContrat) REFERENCES ContratPersonnel(idContrat)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 10. GRH — TARIFS, COMPÉTENCES, DOCUMENTS
-- ==============================================================

CREATE TABLE IF NOT EXISTS TarifVacation (
                                             idTarif VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idNiveau VARCHAR(50) NULL,
    idMatiere VARCHAR(50) NULL,
    tauxHoraire DECIMAL(10,2) NOT NULL,
    dateEffet DATE NOT NULL,
    dateFin DATE NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    tarif_actif VARCHAR(150) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                  CONCAT(IFNULL(idNiveau, '*'), '_', IFNULL(idMatiere, '*'), '_', CAST(dateEffet AS CHAR)) END) VIRTUAL,
    PRIMARY KEY (idTarif),
    UNIQUE KEY uq_tarif_actif (tarif_actif, idEcole),
    INDEX idx_tarif_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_tarif_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_tarif_niveau FOREIGN KEY (idNiveau) REFERENCES Niveau(idNiveau),
    CONSTRAINT fk_grh_tarif_matiere FOREIGN KEY (idMatiere) REFERENCES Matiere(idMatiere)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS PersonnelCompetence (
                                                   idCompetence VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idMatiere VARCHAR(50) NOT NULL,
    idNiveau VARCHAR(50) NOT NULL,
    niveauPriorite INT DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    competence_actif VARCHAR(160) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                       CONCAT(idPersonnel, '_', idMatiere, '_', idNiveau) END) VIRTUAL,
    PRIMARY KEY (idCompetence),
    UNIQUE KEY uq_competence_actif (competence_actif, idEcole),
    INDEX idx_competence_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_comp_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_comp_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_comp_matiere FOREIGN KEY (idMatiere) REFERENCES Matiere(idMatiere),
    CONSTRAINT fk_grh_comp_niveau FOREIGN KEY (idNiveau) REFERENCES Niveau(idNiveau)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DocumentPersonnel (
                                                 idDocument VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    typeDocument VARCHAR(50) NOT NULL,
    nomFichier VARCHAR(255) NOT NULL,
    cheminStockage VARCHAR(500) NOT NULL,
    mimeType VARCHAR(100) NULL,
    tailleOctets BIGINT NULL,
    hashSha256 CHAR(64) NULL,
    dateExpiration DATE NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idDocument),
    INDEX idx_doc_expiration (idEcole, dateExpiration),
    INDEX idx_doc_personnel (idPersonnel),
    INDEX idx_doc_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_doc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_doc_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DiplomePersonnel (
                                                idDiplome VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    typeDiplome VARCHAR(50) NOT NULL,
    intitule VARCHAR(200) NOT NULL,
    specialite VARCHAR(150) NULL,
    etablissement VARCHAR(200) NULL,
    anneeObtention INT NULL,
    mention VARCHAR(50) NULL,
    idDocument VARCHAR(50) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idDiplome),
    INDEX idx_diplome_personnel (idPersonnel),
    INDEX idx_diplome_sync (idEcole, updated_at, version),
    CONSTRAINT fk_dip_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_dip_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_dip_doc FOREIGN KEY (idDocument) REFERENCES DocumentPersonnel(idDocument) ON DELETE SET NULL,
    CONSTRAINT chk_dip_annee CHECK (anneeObtention IS NULL OR (anneeObtention >= 1900 AND anneeObtention <= 2100))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ayants droit (conjoint, enfants) pour prestations familiales CNPS
CREATE TABLE IF NOT EXISTS AyantDroitPersonnel (
                                                   idAyantDroit VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    lienParente VARCHAR(30) NOT NULL,          -- CONJOINT, ENFANT, PARENT, AUTRE
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NULL,
    sexe VARCHAR(20) NULL,
    dateNaissance DATE NULL,
    numCnps VARCHAR(50) NULL,                  -- certains ayants droit ont leur propre numéro CNPS
    estCharge TINYINT(1) DEFAULT 1,            -- ouvrant droit aux prestations ?
    dateDebutValidite DATE NULL,
    dateFinValidite DATE NULL,                 -- ex : enfant > 21 ans sort de la liste
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idAyantDroit),
    INDEX idx_ayant_personnel (idPersonnel),
    INDEX idx_ayant_sync (idEcole, updated_at, version),
    CONSTRAINT fk_ad_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_ad_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT chk_ad_lien CHECK (lienParente IN ('CONJOINT', 'ENFANT', 'PARENT', 'AUTRE')),
    CONSTRAINT chk_ad_dates CHECK (dateFinValidite IS NULL OR dateDebutValidite IS NULL OR dateFinValidite >= dateDebutValidite)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Jours fériés et non ouvrés
CREATE TABLE IF NOT EXISTS JourFerie (
                                         idJourFerie VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NULL,
    libelle VARCHAR(150) NOT NULL,
    dateJour DATE NOT NULL,
    typeJour VARCHAR(30) DEFAULT 'FERIE',
    recurrent TINYINT(1) DEFAULT 0,
    estChome TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    jour_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                 CONCAT(CAST(dateJour AS CHAR), '_', IFNULL(idEcole, '@NATIONAL')) END) VIRTUAL,
    PRIMARY KEY (idJourFerie),
    UNIQUE KEY uq_jour_actif (jour_actif),
    INDEX idx_jour_date (dateJour),
    INDEX idx_jour_ecole (idEcole),
    CONSTRAINT fk_jf_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 11. STRUCTURE ACADÉMIQUE (suite) — Classe
-- ==============================================================

CREATE TABLE IF NOT EXISTS Classe (
                                      idClasse VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idNiveau VARCHAR(50) NOT NULL,
    idFiliere VARCHAR(50) NULL,
    idAnnee VARCHAR(50) NOT NULL,
    idEnseignantTitulaire VARCHAR(50) NULL,
    idSalle VARCHAR(50) NULL,
    codeClasse VARCHAR(20) NOT NULL,
    nomClasse VARCHAR(50) NOT NULL,
    vacation ENUM('MATIN','SOIR','JOURNEE_CONTINUE') NOT NULL DEFAULT 'JOURNEE_CONTINUE',
    capacite INT DEFAULT 40,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    nomClasse_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nomClasse END) VIRTUAL,
    PRIMARY KEY (idClasse),
    UNIQUE KEY uq_classe_annee_vacation_actif (nomClasse_actif, idAnnee, idEcole, vacation),
    INDEX idx_classe_sync (idEcole, updated_at, version),
    INDEX idx_classe_niveau (idNiveau),
    INDEX idx_classe_filiere (idFiliere),
    CONSTRAINT fk_classe_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_classe_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee),
    CONSTRAINT fk_classe_niveau FOREIGN KEY (idNiveau) REFERENCES Niveau(idNiveau),
    CONSTRAINT fk_classe_filiere FOREIGN KEY (idFiliere) REFERENCES Filiere(idFiliere),
    CONSTRAINT fk_classe_salle FOREIGN KEY (idSalle) REFERENCES Salle(idSalle),
    CONSTRAINT fk_classe_enseignant FOREIGN KEY (idEnseignantTitulaire) REFERENCES Personnel(idPersonnel),
    CONSTRAINT chk_classe_capacite CHECK (capacite > 0)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Responsabilités (PP, chef dept, censeur, chef travaux, surveillant général)
CREATE TABLE IF NOT EXISTS ResponsabilitePersonnel (
                                                       idResponsabilite VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    codeResponsabilite VARCHAR(30) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    idDepartement VARCHAR(50) NULL,
    idClasse VARCHAR(50) NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NULL,
    dechargeHeures DECIMAL(4,2) DEFAULT 0.00,
    primeMensuelle DECIMAL(12,2) DEFAULT 0.00,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    responsabilite_actif VARCHAR(120) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                           CONCAT(idPersonnel, '_', codeResponsabilite, '_', IFNULL(idClasse, '@NULL'), '_', CAST(dateDebut AS CHAR)) END) VIRTUAL,
    PRIMARY KEY (idResponsabilite),
    UNIQUE KEY uq_responsabilite_actif (responsabilite_actif, idEcole),
    INDEX idx_resp_personnel (idPersonnel, dateDebut),
    INDEX idx_resp_sync (idEcole, updated_at, version),
    CONSTRAINT fk_resp_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_resp_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_resp_dept FOREIGN KEY (idDepartement) REFERENCES Departement(idDepartement) ON DELETE SET NULL,
    CONSTRAINT fk_resp_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse) ON DELETE SET NULL,
    CONSTRAINT chk_resp_dates CHECK (dateFin IS NULL OR dateFin >= dateDebut),
    CONSTRAINT chk_resp_code CHECK (codeResponsabilite IN (
                                    'PROFESSEUR_PRINCIPAL', 'CHEF_DEPARTEMENT', 'CENSEUR', 'CHEF_TRAVAUX',
                                    'SURVEILLANT_GENERAL', 'DIRECTEUR', 'DIRECTEUR_ADJOINT', 'AUTRE'
                                                          ))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 12. GRH — PAIE : RUBRIQUES (avant ElementPaiePersonnel)
-- ==============================================================

CREATE TABLE IF NOT EXISTS RubriquePaie (
                                            idRubrique VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    typeRubrique VARCHAR(20) NOT NULL,
    modeCalcul VARCHAR(20) NOT NULL,
    valeurParDefaut DECIMAL(12,2) DEFAULT 0.00,
    estPatronale TINYINT(1) DEFAULT 0,
    assiette VARCHAR(30) DEFAULT 'BRUT',
    plafondMensuel DECIMAL(12,2) NULL,
    codeParametre VARCHAR(50) NULL,
    ordreCalcul INT DEFAULT 100,
    estImposable TINYINT(1) DEFAULT 1,
    estCotisable TINYINT(1) DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN code END) VIRTUAL,
    PRIMARY KEY (idRubrique),
    UNIQUE KEY uq_rubrique_code_actif (code_actif, idEcole),
    INDEX idx_rubrique_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_rubrique_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Éléments de paie par personne (primes, indemnités, retenues)
CREATE TABLE IF NOT EXISTS ElementPaiePersonnel (
                                                    idElement VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idContrat VARCHAR(50) NULL,
    idRubrique VARCHAR(50) NOT NULL,
    montantFixe DECIMAL(12,2) NULL,
    taux DECIMAL(7,4) NULL,
    typeElement VARCHAR(20) NOT NULL,
    idPeriode VARCHAR(50) NULL,
    dateDebut DATE NULL,
    dateFin DATE NULL,
    motif VARCHAR(255) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    element_actif VARCHAR(180) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                    CONCAT(idPersonnel, '_', idRubrique, '_', IFNULL(idPeriode, 'REC'), '_', IFNULL(CAST(dateDebut AS CHAR), '@NULL')) END) VIRTUAL,
    PRIMARY KEY (idElement),
    UNIQUE KEY uq_element_actif (element_actif, idEcole),
    INDEX idx_element_personnel (idPersonnel),
    INDEX idx_element_periode (idPeriode),
    INDEX idx_element_sync (idEcole, updated_at, version),
    CONSTRAINT fk_elem_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_elem_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_elem_contrat FOREIGN KEY (idContrat) REFERENCES ContratPersonnel(idContrat) ON DELETE SET NULL,
    CONSTRAINT fk_elem_rubrique FOREIGN KEY (idRubrique) REFERENCES RubriquePaie(idRubrique),
    CONSTRAINT chk_elem_dates CHECK (dateFin IS NULL OR dateDebut IS NULL OR dateFin >= dateDebut),
    CONSTRAINT chk_elem_type CHECK (typeElement IN ('RECURRENT', 'PONCTUEL'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Périodes de paie
CREATE TABLE IF NOT EXISTS PeriodePaie (
                                           idPeriode VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    mois INT NOT NULL,
    annee INT NOT NULL,
    statut VARCHAR(20) DEFAULT 'OUVERTE',
    dateCloture DATETIME NULL,
    idClotureur VARCHAR(50) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    periode_actif VARCHAR(10) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                   CONCAT(annee, '-', LPAD(mois, 2, '0')) END) VIRTUAL,
    PRIMARY KEY (idPeriode),
    UNIQUE KEY uq_periodepaie_actif (periode_actif, idEcole),
    INDEX idx_periodepaie_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_periode_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_periode_user FOREIGN KEY (idClotureur) REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT chk_grh_periode_mois CHECK (mois BETWEEN 1 AND 12)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lots de paiement
CREATE TABLE IF NOT EXISTS LotPaiement (
                                           idLot VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPeriode VARCHAR(50) NOT NULL,
    modePaiement VARCHAR(30) NOT NULL,
    reference VARCHAR(100) NULL,
    montantTotal DECIMAL(14,2) DEFAULT 0.00,
    nombreBulletins INT DEFAULT 0,
    statut VARCHAR(20) DEFAULT 'PREPARE',
    dateExecution DATETIME NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idLot),
    INDEX idx_lot_periode (idPeriode),
    INDEX idx_lot_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_lot_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_lot_periode FOREIGN KEY (idPeriode) REFERENCES PeriodePaie(idPeriode)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Paramètres légaux (CNPS, FNE, IRPP, CAC, RAV…)
CREATE TABLE IF NOT EXISTS ParametreLegalPaie (
                                                  idParametre VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(150) NOT NULL,
    valeur DECIMAL(16,4) NOT NULL,
    unite VARCHAR(20) NOT NULL DEFAULT 'POURCENT',
    dateEffet DATE NOT NULL,
    dateFin DATE NULL,
    referenceTexte VARCHAR(255) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    parametre_actif VARCHAR(70) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                     CONCAT(code, '_', CAST(dateEffet AS CHAR)) END) VIRTUAL,
    PRIMARY KEY (idParametre),
    UNIQUE KEY uq_parametre_actif (parametre_actif),
    INDEX idx_parametre_sync (updated_at, version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tranches de barème IRPP
CREATE TABLE IF NOT EXISTS TrancheBaremeImpot (
                                                  idTranche VARCHAR(50) NOT NULL,
    codeBareme VARCHAR(30) NOT NULL,
    borneInferieure DECIMAL(14,2) NOT NULL,
    borneSuperieure DECIMAL(14,2) NULL,
    taux DECIMAL(7,4) NOT NULL,
    dateEffet DATE NOT NULL,
    dateFin DATE NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    tranche_actif VARCHAR(90) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                   CONCAT(codeBareme, '_', borneInferieure, '_', CAST(dateEffet AS CHAR)) END) VIRTUAL,
    PRIMARY KEY (idTranche),
    UNIQUE KEY uq_tranche_actif (tranche_actif),
    INDEX idx_tranche_sync (updated_at, version),
    CONSTRAINT chk_grh_tranche_bornes CHECK (borneSuperieure IS NULL OR borneSuperieure > borneInferieure)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 13. GRH — BULLETINS (avec snapshot et type)
-- ==============================================================

CREATE TABLE IF NOT EXISTS BulletinPaie (
                                            idBulletin VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idContrat VARCHAR(50) NOT NULL,
    idPeriode VARCHAR(50) NULL,
    idLotPaiement VARCHAR(50) NULL,
    mois INT NOT NULL,
    annee INT NOT NULL,
    typeBulletin VARCHAR(30) DEFAULT 'PAIE_MENSUELLE',

    -- Snapshot des données au moment du calcul
    snapshotPoste VARCHAR(100) NULL,
    snapshotCategorie VARCHAR(20) NULL,
    snapshotEchelon VARCHAR(10) NULL,
    snapshotGrade VARCHAR(100) NULL,
    snapshotDateEmbauche DATE NULL,
    snapshotNumCnps VARCHAR(50) NULL,
    snapshotNIU VARCHAR(50) NULL,
    snapshotMatriculeInterne VARCHAR(50) NULL,
    snapshotNomComplet VARCHAR(200) NULL,
    snapshotJSON JSON NULL,

    -- Montants
    salaireBrut DECIMAL(12,2) NOT NULL,
    netImposable DECIMAL(12,2) DEFAULT 0.00,
    totalRetenues DECIMAL(12,2) NOT NULL,
    totalChargesPatronales DECIMAL(12,2) DEFAULT 0.00,
    salaireNet DECIMAL(12,2) NOT NULL,

    statutPaiement VARCHAR(20) DEFAULT 'BROUILLON',
    datePaiement DATETIME NULL,
    modePaiementUtilise VARCHAR(30) NULL,
    referencePaiement VARCHAR(100) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    bulletin_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                     CONCAT(idPersonnel, '_', annee, '_', LPAD(mois, 2, '0'), '_', typeBulletin) END) VIRTUAL,
    PRIMARY KEY (idBulletin),
    UNIQUE KEY uq_bulletin_actif (bulletin_actif, idEcole),
    INDEX idx_bulletin_periode (idEcole, annee, mois, statutPaiement),
    INDEX idx_bulletin_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_bulletin_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_bulletin_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_bulletin_contrat FOREIGN KEY (idContrat) REFERENCES ContratPersonnel(idContrat),
    CONSTRAINT fk_grh_bulletin_periode FOREIGN KEY (idPeriode) REFERENCES PeriodePaie(idPeriode),
    CONSTRAINT fk_grh_bulletin_lot FOREIGN KEY (idLotPaiement) REFERENCES LotPaiement(idLot),
    CONSTRAINT chk_grh_bulletin_mois CHECK (mois BETWEEN 1 AND 12),
    CONSTRAINT chk_grh_bulletin_type CHECK (typeBulletin IN (
                                            'PAIE_MENSUELLE', 'SOLDE_TOUT_COMPTE', 'PRIME_EXCEPTIONNELLE', 'RAPPEL', 'RAPPEL_RETENUE'
                                                            ))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS LigneBulletin (
                                             idLigneBulletin VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idBulletin VARCHAR(50) NOT NULL,
    idRubrique VARCHAR(50) NOT NULL,
    libelleRubrique VARCHAR(100) NULL,
    baseCalcul DECIMAL(12,2) NOT NULL,
    taux DECIMAL(7,4) DEFAULT 0.0000,
    montantGain DECIMAL(12,2) DEFAULT 0.00,
    montantRetenue DECIMAL(12,2) DEFAULT 0.00,
    montantPatronal DECIMAL(12,2) DEFAULT 0.00,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idLigneBulletin),
    INDEX idx_ligne_bulletin (idBulletin),
    INDEX idx_ligne_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_ligne_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_ligne_bulletin FOREIGN KEY (idBulletin) REFERENCES BulletinPaie(idBulletin),
    CONSTRAINT fk_grh_ligne_rubrique FOREIGN KEY (idRubrique) REFERENCES RubriquePaie(idRubrique)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Solde de tout compte
CREATE TABLE IF NOT EXISTS SoldeToutCompte (
                                               idSoldeToutCompte VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idContrat VARCHAR(50) NULL,
    idBulletin VARCHAR(50) NULL,
    dateSortie DATE NOT NULL,
    motifSortie VARCHAR(50) NOT NULL,          -- DEMISSION, LICENCIEMENT, RETRAITE, FIN_CDD, DECES, RUPTURE_CONVENTIONNELLE
    dateNotification DATE NULL,
    dateFinPreavis DATE NULL,
    indemnitePreavis DECIMAL(12,2) DEFAULT 0.00,
    indemniteLicenciement DECIMAL(12,2) DEFAULT 0.00,
    indemniteCongesNonPris DECIMAL(12,2) DEFAULT 0.00,
    autresIndemnites DECIMAL(12,2) DEFAULT 0.00,
    totalBrut DECIMAL(12,2) DEFAULT 0.00,
    totalRetenues DECIMAL(12,2) DEFAULT 0.00,
    totalNet DECIMAL(12,2) DEFAULT 0.00,
    statut VARCHAR(20) DEFAULT 'BROUILLON',    -- BROUILLON, VALIDE, PAYE, ANNULE
    datePaiement DATETIME NULL,
    motif TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idSoldeToutCompte),
    INDEX idx_stc_personnel (idPersonnel),
    INDEX idx_stc_sync (idEcole, updated_at, version),
    CONSTRAINT fk_stc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_stc_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_stc_contrat FOREIGN KEY (idContrat) REFERENCES ContratPersonnel(idContrat) ON DELETE SET NULL,
    CONSTRAINT fk_stc_bulletin FOREIGN KEY (idBulletin) REFERENCES BulletinPaie(idBulletin) ON DELETE SET NULL,
    CONSTRAINT chk_stc_motif CHECK (motifSortie IN (
                                    'DEMISSION', 'LICENCIEMENT', 'RETRAITE', 'FIN_CDD', 'DECES', 'RUPTURE_CONVENTIONNELLE', 'AUTRE'
                                                   ))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bilan heures enseignants
CREATE TABLE IF NOT EXISTS BilanHeuresEnseignant (
                                                     idBilan VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idBulletin VARCHAR(50) NULL,
    mois INT NOT NULL,
    annee INT NOT NULL,
    heuresStatutaires DECIMAL(6,2) DEFAULT 0.00,
    heuresEffectuees DECIMAL(6,2) DEFAULT 0.00,
    heuresSupplementaires DECIMAL(6,2) DEFAULT 0.00,
    heuresAbsences DECIMAL(6,2) DEFAULT 0.00,
    heuresPayables DECIMAL(6,2) DEFAULT 0.00,
    validePourPaie TINYINT(1) DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    bilan_actif VARCHAR(70) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                 CONCAT(idPersonnel, '_', annee, '_', LPAD(mois, 2, '0')) END) VIRTUAL,
    PRIMARY KEY (idBilan),
    UNIQUE KEY uq_bilan_actif (bilan_actif, idEcole),
    INDEX idx_bilan_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_bilan_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_bilan_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_bilan_bulletin FOREIGN KEY (idBulletin) REFERENCES BulletinPaie(idBulletin),
    CONSTRAINT chk_grh_bilan_mois CHECK (mois BETWEEN 1 AND 12)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Avances
CREATE TABLE IF NOT EXISTS AvanceSalaire (
                                             idAvance VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    typeAvance VARCHAR(20) DEFAULT 'AVANCE',
    montant DECIMAL(12,2) NOT NULL,
    dateDemande DATE NOT NULL,
    nombreEcheances INT DEFAULT 1,
    moisRemboursement INT NOT NULL,
    anneeRemboursement INT NOT NULL,
    motif VARCHAR(255) NULL,
    statut VARCHAR(20) DEFAULT 'ACCORDE',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idAvance),
    INDEX idx_avance_statut (idPersonnel, statut),
    INDEX idx_avance_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_avance_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_avance_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT chk_grh_avance_montant CHECK (montant > 0)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS EcheanceAvance (
                                              idEcheance VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idAvance VARCHAR(50) NOT NULL,
    mois INT NOT NULL,
    annee INT NOT NULL,
    montant DECIMAL(12,2) NOT NULL,
    statut VARCHAR(20) DEFAULT 'A_PRELEVER',
    idLigneBulletin VARCHAR(50) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    echeance_actif VARCHAR(80) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                    CONCAT(idAvance, '_', annee, '_', LPAD(mois, 2, '0')) END) VIRTUAL,
    PRIMARY KEY (idEcheance),
    UNIQUE KEY uq_echeance_actif (echeance_actif, idEcole),
    INDEX idx_echeance_periode (idEcole, annee, mois, statut),
    INDEX idx_echeance_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_echeance_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_echeance_avance FOREIGN KEY (idAvance) REFERENCES AvanceSalaire(idAvance),
    CONSTRAINT fk_grh_echeance_ligne FOREIGN KEY (idLigneBulletin) REFERENCES LigneBulletin(idLigneBulletin),
    CONSTRAINT chk_grh_echeance_mois CHECK (mois BETWEEN 1 AND 12)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 14. DÉCLARATIONS SOCIALES ET FISCALES
-- ==============================================================

CREATE TABLE IF NOT EXISTS DeclarationSociale (
                                                  idDeclaration VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPeriode VARCHAR(50) NOT NULL,             -- mois/année
    typeOrganisme VARCHAR(30) NOT NULL DEFAULT 'CNPS',  -- CNPS, AUTRE
    numeroDeclaration VARCHAR(50) NULL,         -- numéro de bordereau
    referenceTexte VARCHAR(255) NULL,
    montantSalarial DECIMAL(14,2) DEFAULT 0.00,
    montantPatronal DECIMAL(14,2) DEFAULT 0.00,
    montantTotal DECIMAL(14,2) DEFAULT 0.00,
    nombreSalaries INT DEFAULT 0,
    statut VARCHAR(20) DEFAULT 'BROUILLON',     -- BROUILLON, GENERE, TRANSMIS, PAYE, REJETE
    dateGeneration DATETIME NULL,
    dateTransmission DATETIME NULL,
    datePaiement DATETIME NULL,
    cheminFichier VARCHAR(500) NULL,            -- bordereau PDF généré
    details TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    decl_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                 CONCAT(typeOrganisme, '_', idPeriode) END) VIRTUAL,
    PRIMARY KEY (idDeclaration),
    UNIQUE KEY uq_decl_sociale_actif (decl_actif, idEcole),
    INDEX idx_decl_sociale_periode (idEcole, idPeriode),
    INDEX idx_decl_sociale_sync (idEcole, updated_at, version),
    CONSTRAINT fk_decl_soc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_decl_soc_periode FOREIGN KEY (idPeriode) REFERENCES PeriodePaie(idPeriode)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DeclarationFiscale (
                                                  idDeclaration VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPeriode VARCHAR(50) NOT NULL,
    typeImpot VARCHAR(30) NOT NULL DEFAULT 'IRPP',  -- IRPP, CAC, RAV, AUTRE
    numeroDeclaration VARCHAR(50) NULL,
    referenceTexte VARCHAR(255) NULL,
    baseImposable DECIMAL(14,2) DEFAULT 0.00,
    montantRetenu DECIMAL(14,2) DEFAULT 0.00,
    nombreSalaries INT DEFAULT 0,
    statut VARCHAR(20) DEFAULT 'BROUILLON',
    dateGeneration DATETIME NULL,
    dateTransmission DATETIME NULL,
    datePaiement DATETIME NULL,
    cheminFichier VARCHAR(500) NULL,
    details TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    decl_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                 CONCAT(typeImpot, '_', idPeriode) END) VIRTUAL,
    PRIMARY KEY (idDeclaration),
    UNIQUE KEY uq_decl_fiscale_actif (decl_actif, idEcole),
    INDEX idx_decl_fiscale_periode (idEcole, idPeriode),
    INDEX idx_decl_fiscale_sync (idEcole, updated_at, version),
    CONSTRAINT fk_decl_fisc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_decl_fisc_periode FOREIGN KEY (idPeriode) REFERENCES PeriodePaie(idPeriode)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 15. GRH — CONGÉS ET ABSENCES
-- ==============================================================

CREATE TABLE IF NOT EXISTS TypeConge (
                                         idTypeConge VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    code VARCHAR(20) NULL,
    libelle VARCHAR(100) NOT NULL,
    remunere TINYINT(1) DEFAULT 1,
    deduitDuSolde TINYINT(1) DEFAULT 1,
    justificatifRequis TINYINT(1) DEFAULT 0,
    quotaAnnuelJours INT DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN code END) VIRTUAL,
    PRIMARY KEY (idTypeConge),
    UNIQUE KEY uq_typeconge_code_actif (code_actif, idEcole),
    INDEX idx_typeconge_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_typeconge_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DemandeConge (
                                            idDemandeConge VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idTypeConge VARCHAR(50) NOT NULL,
    idRemplacant VARCHAR(50) NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    nombreJours DECIMAL(5,1) NOT NULL,
    motif TEXT,
    cheminJustificatif VARCHAR(500) NULL,
    statut VARCHAR(20) DEFAULT 'EN_ATTENTE',
    idValidateur VARCHAR(50) NULL,
    dateValidation DATETIME NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idDemandeConge),
    INDEX idx_conge_periode (idPersonnel, dateDebut, dateFin),
    INDEX idx_conge_statut (idEcole, statut),
    INDEX idx_conge_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_conge_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_conge_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_conge_type FOREIGN KEY (idTypeConge) REFERENCES TypeConge(idTypeConge),
    CONSTRAINT fk_grh_conge_remp FOREIGN KEY (idRemplacant) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_conge_valid FOREIGN KEY (idValidateur) REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT chk_grh_conge_dates CHECK (dateFin >= dateDebut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ValidationConge (
                                               idValidation VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idDemandeConge VARCHAR(50) NOT NULL,
    ordre INT NOT NULL DEFAULT 1,
    idValidateur VARCHAR(50) NOT NULL,
    decision VARCHAR(20) DEFAULT 'EN_ATTENTE',
    dateDecision DATETIME NULL,
    commentaire VARCHAR(500) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    validation_actif VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                       CONCAT(idDemandeConge, '_', ordre) END) VIRTUAL,
    PRIMARY KEY (idValidation),
    UNIQUE KEY uq_validation_actif (validation_actif, idEcole),
    INDEX idx_validation_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_valid_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_valid_demande FOREIGN KEY (idDemandeConge) REFERENCES DemandeConge(idDemandeConge),
    CONSTRAINT fk_grh_valid_user FOREIGN KEY (idValidateur) REFERENCES Utilisateur(idUtilisateur)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SoldeConge (
                                          idSolde VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idTypeConge VARCHAR(50) NOT NULL,
    annee INT NOT NULL,
    joursAcquis DECIMAL(5,1) DEFAULT 0.0,
    joursPris DECIMAL(5,1) DEFAULT 0.0,
    joursReportes DECIMAL(5,1) DEFAULT 0.0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    solde_actif VARCHAR(160) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                  CONCAT(idPersonnel, '_', idTypeConge, '_', annee) END) VIRTUAL,
    PRIMARY KEY (idSolde),
    UNIQUE KEY uq_solde_actif (solde_actif, idEcole),
    INDEX idx_solde_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_solde_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_solde_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_solde_type FOREIGN KEY (idTypeConge) REFERENCES TypeConge(idTypeConge)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS AbsencePersonnel (
                                                idAbsencePersonnel VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    typeAbsence VARCHAR(30) NULL,
    dateDebut DATETIME NOT NULL,
    dateFin DATETIME NOT NULL,
    motif VARCHAR(255),
    estJustifiee TINYINT(1) DEFAULT 0,
    idDemandeConge VARCHAR(50) NULL,
    impactPaieRetenue TINYINT(1) DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idAbsencePersonnel),
    INDEX idx_absence_periode (idPersonnel, dateDebut),
    INDEX idx_absence_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_abs_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_abs_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_abs_conge FOREIGN KEY (idDemandeConge) REFERENCES DemandeConge(idDemandeConge),
    CONSTRAINT chk_grh_abs_dates CHECK (dateFin >= dateDebut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS PointagePersonnel (
                                                 idPointage VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    dateJour DATE NOT NULL,
    heureArrivee TIME NULL,
    heureDepart TIME NULL,
    statut VARCHAR(20) DEFAULT 'PRESENT',
    source VARCHAR(20) DEFAULT 'MANUEL',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    pointage_actif VARCHAR(70) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                    CONCAT(idPersonnel, '_', CAST(dateJour AS CHAR)) END) VIRTUAL,
    PRIMARY KEY (idPointage),
    UNIQUE KEY uq_pointage_actif (pointage_actif, idEcole),
    INDEX idx_pointage_jour (idEcole, dateJour),
    INDEX idx_pointage_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_pointage_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_pointage_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 16. GRH — CARRIÈRE, PERFORMANCE, FORMATION, RECRUTEMENT
-- ==============================================================

CREATE TABLE IF NOT EXISTS HistoriqueCarriere (
                                                  idHistorique VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idContrat VARCHAR(50) NULL,
    typeEvenement VARCHAR(50) NOT NULL,
    dateEvenement DATE NOT NULL,
    idPosteAvant VARCHAR(50) NULL,
    idPosteApres VARCHAR(50) NULL,
    niveauSanction VARCHAR(30) NULL,
    description TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idHistorique),
    INDEX idx_carriere_personnel (idPersonnel, dateEvenement),
    INDEX idx_carriere_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_carriere_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_carriere_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_carriere_contrat FOREIGN KEY (idContrat) REFERENCES ContratPersonnel(idContrat),
    CONSTRAINT fk_grh_carriere_avant FOREIGN KEY (idPosteAvant) REFERENCES Poste(idPoste),
    CONSTRAINT fk_grh_carriere_apres FOREIGN KEY (idPosteApres) REFERENCES Poste(idPoste)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS EvaluationPersonnel (
    idEvaluation VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idEvaluateur VARCHAR(50) NOT NULL,
    anneeScolaire VARCHAR(9) NOT NULL,
    noteGlobale DECIMAL(4,2) NULL,
    pointsForts TEXT,
    axesProgres TEXT,
    objectifs TEXT,
    dateEntretien DATE NULL,
    statut VARCHAR(20) DEFAULT 'BROUILLON',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    evaluation_actif VARCHAR(130) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN
                                                       CONCAT(idPersonnel, '_', anneeScolaire, '_', idEvaluateur) END) VIRTUAL,
    PRIMARY KEY (idEvaluation),
    UNIQUE KEY uq_evaluation_actif (evaluation_actif, idEcole),
    INDEX idx_evaluation_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_eval_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_eval_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_eval_evaluateur FOREIGN KEY (idEvaluateur) REFERENCES Personnel(idPersonnel),
    CONSTRAINT chk_grh_eval_note CHECK (noteGlobale IS NULL OR noteGlobale BETWEEN 0 AND 20)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS FormationPersonnel (
                                                  idFormation VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    intitule VARCHAR(200) NOT NULL,
    organisme VARCHAR(150) NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NULL,
    dureeHeures DECIMAL(6,1) NULL,
    cout DECIMAL(12,2) DEFAULT 0.00,
    estCertifiante TINYINT(1) DEFAULT 0,
    idDocument VARCHAR(50) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idFormation),
    INDEX idx_formation_personnel (idPersonnel),
    INDEX idx_formation_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_formation_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_formation_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_grh_formation_doc FOREIGN KEY (idDocument) REFERENCES DocumentPersonnel(idDocument),
    CONSTRAINT chk_grh_formation_dates CHECK (dateFin IS NULL OR dateFin >= dateDebut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Candidature (
                                           idCandidature VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPoste VARCHAR(50) NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NULL,
    email VARCHAR(150) NULL,
    telephone VARCHAR(30) NULL,
    dateCandidature DATE NOT NULL,
    etape VARCHAR(30) DEFAULT 'RECUE',
    cheminCV VARCHAR(500) NULL,
    notes TEXT,
    idPersonnelEmbauche VARCHAR(50) NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idCandidature),
    INDEX idx_candidature_etape (idEcole, etape),
    INDEX idx_candidature_sync (idEcole, updated_at, version),
    CONSTRAINT fk_grh_candidature_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_grh_candidature_poste FOREIGN KEY (idPoste) REFERENCES Poste(idPoste),
    CONSTRAINT fk_grh_candidature_pers FOREIGN KEY (idPersonnelEmbauche) REFERENCES Personnel(idPersonnel)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 17. ÉLÈVES & INSCRIPTIONS
-- ==============================================================

CREATE TABLE IF NOT EXISTS Eleve (
                                     idEleve VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NULL,
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
    idUtilisateur_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN idUtilisateur END) VIRTUAL,
    PRIMARY KEY (idEleve),
    UNIQUE KEY uq_eleve_matricule_actif (matricule_actif, idEcole),
    INDEX idx_eleve_sync (idEcole, updated_at, version),
    CONSTRAINT fk_eleve_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_eleve_user FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS Parent (
                                      idParent VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idUtilisateur VARCHAR(50) NULL,
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
-- 18. PÉDAGOGIE
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
    INDEX idx_per_sync (idEcole, updated_at, version),
    CONSTRAINT fk_per_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_per_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS PeriodeEvaluation (
    idPeriode VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    idPeriodeParent VARCHAR(50) NULL,
    codePeriode VARCHAR(20) NOT NULL,
    libelle VARCHAR(50) NOT NULL,
    typePeriode ENUM('SEQUENCE', 'TRIMESTRE', 'SEMESTRE', 'ANNEE') NOT NULL,
    numeroOrdre INT NOT NULL,
    poids DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    estCloturee TINYINT(1) NOT NULL DEFAULT 0,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codePeriode END) VIRTUAL,
    PRIMARY KEY (idPeriode),
    UNIQUE KEY uq_periode_code_annee_actif (code_actif, idAnnee, idEcole),
    INDEX idx_periode_sync (idEcole, updated_at, version),
    INDEX idx_periode_parent (idPeriodeParent),
    INDEX idx_periode_annee (idAnnee),
    CONSTRAINT fk_periode_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_periode_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee),
    CONSTRAINT fk_periode_parent FOREIGN KEY (idPeriodeParent) REFERENCES PeriodeEvaluation(idPeriode),
    CONSTRAINT chk_periode_dates CHECK (dateFin >= dateDebut),
    CONSTRAINT chk_periode_poids CHECK (poids > 0)
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
    CONSTRAINT fk_eval_periode FOREIGN KEY (idPeriode) REFERENCES PeriodeEvaluation(idPeriode)
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
-- 19. FINANCES
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
-- 20. ABSENCES ÉLÈVES
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
-- 21. EMPLOI DU TEMPS
-- ==============================================================

CREATE TABLE IF NOT EXISTS CreneauHoraire (
                                              idCreneau VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    codeCreneau VARCHAR(20) NOT NULL,
    jourSemaine ENUM('LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE') NOT NULL,
    heureDebut TIME NOT NULL,
    heureFin TIME NOT NULL,
    estPause TINYINT(1) DEFAULT 0,
    ordreAffichage INT DEFAULT 1,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeCreneau END) VIRTUAL,
    PRIMARY KEY (idCreneau),
    UNIQUE KEY uq_creneau_code_actif (code_actif, idEcole),
    INDEX idx_creneau_sync (idEcole, updated_at, version),
    CONSTRAINT fk_creneau_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS EmploiDuTemps (
                                             idEmploiDuTemps VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    codeEDT VARCHAR(20) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    dateDebutValidite DATE NOT NULL,
    dateFinValidite DATE NOT NULL,
    estActif TINYINT(1) DEFAULT 0,
    statut ENUM('BROUILLON', 'VALIDE', 'ARCHIVE') DEFAULT 'BROUILLON',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    code_actif VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN codeEDT END) VIRTUAL,
    PRIMARY KEY (idEmploiDuTemps),
    UNIQUE KEY uq_edt_code_actif (code_actif, idEcole),
    INDEX idx_edt_sync (idEcole, updated_at, version),
    CONSTRAINT fk_edt_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_edt_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SousGroupeClasse (
                                                idSousGroupe VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idClasse VARCHAR(50) NOT NULL,
    nomSousGroupe VARCHAR(50) NOT NULL,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    nom_actif VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nomSousGroupe END) VIRTUAL,
    PRIMARY KEY (idSousGroupe),
    UNIQUE KEY uq_sg_classe_actif (idClasse, nom_actif, idEcole),
    INDEX idx_sg_sync (idEcole, updated_at, version),
    CONSTRAINT fk_sg_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_sg_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SeanceCours (
                                           idSeance VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idEmploiDuTemps VARCHAR(50) NOT NULL,
    idClasse VARCHAR(50) NOT NULL,
    idSousGroupe VARCHAR(50) NULL,
    idMatiere VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idSalle VARCHAR(50) NULL,
    idCreneau VARCHAR(50) NOT NULL,
    typeSeance ENUM('COURS', 'TD', 'TP', 'RATTRAPAGE', 'EXAMEN') DEFAULT 'COURS',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    pers_creneau_actif VARCHAR(105) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN CONCAT(idPersonnel, '-', idCreneau) END) VIRTUAL,
    salle_creneau_actif VARCHAR(105) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL AND idSalle IS NOT NULL THEN CONCAT(idSalle, '-', idCreneau) END) VIRTUAL,
    classe_creneau_actif VARCHAR(105) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL AND idSousGroupe IS NULL THEN CONCAT(idClasse, '-', idCreneau) END) VIRTUAL,
    PRIMARY KEY (idSeance),
    UNIQUE KEY uq_seance_pers (pers_creneau_actif, idEmploiDuTemps),
    UNIQUE KEY uq_seance_salle (salle_creneau_actif, idEmploiDuTemps),
    UNIQUE KEY uq_seance_classe (classe_creneau_actif, idEmploiDuTemps),
    INDEX idx_seance_sync (idEcole, updated_at, version),
    CONSTRAINT fk_sceance_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_sc_edt FOREIGN KEY (idEmploiDuTemps) REFERENCES EmploiDuTemps(idEmploiDuTemps) ON DELETE CASCADE,
    CONSTRAINT fk_sc_classe FOREIGN KEY (idClasse) REFERENCES Classe(idClasse),
    CONSTRAINT fk_sc_sg FOREIGN KEY (idSousGroupe) REFERENCES SousGroupeClasse(idSousGroupe),
    CONSTRAINT fk_sc_matiere FOREIGN KEY (idMatiere) REFERENCES Matiere(idMatiere),
    CONSTRAINT fk_sc_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_sc_salle FOREIGN KEY (idSalle) REFERENCES Salle(idSalle),
    CONSTRAINT fk_sc_creneau FOREIGN KEY (idCreneau) REFERENCES CreneauHoraire(idCreneau)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS IndisponibiliteEnseignant (
                                                         idIndisponibilite VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idPersonnel VARCHAR(50) NOT NULL,
    idAnnee VARCHAR(50) NOT NULL,
    idCreneau VARCHAR(50) NULL,
    jourSemaine ENUM('LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE') NULL,
    dateSpecifique DATE NULL,
    motif VARCHAR(255),
    typeContrainte ENUM('INFRANCHISSABLE', 'PREFERENCE_EVITER') DEFAULT 'INFRANCHISSABLE',
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idIndisponibilite),
    INDEX idx_indisp_sync (idEcole, updated_at, version),
    CONSTRAINT fk_indisp_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_indisp_pers FOREIGN KEY (idPersonnel) REFERENCES Personnel(idPersonnel) ON DELETE CASCADE,
    CONSTRAINT fk_indisp_annee FOREIGN KEY (idAnnee) REFERENCES AnneeScolaire(idAnnee),
    CONSTRAINT fk_indisp_creneau FOREIGN KEY (idCreneau) REFERENCES CreneauHoraire(idCreneau)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ExceptionSeance (
                                               idException VARCHAR(50) NOT NULL,
    idEcole VARCHAR(50) NOT NULL,
    idSeance VARCHAR(50) NOT NULL,
    dateException DATE NOT NULL,
    typeException ENUM('ANNULE', 'REMPLACE', 'DEPLACE', 'SALLE_CHANGEMENT') NOT NULL,
    idRemplacant VARCHAR(50) NULL,
    idNouvelleSalle VARCHAR(50) NULL,
    motif TEXT,
    version BIGINT DEFAULT 1,
    deleted_at TIMESTAMP(3) NULL DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (idException),
    INDEX idx_exc_sync (idEcole, updated_at, version),
    CONSTRAINT fk_exc_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole),
    CONSTRAINT fk_exc_seance FOREIGN KEY (idSeance) REFERENCES SeanceCours(idSeance) ON DELETE CASCADE,
    CONSTRAINT fk_exc_remplacant FOREIGN KEY (idRemplacant) REFERENCES Personnel(idPersonnel),
    CONSTRAINT fk_exc_salle FOREIGN KEY (idNouvelleSalle) REFERENCES Salle(idSalle)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 22. SYNCHRONISATION
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
    CONSTRAINT fk_syncclient_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole)
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
    userid VARCHAR(50) NOT NULL,
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
-- 23. AUDIT
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
    CONSTRAINT fk_arbac_act FOREIGN KEY (idUtilisateurAction) REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT fk_arbac_cib FOREIGN KEY (idUtilisateurCible) REFERENCES Utilisateur(idUtilisateur) ON DELETE SET NULL,
    CONSTRAINT fk_arbac_ecole FOREIGN KEY (idEcoleConcernee) REFERENCES Ecole(idEcole) ON DELETE SET NULL
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
    CONSTRAINT fk_log_usr FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(idUtilisateur),
    CONSTRAINT fk_log_ecole FOREIGN KEY (idEcole) REFERENCES Ecole(idEcole) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- 24. FK DIFFÉRÉE : Departement.idResponsable -> Personnel
-- ==============================================================


-- ALTER TABLE Departement
--     ADD CONSTRAINT fk_grh_dep_resp
--         FOREIGN KEY (idResponsable) REFERENCES Personnel(idPersonnel) ON DELETE SET NULL,
--     ADD CONSTRAINT fk_grh_dep_resp_adj
--         FOREIGN KEY (idResponsableAdjoint) REFERENCES Personnel(idPersonnel) ON DELETE SET NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- FIN DU SCRIPT
-- Nombre total de tables : 51
-- =======================================================================================================================================================