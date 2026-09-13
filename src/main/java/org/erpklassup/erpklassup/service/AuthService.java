package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.*;
import org.erpklassup.erpklassup.models.TentativeConnexion;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.erpklassup.erpklassup.util.TotpUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AuthService {

    private static final int MAX_TENTATIVES = 5;
    private static final int DUREE_VERROUILLAGE_MINUTES = 15;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();
    private final TentativeConnexionDAO tentativeConnexionDAO = new TentativeConnexionDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // ✅ Plus de "final" ni de "new" ici : instanciation paresseuse
    private AuditService auditService;

    /** Récupère l'AuditService à la demande, après construction complète. */
    private AuditService audit() {
        if (auditService == null) {
            auditService = new AuditService();
        }
        return auditService;
    }

//    public ResultatConnexion seConnecter(Stage stage, String username, String motDePasse, String code2FA, String idEcole, String ipAdresse, String userAgent, String nomApp) {
//
//        Optional<Utilisateur> userOpt = utilisateurDAO.findByUsername(username, idEcole);
//
//        // 1. Utilisateur non trouvé
//        if (userOpt.isEmpty()) {
//            enregistrerSecurite(username, ipAdresse, userAgent, false, "UTILISATEUR_INEXISTANT",
//                    "AUTHENTIFICATION", "ECHEC_CONNEXION", "Tentative avec un identifiant inconnu : " + username);
//
//            afficherToast(stage, () -> ToastNotification.erreur(stage, "Identifiant ou mot de passe incorrect"));
//            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
//        }
//
//        Utilisateur user = userOpt.get();
//
//        // 2. Compte désactivé
//        if (!user.isEstActif()) {
//            enregistrerSecurite(username, ipAdresse, userAgent, false, "COMPTE_DESACTIVE",
//                    "AUTHENTIFICATION", "ECHEC_CONNEXION", "Connexion refusée — Compte désactivé : " + username);
//
//            afficherToast(stage, () -> ToastNotification.avertissement(stage, "Ce compte a été désactivé"));
//            return new ResultatConnexion(false, "Ce compte est désactivé");
//        }
//
//        // 3. Compte verrouillé
//        if (user.isCompteVerrouille()) {
//            if (user.getVerrouilleJusqua() != null && user.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
//                enregistrerSecurite(username, ipAdresse, userAgent, false, "COMPTE_VERROUILLE",
//                        "AUTHENTIFICATION", "ECHEC_CONNEXION", "Connexion refusée — Compte verrouillé : " + username);
//
//                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé. Réessayez plus tard."));
//                return new ResultatConnexion(false, "Compte verrouillé. Réessayez plus tard.");
//            } else {
//                utilisateurDAO.deverrouillerCompte(user.getIdUtilisateur());
//                user.setCompteVerrouille(false);
//            }
//        }
//
//        // 4. Mot de passe incorrect
//        if (!PasswordService.verifier(motDePasse, user.getPasswordHash())) {
//            utilisateurDAO.incrementerTentativesEchec(user.getIdUtilisateur());
//            int tentativesEchouees = user.getNombreTentativesEchec() + 1;
//
//            if (tentativesEchouees >= MAX_TENTATIVES) {
//                utilisateurDAO.verrouillerCompte(user.getIdUtilisateur(), DUREE_VERROUILLAGE_MINUTES);
//
//                enregistrerSecurite(username, ipAdresse, userAgent, false, "SEUIL_ECHECS_ATTEINT",
//                        "AUTHENTIFICATION", "COMPTE_VERROUILLE", "Compte verrouillé après " + MAX_TENTATIVES + " échecs : " + username);
//
//                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé après trop d'échecs"));
//                return new ResultatConnexion(false, "Compte verrouillé");
//            } else {
//                int restantes = MAX_TENTATIVES - tentativesEchouees;
//
//                enregistrerSecurite(username, ipAdresse, userAgent, false, "MOT_DE_PASSE_INCORRECT",
//                        "AUTHENTIFICATION", "ECHEC_CONNEXION", "Mot de passe erroné pour " + username + " (" + restantes + " essai(s) restant(s))");
//
//                afficherToast(stage, () -> ToastNotification.avertissement(stage, "Mot de passe incorrect. " + restantes + " tentative(s) restante(s)"));
//                return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
//            }
//        }
//
//        // Mot de passe correct : Réinitialisation du compteur d'échecs
//        utilisateurDAO.reinitialiserTentativesEchec(user.getIdUtilisateur());
//
//        if (user.isDoitConfigurer2FA() && !user.isDeuxFacteursActive()) {
//            enregistrerSecurite(username, ipAdresse, userAgent, true, null,
//                    "AUTHENTIFICATION", "SETUP_2FA_REQUIS",
//                    "Configuration 2FA obligatoire pour " + user.getNomComplet());
//
//            afficherToast(stage, () -> ToastNotification.info(stage,
//                    "Vous devez configurer la 2FA avant de continuer."));
//
//            return ResultatConnexion.requiertSetup2FA(user,
//                    "Configuration 2FA obligatoire");
//        }
//
//        // 5. Validation du Double Facteur (2FA)
//        if (user.isDeuxFacteursActive()) {
//            if (code2FA == null || code2FA.isBlank()) {
//                enregistrerSecurite(username, ipAdresse, userAgent, true, null,
//                        "AUTHENTIFICATION", "ATTENTE_2FA", "Mot de passe valide, en attente du code 2FA pour " + user.getNomComplet());
//
//                afficherToast(stage, () -> ToastNotification.info(stage, "Veuillez saisir votre code d'authentification."));
//
//                // Retourne un statut indiquant au LoginController d'afficher le champ 2FA
//                return ResultatConnexion.requiert2FA("Authentification 2FA requise");
//            }
//
//            // Vérification du code via TotpUtil
//            boolean codeValide = TotpUtil.verifierCode(user.getSecret2FA(), code2FA);
//
//            // Fallback : test via code de secours si le TOTP échoue
//            if (!codeValide) {
//                codeValide = utilisateurDAO.validerEtConsommerCodeSecours(user.getIdUtilisateur(), code2FA);
//            }
//
//            if (!codeValide) {
//                enregistrerSecurite(username, ipAdresse, userAgent, false, "CODE_2FA_INCORRECT",
//                        "AUTHENTIFICATION", "ECHEC_CONNEXION", "Code 2FA invalide fourni pour " + username);
//
//                afficherToast(stage, () -> ToastNotification.erreur(stage, "Code 2FA invalide ou expiré"));
//                return ResultatConnexion.requiert2FA("Code de vérification incorrect");
//            }
//        }
//
//        // 6. Succès final : Enregistrement Audit
//
//        // 7. Chargement des permissions effectives
//        Set<String> permissionsEffectives = permissionDAO.findPermissionsEffectivesPourUtilisateur(user.getIdUtilisateur());
//
//        // 8. Enregistrement de la session BDD
//        String idSession = UUID.randomUUID().toString();
//        String refreshTokenHash = UUID.randomUUID().toString();
//        String familleToken = UUID.randomUUID().toString();
//
//        sessionUtilisateurDAO.creerSession(
//                idSession,
//                user.getIdUtilisateur(),
//                idEcole,
//                refreshTokenHash,
//                familleToken,
//                ipAdresse,
//                userAgent,
//                nomApp
//        );
//
//        // 9. Démarrage de la session mémoire
//        sessionManager.demarrerSession(idSession, user, permissionsEffectives);
//        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());
//
//        // 10. Toast Notification de succès
//        enregistrerSecurite(username, ipAdresse, userAgent, true, null,
//                "AUTHENTIFICATION", "CONNEXION", "Connexion réussie pour " + user.getNomComplet());
//
//        afficherToast(stage, () -> ToastNotification.succes(stage, "Bienvenue " + user.getPrenom() + " !"));
//
//        return new ResultatConnexion(true, "Connexion réussie", user, permissionsEffectives);
//    }
//
//    // Surcharge pour conserver la compatibilité sans code 2FA
//    public ResultatConnexion seConnecter(Stage stage, String username, String motDePasse, String idEcole, String ipAdresse, String userAgent, String nomApp) {
//        return seConnecter(stage, username, motDePasse, "", idEcole, ipAdresse, userAgent, nomApp);
//    }


    public ResultatConnexion seConnecter(Stage stage, String username, String motDePasse, String code2FA,
                                         String idEcole, String ipAdresse, String userAgent, String nomApp) {

        Optional<Utilisateur> userOpt = utilisateurDAO.findByUsername(username, idEcole);

        // 1. Utilisateur non trouvé
        if (userOpt.isEmpty()) {
            enregistrerSecurite(username, ipAdresse, userAgent, false, "UTILISATEUR_INEXISTANT",
                    "AUTHENTIFICATION", "ECHEC_CONNEXION",
                    "Tentative avec un identifiant inconnu : " + username);

            afficherToast(stage, () -> ToastNotification.erreur(stage, "Identifiant ou mot de passe incorrect"));
            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
        }

        Utilisateur user = userOpt.get();

        // 2. Compte désactivé
        if (!user.isEstActif()) {
            enregistrerSecurite(username, ipAdresse, userAgent, false, "COMPTE_DESACTIVE",
                    "AUTHENTIFICATION", "ECHEC_CONNEXION",
                    "Connexion refusée — Compte désactivé : " + username);

            afficherToast(stage, () -> ToastNotification.avertissement(stage, "Ce compte a été désactivé"));
            return new ResultatConnexion(false, "Ce compte est désactivé");
        }

        // 3. Compte verrouillé
        if (user.isCompteVerrouille()) {
            if (user.getVerrouilleJusqua() != null && user.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
                enregistrerSecurite(username, ipAdresse, userAgent, false, "COMPTE_VERROUILLE",
                        "AUTHENTIFICATION", "ECHEC_CONNEXION",
                        "Connexion refusée — Compte verrouillé : " + username);

                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé. Réessayez plus tard."));
                return new ResultatConnexion(false, "Compte verrouillé. Réessayez plus tard.");
            } else {
                utilisateurDAO.deverrouillerCompte(user.getIdUtilisateur());
                user.setCompteVerrouille(false);
            }
        }

        // 4. Mot de passe incorrect
        if (!PasswordService.verifier(motDePasse, user.getPasswordHash())) {
            utilisateurDAO.incrementerTentativesEchec(user.getIdUtilisateur());
            int tentativesEchouees = user.getNombreTentativesEchec() + 1;

            if (tentativesEchouees >= MAX_TENTATIVES) {
                utilisateurDAO.verrouillerCompte(user.getIdUtilisateur(), DUREE_VERROUILLAGE_MINUTES);

                enregistrerSecurite(username, ipAdresse, userAgent, false, "SEUIL_ECHECS_ATTEINT",
                        "AUTHENTIFICATION", "COMPTE_VERROUILLE",
                        "Compte verrouillé après " + MAX_TENTATIVES + " échecs : " + username);

                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé après trop d'échecs"));
                return new ResultatConnexion(false, "Compte verrouillé");
            } else {
                int restantes = MAX_TENTATIVES - tentativesEchouees;

                enregistrerSecurite(username, ipAdresse, userAgent, false, "MOT_DE_PASSE_INCORRECT",
                        "AUTHENTIFICATION", "ECHEC_CONNEXION",
                        "Mot de passe erroné pour " + username + " (" + restantes + " essai(s) restant(s))");

                afficherToast(stage, () -> ToastNotification.avertissement(stage,
                        "Mot de passe incorrect. " + restantes + " tentative(s) restante(s)"));
                return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
            }
        }

        // Mot de passe correct : Réinitialisation du compteur d'échecs
        utilisateurDAO.reinitialiserTentativesEchec(user.getIdUtilisateur());

        // ==========================================
        // 5. GESTION DU 2FA — ORDRE IMPORTANT
        // ==========================================

        // ✅ CAS A : L'utilisateur doit CONFIGURER sa 2FA (forcé par admin, pas encore activée)
        if (user.isDoitConfigurer2FA() && !user.isDeuxFacteursActive()) {
            enregistrerSecurite(username, ipAdresse, userAgent, true, null,
                    "AUTHENTIFICATION", "SETUP_2FA_REQUIS",
                    "Configuration 2FA obligatoire pour " + user.getNomComplet());

            afficherToast(stage, () -> ToastNotification.info(stage,
                    "Vous devez configurer la 2FA avant de continuer."));

            return ResultatConnexion.requiertSetup2FA(user,
                    "Configuration 2FA obligatoire");
        }

        // ✅ CAS B : L'utilisateur a DÉJÀ une 2FA active → demander le code
        if (user.isDeuxFacteursActive()) {
            if (code2FA == null || code2FA.isBlank()) {
                enregistrerSecurite(username, ipAdresse, userAgent, true, null,
                        "AUTHENTIFICATION", "ATTENTE_2FA",
                        "Mot de passe valide, en attente du code 2FA pour " + user.getNomComplet());

                afficherToast(stage, () -> ToastNotification.info(stage,
                        "Veuillez saisir votre code d'authentification."));

                return ResultatConnexion.requiert2FA("Authentification 2FA requise");
            }

            // Vérification du code TOTP via TotpUtil
            boolean codeValide = TotpUtil.verifierCode(user.getSecret2FA(), code2FA);

            // Fallback : test via code de secours si le TOTP échoue
            if (!codeValide) {
                codeValide = utilisateurDAO.validerEtConsommerCodeSecours(
                        user.getIdUtilisateur(), code2FA);
            }

            if (!codeValide) {
                enregistrerSecurite(username, ipAdresse, userAgent, false, "CODE_2FA_INCORRECT",
                        "AUTHENTIFICATION", "ECHEC_CONNEXION",
                        "Code 2FA invalide fourni pour " + username);

                afficherToast(stage, () -> ToastNotification.erreur(stage,
                        "Code 2FA invalide ou expiré"));

                return ResultatConnexion.requiert2FA("Code de vérification incorrect");
            }
        }

        // ==========================================
        // 6. SUCCÈS FINAL
        // ==========================================

        // Chargement des permissions effectives
        Set<String> permissionsEffectives =
                permissionDAO.findPermissionsEffectivesPourUtilisateur(user.getIdUtilisateur());

        // Enregistrement de la session BDD
        String idSession = UUID.randomUUID().toString();
        String refreshTokenHash = UUID.randomUUID().toString();
        String familleToken = UUID.randomUUID().toString();

        sessionUtilisateurDAO.creerSession(
                idSession,
                user.getIdUtilisateur(),
                idEcole,
                refreshTokenHash,
                familleToken,
                ipAdresse,
                userAgent,
                nomApp
        );

        // Démarrage de la session mémoire
        sessionManager.demarrerSession(idSession, user, permissionsEffectives);
        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());

        // ✅ Audit APRÈS demarrerSession (pour que la session soit active)
        enregistrerSecurite(username, ipAdresse, userAgent, true, null,
                "AUTHENTIFICATION", "CONNEXION",
                "Connexion réussie pour " + user.getNomComplet());

        // Toast de succès
        afficherToast(stage, () -> ToastNotification.succes(stage,
                "Bienvenue " + user.getPrenom() + " !"));

        return new ResultatConnexion(true, "Connexion réussie", user, permissionsEffectives);
    }

    public void seDeconnecter(Stage stage) {
        if (sessionManager.estConnecte()) {
            audit().tracerActionAsync("AUTHENTIFICATION", "DECONNEXION",
                    "Déconnexion de l'utilisateur " + sessionManager.getUtilisateurCourant().getNomComplet());
            sessionManager.terminerSession();
            afficherToast(stage, () -> ToastNotification.info(stage, "Vous avez été déconnecté"));
        }
    }



    private void enregistrerSecurite(String username, String ip, String userAgent, boolean succes, String motifEchec,
                                     String categorieAudit, String actionAudit, String detailsAudit) {

        // 1. Enregistrement asynchrone dans TentativeConnexion (Table sécurité brute)
        AppExecutor.get().submit(() -> {
            try {
                TentativeConnexion tentative = new TentativeConnexion();
                tentative.setIdTentative(UUID.randomUUID().toString());
                tentative.setUsernameSaisi(username != null ? username : "INCONNU");
                tentative.setAdresseIp(ip != null ? ip : "127.0.0.1");
                tentative.setUserAgent(userAgent);
                tentative.setSucces(succes);
                tentative.setMotifEchec(motifEchec);

                tentativeConnexionDAO.enregistrer(tentative);
            } catch (Exception e) {
                System.err.println("❌ Erreur enregistrement TentativeConnexion : " + e.getMessage());
                e.printStackTrace();
            }
        });

        try {
            audit().tracerActionAsync(categorieAudit, actionAudit, detailsAudit);
        } catch (Exception e) {
            System.err.println("❌ Erreur AuditService : " + e.getMessage());
        }
    }


    private void afficherToast(Stage stage, Runnable action) {
        if (stage == null) return;
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static class ResultatConnexion {
        private final boolean succes;
        private final boolean requiert2FA;
        private final boolean doitConfigurer2FA;   // ✅ AJOUT
        private final String message;
        private final Utilisateur utilisateur;
        private final Set<String> permissions;

        // ==========================================
        // CONSTRUCTEURS PUBLICS
        // ==========================================

        /** Échec simple (message seul) */
        public ResultatConnexion(boolean succes, String message) {
            this(succes, false, false, message, null, null);
        }

        /** Succès avec utilisateur + permissions */
        public ResultatConnexion(boolean succes, String message,
                                 Utilisateur user, Set<String> permissions) {
            this(succes, false, false, message, user, permissions);
        }

        // ==========================================
        // CONSTRUCTEUR PRIVÉ (central)
        // ==========================================
        private ResultatConnexion(boolean succes, boolean requiert2FA, boolean doitConfigurer2FA,
                                  String message, Utilisateur user, Set<String> permissions) {
            this.succes = succes;
            this.requiert2FA = requiert2FA;
            this.doitConfigurer2FA = doitConfigurer2FA;   // ✅ INITIALISATION (corrige l'erreur 2)
            this.message = message;
            this.utilisateur = user;
            this.permissions = permissions;
        }

        // ==========================================
        // FACTORY METHODS
        // ==========================================

        /** L'utilisateur doit saisir un code 2FA (2FA déjà configurée) */
        public static ResultatConnexion requiert2FA(String message) {
            return new ResultatConnexion(false, true, false, message, null, null);
        }

        /** L'utilisateur doit CONFIGURER sa 2FA (forcé par admin) */
        public static ResultatConnexion requiertSetup2FA(Utilisateur user, String message) {
            return new ResultatConnexion(false, false, true, message, user, null);
        }

        // ==========================================
        // GETTERS
        // ==========================================
        public boolean isSucces() { return succes; }
        public boolean isRequiert2FA() { return requiert2FA; }
        public boolean isDoitConfigurer2FA() { return doitConfigurer2FA; }   // ✅ AJOUT
        public String getMessage() { return message; }
        public Utilisateur getUtilisateur() { return utilisateur; }
        public Set<String> getPermissions() { return permissions; }
    }

    public ResultatConnexion validerEtFinaliser2FA(Stage stage, Utilisateur user, String codeSaisi, String idEcole, String ipAdresse, String userAgent, String nomApp) {
        Security2FAService security2FAService = new Security2FAService();

        // 1. Validation du code TOTP à 6 chiffres
        boolean codeValide = false;
        try {
            int codeInt = Integer.parseInt(codeSaisi.trim());
            codeValide = security2FAService.verifierCodeTOTP(user.getSecret2FA(), codeInt);
        } catch (NumberFormatException e) {
            codeValide = false;
        }

        // 2. Si invalide, on tente de vérifier s'il s'agit d'un code de secours (table CodeSecours2FA)
        if (!codeValide) {
            // ✅ APRÈS
            codeValide = new CodeSecours2FADAO().verifierEtConsommer(
                    user.getIdUtilisateur(), codeSaisi.trim());
        }

        // 3. Traitement si le code demeure invalide
        if (!codeValide) {
            enregistrerSecurite(user.getUsername(), ipAdresse, userAgent, false, "CODE_2FA_INCORRECT",
                    "AUTHENTIFICATION", "ECHEC_2FA", "Code 2FA invalide saisi par : " + user.getUsername());

            afficherToast(stage, () -> ToastNotification.erreur(stage, "Code 2FA incorrect ou expiré."));
            return new ResultatConnexion(false, "Code 2FA invalide.");
        }

        // 4. Enregistrement de l'audit et finalisation (Reprise des étapes 6 à 10)
        enregistrerSecurite(user.getUsername(), ipAdresse, userAgent, true, null,
                "AUTHENTIFICATION", "CONNEXION_2FA", "Connexion 2FA réussie pour " + user.getNomComplet());

        Set<String> permissionsEffectives = permissionDAO.findPermissionsEffectivesPourUtilisateur(user.getIdUtilisateur());

        String idSession = UUID.randomUUID().toString();
        String refreshTokenHash = UUID.randomUUID().toString();
        String familleToken = UUID.randomUUID().toString();

        sessionUtilisateurDAO.creerSession(
                idSession,
                user.getIdUtilisateur(),
                idEcole,
                refreshTokenHash,
                familleToken,
                ipAdresse,
                userAgent,
                nomApp
        );

        sessionManager.demarrerSession(idSession, user, permissionsEffectives);
        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());

        afficherToast(stage, () -> ToastNotification.succes(stage, "Bienvenue " + user.getPrenom() + " !"));

        return new ResultatConnexion(true, "Connexion réussie", user, permissionsEffectives);
    }
}
