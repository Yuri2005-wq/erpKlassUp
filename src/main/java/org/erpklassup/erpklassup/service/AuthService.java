package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.dao.TentativeConnexionDAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.models.TentativeConnexion;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.util.ToastNotification;

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

    public ResultatConnexion seConnecter(Stage stage, String username, String motDePasse, String idEcole, String ipAdresse, String userAgent, String nomApp) {

        Optional<Utilisateur> userOpt = utilisateurDAO.findByUsername(username, idEcole);

        // 1. Utilisateur non trouvé
        if (userOpt.isEmpty()) {
            enregistrerSecurite(username, ipAdresse, userAgent, false, "UTILISATEUR_INEXISTANT",
                    "AUTHENTIFICATION", "ECHEC_CONNEXION", "Tentative avec un identifiant inconnu : " + username);

            afficherToast(stage, () -> ToastNotification.erreur(stage, "Identifiant ou mot de passe incorrect"));
            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
        }

        Utilisateur user = userOpt.get();

        // 2. Compte désactivé
        if (!user.isEstActif()) {
            enregistrerSecurite(username, ipAdresse, userAgent, false, "COMPTE_DESACTIVE",
                    "AUTHENTIFICATION", "ECHEC_CONNEXION", "Connexion refusée — Compte désactivé : " + username);

            afficherToast(stage, () -> ToastNotification.avertissement(stage, "Ce compte a été désactivé"));
            return new ResultatConnexion(false, "Ce compte est désactivé");
        }

        // 3. Compte verrouillé
        if (user.isCompteVerrouille()) {
            if (user.getVerrouilleJusqua() != null && user.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
                enregistrerSecurite(username, ipAdresse, userAgent, false, "COMPTE_VERROUILLE",
                        "AUTHENTIFICATION", "ECHEC_CONNEXION", "Connexion refusée — Compte verrouillé : " + username);

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
                        "AUTHENTIFICATION", "COMPTE_VERROUILLE", "Compte verrouillé après " + MAX_TENTATIVES + " échecs : " + username);

                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé après trop d'échecs"));
                return new ResultatConnexion(false, "Compte verrouillé");
            } else {
                int restantes = MAX_TENTATIVES - tentativesEchouees;

                enregistrerSecurite(username, ipAdresse, userAgent, false, "MOT_DE_PASSE_INCORRECT",
                        "AUTHENTIFICATION", "ECHEC_CONNEXION", "Mot de passe erroné pour " + username + " (" + restantes + " essai(s) restant(s))");

                afficherToast(stage, () -> ToastNotification.avertissement(stage, "Mot de passe incorrect. " + restantes + " tentative(s) restante(s)"));
                return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
            }
        }

        // 5. Succès de l'authentification
        utilisateurDAO.reinitialiserTentativesEchec(user.getIdUtilisateur());

        // 6. Enregistrement de la tentative réussie + Audit
        enregistrerSecurite(username, ipAdresse, userAgent, true, null,
                "AUTHENTIFICATION", "CONNEXION", "Connexion réussie pour " + user.getNomComplet());

        // 7. Chargement des permissions effectives
        Set<String> permissionsEffectives = permissionDAO.findPermissionsEffectivesPourUtilisateur(user.getIdUtilisateur());

        // 8. Enregistrement de la session BDD
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

        // 9. Démarrage de la session mémoire
        sessionManager.demarrerSession(idSession, user, permissionsEffectives);
        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());

        // 10. Toast Notification
        afficherToast(stage, () -> ToastNotification.succes(stage, "Bienvenue " + user.getPrenom() + " !"));

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

    /**
     * Méthode utilitaire asynchrone construisant le modèle TentativeConnexion
     * et appelant le DAO ainsi que le service d'audit.
     */
    private void enregistrerSecurite(String username, String ip, String userAgent, boolean succes, String motifEchec,
                                     String categorieAudit, String actionAudit, String detailsAudit) {
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
                System.err.println("Erreur enregistrement sécurité : " + e.getMessage());
            }
        });

        audit().tracerActionAsync(categorieAudit, actionAudit, detailsAudit);
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
        private final String message;
        private final Utilisateur utilisateur;
        private final Set<String> permissions;

        public ResultatConnexion(boolean succes, String message) {
            this(succes, message, null, null);
        }

        public ResultatConnexion(boolean succes, String message, Utilisateur utilisateur, Set<String> permissions) {
            this.succes = succes;
            this.message = message;
            this.utilisateur = utilisateur;
            this.permissions = permissions;
        }

        public boolean isSucces() { return succes; }
        public String getMessage() { return message; }
        public Utilisateur getUtilisateur() { return utilisateur; }
        public Set<String> getPermissions() { return permissions; }
    }
}
//package org.erpklassup.erpklassup.service;
//
//import javafx.application.Platform;
//import javafx.stage.Stage;
//import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
//import org.erpklassup.erpklassup.dao.PermissionDAO;
//import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
//import org.erpklassup.erpklassup.dao.UtilisateurDAO;
//import org.erpklassup.erpklassup.models.Utilisateur;
//import org.erpklassup.erpklassup.util.ToastNotification;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.Set;
//import java.util.UUID;
//
//import static okhttp3.internal.Util.userAgent;
//
//public class AuthService {
//
//    private static final int MAX_TENTATIVES = 5;
//    private static final int DUREE_VERROUILLAGE_MINUTES = 15;
//
//    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
//    private final PermissionDAO permissionDAO = new PermissionDAO();
//    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
//    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();
//    private final SessionManager sessionManager = SessionManager.getInstance();
//    private final AuditService auditService = new AuditService();
//    public ResultatConnexion seConnecter(Stage stage, String username, String motDePasse, String idEcole, String ipAdresse, String nomApp) {
//
//        Optional<Utilisateur> userOpt = utilisateurDAO.findByUsername(username, idEcole);
//
//        if (userOpt.isEmpty()) {
//            afficherToast(stage, () -> ToastNotification.erreur(stage, "Identifiant ou mot de passe incorrect"));
//            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
//        }
//
//        Utilisateur user = userOpt.get();
//
//        if (!user.isEstActif()) {
//            auditService.tracerActionAsync("AUTHENTIFICATION", "ECHEC_CONNEXION", "Compte désactivé : " + username);
//            afficherToast(stage, () -> ToastNotification.avertissement(stage, "Ce compte a été désactivé"));
//            return new ResultatConnexion(false, "Ce compte est désactivé");
//        }
//
//        if (user.isCompteVerrouille()) {
//            if (user.getVerrouilleJusqua() != null && user.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
//                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé. Réessayez plus tard."));
//                return new ResultatConnexion(false, "Compte verrouillé. Réessayez plus tard.");
//            } else {
//                utilisateurDAO.deverrouillerCompte(user.getIdUtilisateur());
//                user.setCompteVerrouille(false);
//            }
//        }
//
//        if (!PasswordService.verifier(motDePasse, user.getPasswordHash())) {
//            utilisateurDAO.incrementerTentativesEchec(user.getIdUtilisateur());
//            int tentativesEchouees = user.getNombreTentativesEchec() + 1;
//
//            if (tentativesEchouees >= MAX_TENTATIVES) {
//                utilisateurDAO.verrouillerCompte(user.getIdUtilisateur(), DUREE_VERROUILLAGE_MINUTES);
//                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé après trop d'échecs"));
//                return new ResultatConnexion(false, "Compte verrouillé");
//            } else {
//                int restantes = MAX_TENTATIVES - tentativesEchouees;
//                afficherToast(stage, () -> ToastNotification.avertissement(stage, "Mot de passe incorrect. " + restantes + " tentative(s) restante(s)"));
//                return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
//            }
//        }
//
//        utilisateurDAO.reinitialiserTentativesEchec(user.getIdUtilisateur());
//
//        // 6. Chargement des permissions effectives (Directes + Héritées via CTE SQL)
//        Set<String> permissionsEffectives = permissionDAO.findPermissionsEffectivesPourUtilisateur(user.getIdUtilisateur());
//
//        // 7. Enregistrement de la session BDD
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
//        // 8. Démarrage de la session mémoire
//        sessionManager.demarrerSession(idSession, user, permissionsEffectives);
//
//        // 8bis. Chargement IMMÉDIAT du mapping action -> permission.
//        // Sans cet appel, mappingActions reste vide jusqu'au premier tick du
//        // scheduler background (délai initial de 15s dans
//        // demarrerPlanificateurArrierePlan) : pendant cette fenêtre,
//        // peutExecuterAction(...) retourne systématiquement false et tous les
//        // boutons/actions conditionnés par une permission restent cachés.
//        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());
//
//        // 9. Traçage Audit
//        auditService.tracerActionAsync("AUTHENTIFICATION", "CONNEXION", "Connexion réussie pour " + user.getNomComplet());
//
//        // 10. Toast Notification
//        afficherToast(stage, () -> ToastNotification.succes(stage, "Bienvenue " + user.getPrenom() + " !"));
//
//        return new ResultatConnexion(true, "Connexion réussie", user, permissionsEffectives);
//    }
//
//    public void seDeconnecter(Stage stage) {
//        if (sessionManager.estConnecte()) {
//            auditService.tracerActionAsync("AUTHENTIFICATION", "DECONNEXION", "Déconnexion de l'utilisateur " + sessionManager.getUtilisateurCourant().getNomComplet());
//            sessionManager.terminerSession();
//            afficherToast(stage, () -> ToastNotification.info(stage, "Vous avez été déconnecté"));
//        }
//    }
//
//    private void afficherToast(Stage stage, Runnable action) {
//        if (stage == null) return;
//        if (Platform.isFxApplicationThread()) {
//            action.run();
//        } else {
//            Platform.runLater(action);
//        }
//    }
//
//    public static class ResultatConnexion {
//        private final boolean succes;
//        private final String message;
//        private final Utilisateur utilisateur;
//        private final Set<String> permissions;
//
//        public ResultatConnexion(boolean succes, String message) {
//            this(succes, message, null, null);
//        }
//
//        public ResultatConnexion(boolean succes, String message, Utilisateur utilisateur, Set<String> permissions) {
//            this.succes = succes;
//            this.message = message;
//            this.utilisateur = utilisateur;
//            this.permissions = permissions;
//        }
//
//        public boolean isSucces() { return succes; }
//        public String getMessage() { return message; }
//        public Utilisateur getUtilisateur() { return utilisateur; }
//        public Set<String> getPermissions() { return permissions; }
//    }
//}