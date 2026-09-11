package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static okhttp3.internal.Util.userAgent;

public class AuthService {

    private static final int MAX_TENTATIVES = 5;
    private static final int DUREE_VERROUILLAGE_MINUTES = 15;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuditService auditService = new AuditService();
    public ResultatConnexion seConnecter(Stage stage, String username, String motDePasse, String idEcole, String ipAdresse, String nomApp) {

        Optional<Utilisateur> userOpt = utilisateurDAO.findByUsername(username, idEcole);

        if (userOpt.isEmpty()) {
            afficherToast(stage, () -> ToastNotification.erreur(stage, "Identifiant ou mot de passe incorrect"));
            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
        }

        Utilisateur user = userOpt.get();

        if (!user.isEstActif()) {
            auditService.tracerActionAsync("AUTHENTIFICATION", "ECHEC_CONNEXION", "Compte désactivé : " + username);
            afficherToast(stage, () -> ToastNotification.avertissement(stage, "Ce compte a été désactivé"));
            return new ResultatConnexion(false, "Ce compte est désactivé");
        }

        if (user.isCompteVerrouille()) {
            if (user.getVerrouilleJusqua() != null && user.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé. Réessayez plus tard."));
                return new ResultatConnexion(false, "Compte verrouillé. Réessayez plus tard.");
            } else {
                utilisateurDAO.deverrouillerCompte(user.getIdUtilisateur());
                user.setCompteVerrouille(false);
            }
        }

        if (!PasswordService.verifier(motDePasse, user.getPasswordHash())) {
            utilisateurDAO.incrementerTentativesEchec(user.getIdUtilisateur());
            int tentativesEchouees = user.getNombreTentativesEchec() + 1;

            if (tentativesEchouees >= MAX_TENTATIVES) {
                utilisateurDAO.verrouillerCompte(user.getIdUtilisateur(), DUREE_VERROUILLAGE_MINUTES);
                afficherToast(stage, () -> ToastNotification.erreur(stage, "Compte verrouillé après trop d'échecs"));
                return new ResultatConnexion(false, "Compte verrouillé");
            } else {
                int restantes = MAX_TENTATIVES - tentativesEchouees;
                afficherToast(stage, () -> ToastNotification.avertissement(stage, "Mot de passe incorrect. " + restantes + " tentative(s) restante(s)"));
                return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
            }
        }

        utilisateurDAO.reinitialiserTentativesEchec(user.getIdUtilisateur());

        // 6. Chargement des permissions effectives (Directes + Héritées via CTE SQL)
        Set<String> permissionsEffectives = permissionDAO.findPermissionsEffectivesPourUtilisateur(user.getIdUtilisateur());

        // 7. Enregistrement de la session BDD
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

        // 8. Démarrage de la session mémoire
        sessionManager.demarrerSession(idSession, user, permissionsEffectives);

        // 8bis. Chargement IMMÉDIAT du mapping action -> permission.
        // Sans cet appel, mappingActions reste vide jusqu'au premier tick du
        // scheduler background (délai initial de 15s dans
        // demarrerPlanificateurArrierePlan) : pendant cette fenêtre,
        // peutExecuterAction(...) retourne systématiquement false et tous les
        // boutons/actions conditionnés par une permission restent cachés.
        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());

        // 9. Traçage Audit
        auditService.tracerActionAsync("AUTHENTIFICATION", "CONNEXION", "Connexion réussie pour " + user.getNomComplet());

        // 10. Toast Notification
        afficherToast(stage, () -> ToastNotification.succes(stage, "Bienvenue " + user.getPrenom() + " !"));

        return new ResultatConnexion(true, "Connexion réussie", user, permissionsEffectives);
    }

    public void seDeconnecter(Stage stage) {
        if (sessionManager.estConnecte()) {
            auditService.tracerActionAsync("AUTHENTIFICATION", "DECONNEXION", "Déconnexion de l'utilisateur " + sessionManager.getUtilisateurCourant().getNomComplet());
            sessionManager.terminerSession();
            afficherToast(stage, () -> ToastNotification.info(stage, "Vous avez été déconnecté"));
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