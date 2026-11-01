package org.erpklassup.erpklassup.service;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.util.NavigationUtil;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.erpklassup.erpklassup.util.ViewRegistry;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

public class RafraichisseurPermissions {

    private static final String CHEMIN_LOGIN = "/org/erpklassup/erpklassup/view/login-view.fxml";

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO(); // ✅ AJOUT
    private final AuditService auditService = new AuditService();

    public void rafraichirPourUtilisateur(String idUtilisateurCible) {
        String idConnecte = SessionManager.getInstance().getIdUtilisateurCourant();
        if (idConnecte == null || idUtilisateurCible == null) return;

        if (idConnecte.equalsIgnoreCase(idUtilisateurCible)) {
            rafraichir();
        }
    }

    public void rafraichir() {
        String idUtilisateur = SessionManager.getInstance().getIdUtilisateurCourant();
        if (idUtilisateur == null) return;

        // ✅ NOUVEAU : vérifier en premier que la session locale est toujours
        // celle qui fait autorité. Si une nouvelle connexion pour ce même
        // compte a été détectée ailleurs, AuthService a déjà révoqué cette
        // session en base — on le détecte ici et on se déconnecte proprement.
        String idSessionCourante = SessionManager.getInstance().getIdSessionCourante();
        if (idSessionCourante != null && !sessionUtilisateurDAO.estSessionValide(idSessionCourante)) {
            forcerDeconnexion("Une connexion a été détectée depuis un autre poste. Vous avez été déconnecté.");
            return;
        }

        Optional<Utilisateur> userOpt = utilisateurDAO.findById(idUtilisateur);

        if (userOpt.isEmpty()) {
            forcerDeconnexion("Votre compte a été supprimé par l'administrateur.");
            return;
        }

        Utilisateur user = userOpt.get();

        if (!user.isEstActif()) {
            forcerDeconnexion("Votre compte a été désactivé.");
            return;
        }

        if (user.isCompteVerrouille()) {
            forcerDeconnexion("Votre compte a été verrouillé.");
            return;
        }

        Set<String> permissionsEffectives =
                permissionDAO.findPermissionsEffectivesPourUtilisateur(idUtilisateur);

        SessionManager.getInstance().remplacerCodesPermissions(permissionsEffectives);
        SessionManager.getInstance().remplacerMappingActions(actionPermissionDAO.chargerMapping());
    }

    /**
     * Déconnexion forcée : invalide la session, vide les vues et redirige vers le login.
     * Cette méthode est thread-safe et peut être appelée depuis n'importe quel thread.
     */
    private void forcerDeconnexion(String message) {
        // 1. Audit AVANT destruction de session
        auditService.tracerActionAsync(
                "AUTHENTIFICATION",
                "DECONNEXION_FORCEE",
                "Déconnexion automatique : " + message
        );

        // 2. Tout le reste sur le thread JavaFX
        Platform.runLater(() -> {
            SessionManager sessionManager = SessionManager.getInstance();
            Stage stage = sessionManager.getStagePrincipal();

            // 3. Vider le ViewRegistry AVANT de terminer la session
            ViewRegistry registry = sessionManager.getViewRegistry();
            if (registry != null) {
                registry.toutReinitialiser();
            }

            // 4. Terminer la session (BDD + mémoire)
            sessionManager.terminerSession();

            // 5. Redirection vers le login (même animation que la déconnexion manuelle)
            if (stage != null) {
                try {
                    NavigationUtil.retournerAuLogin(stage);

                    // Toast après un court délai (le temps que l'animation se termine)
                    PauseTransition pause = new PauseTransition(Duration.millis(600));
                    pause.setOnFinished(e -> {
                        if (stage.isShowing()) {
                            ToastNotification.erreur(stage, message);
                        }
                    });
                    pause.play();

                } catch (IOException e) {
                    System.err.println("❌ [Rafraichisseur] Erreur redirection login : " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("⚠️ [Rafraichisseur] Stage null — redirection impossible");
            }
        });
    }

    /**
     * Redirige le Stage vers l'écran de login, avec fallback si le ViewRegistry est absent.
     */
    private void redirigerVersLogin(Stage stage, ViewRegistry registry, String message) {
        // Cas 1 : on a un ViewRegistry → on l'utilise pour charger le login
        if (registry != null) {
            registry.afficherVue(CHEMIN_LOGIN);

            // Toast après un court délai pour laisser l'animation se faire
            PauseTransition pause = new PauseTransition(Duration.millis(350));
            pause.setOnFinished(e -> {
                if (stage.isShowing()) {
                    ToastNotification.erreur(stage, message);
                }
            });
            pause.play();
            return;
        }

        // Cas 2 : pas de ViewRegistry → on remplace la scène manuellement
        try {
            URL ressource = getClass().getResource(CHEMIN_LOGIN);
            if (ressource == null) {
                System.err.println("❌ FXML login introuvable : " + CHEMIN_LOGIN);
                return;
            }

            FXMLLoader loader = new FXMLLoader(ressource);
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot, 1000, 640);

            Scene sceneActuelle = stage.getScene();
            Parent rootActuel = sceneActuelle != null ? sceneActuelle.getRoot() : null;

            Runnable appliquer = () -> {
                if (stage.isMaximized()) stage.setMaximized(false);
                stage.setScene(loginScene);
                stage.setWidth(1000);
                stage.setHeight(640);
                stage.setResizable(false);
                stage.setTitle("KlassUp - Connexion");
                stage.centerOnScreen();

                loginRoot.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), loginRoot);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();

                PauseTransition pause = new PauseTransition(Duration.millis(400));
                pause.setOnFinished(e -> {
                    if (stage.isShowing()) {
                        ToastNotification.erreur(stage, message);
                    }
                });
                pause.play();
            };

            if (rootActuel != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(180), rootActuel);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> appliquer.run());
                fadeOut.play();
            } else {
                appliquer.run();
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur redirection login : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
//package org.erpklassup.erpklassup.service;
//
//import javafx.animation.FadeTransition;
//import javafx.animation.PauseTransition;
//import javafx.application.Platform;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
//import org.erpklassup.erpklassup.dao.PermissionDAO;
//import org.erpklassup.erpklassup.dao.UtilisateurDAO;
//import org.erpklassup.erpklassup.models.Utilisateur;
//import org.erpklassup.erpklassup.util.NavigationUtil;
//import org.erpklassup.erpklassup.util.ToastNotification;
//import org.erpklassup.erpklassup.util.ViewRegistry;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.Optional;
//import java.util.Set;
//
//public class RafraichisseurPermissions {
//
//    private static final String CHEMIN_LOGIN = "/org/erpklassup/erpklassup/view/login-view.fxml";
//
//    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
//    private final PermissionDAO permissionDAO = new PermissionDAO();
//    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
//    private final AuditService auditService = new AuditService();
//
//    public void rafraichirPourUtilisateur(String idUtilisateurCible) {
//        String idConnecte = SessionManager.getInstance().getIdUtilisateurCourant();
//        if (idConnecte == null || idUtilisateurCible == null) return;
//
//        if (idConnecte.equalsIgnoreCase(idUtilisateurCible)) {
//            rafraichir();
//        }
//    }
//
//    public void rafraichir() {
//        String idUtilisateur = SessionManager.getInstance().getIdUtilisateurCourant();
//        if (idUtilisateur == null) return;
//
//        Optional<Utilisateur> userOpt = utilisateurDAO.findById(idUtilisateur);
//
//        if (userOpt.isEmpty()) {
//            forcerDeconnexion("Votre compte a été supprimé par l'administrateur.");
//            return;
//        }
//
//        Utilisateur user = userOpt.get();
//
//        if (!user.isEstActif()) {
//            forcerDeconnexion("Votre compte a été désactivé.");
//            return;
//        }
//
//        if (user.isCompteVerrouille()) {
//            forcerDeconnexion("Votre compte a été verrouillé.");
//            return;
//        }
//
//        Set<String> permissionsEffectives =
//                permissionDAO.findPermissionsEffectivesPourUtilisateur(idUtilisateur);
//
//        SessionManager.getInstance().remplacerCodesPermissions(permissionsEffectives);
//        SessionManager.getInstance().remplacerMappingActions(actionPermissionDAO.chargerMapping());
//    }
//
//    /**
//     * Déconnexion forcée : invalide la session, vide les vues et redirige vers le login.
//     * Cette méthode est thread-safe et peut être appelée depuis n'importe quel thread.
//     */
//    private void forcerDeconnexion(String message) {
//        // 1. Audit AVANT destruction de session
//        auditService.tracerActionAsync(
//                "AUTHENTIFICATION",
//                "DECONNEXION_FORCEE",
//                "Déconnexion automatique : " + message
//        );
//
//        // 2. Tout le reste sur le thread JavaFX
//        Platform.runLater(() -> {
//            SessionManager sessionManager = SessionManager.getInstance();
//            Stage stage = sessionManager.getStagePrincipal();
//
//            // 3. Vider le ViewRegistry AVANT de terminer la session
//            ViewRegistry registry = sessionManager.getViewRegistry();
//            if (registry != null) {
//                registry.toutReinitialiser();
//            }
//
//            // 4. Terminer la session (BDD + mémoire)
//            sessionManager.terminerSession();
//
//            // 5. Redirection vers le login (même animation que la déconnexion manuelle)
//            if (stage != null) {
//                try {
//                    NavigationUtil.retournerAuLogin(stage);
//
//                    // Toast après un court délai (le temps que l'animation se termine)
//                    PauseTransition pause = new PauseTransition(Duration.millis(600));
//                    pause.setOnFinished(e -> {
//                        if (stage.isShowing()) {
//                            ToastNotification.erreur(stage, message);
//                        }
//                    });
//                    pause.play();
//
//                } catch (IOException e) {
//                    System.err.println("❌ [Rafraichisseur] Erreur redirection login : " + e.getMessage());
//                    e.printStackTrace();
//                }
//            } else {
//                System.err.println("⚠️ [Rafraichisseur] Stage null — redirection impossible");
//            }
//        });
//    }
//
//    /**
//     * Redirige le Stage vers l'écran de login, avec fallback si le ViewRegistry est absent.
//     */
//    private void redirigerVersLogin(Stage stage, ViewRegistry registry, String message) {
//        // Cas 1 : on a un ViewRegistry → on l'utilise pour charger le login
//        if (registry != null) {
//            registry.afficherVue(CHEMIN_LOGIN);
//
//            // Toast après un court délai pour laisser l'animation se faire
//            PauseTransition pause = new PauseTransition(Duration.millis(350));
//            pause.setOnFinished(e -> {
//                if (stage.isShowing()) {
//                    ToastNotification.erreur(stage, message);
//                }
//            });
//            pause.play();
//            return;
//        }
//
//        // Cas 2 : pas de ViewRegistry → on remplace la scène manuellement
//        try {
//            URL ressource = getClass().getResource(CHEMIN_LOGIN);
//            if (ressource == null) {
//                System.err.println("❌ FXML login introuvable : " + CHEMIN_LOGIN);
//                return;
//            }
//
//            FXMLLoader loader = new FXMLLoader(ressource);
//            Parent loginRoot = loader.load();
//            Scene loginScene = new Scene(loginRoot, 1000, 640);
//
//            Scene sceneActuelle = stage.getScene();
//            Parent rootActuel = sceneActuelle != null ? sceneActuelle.getRoot() : null;
//
//            Runnable appliquer = () -> {
//                if (stage.isMaximized()) stage.setMaximized(false);
//                stage.setScene(loginScene);
//                stage.setWidth(1000);
//                stage.setHeight(640);
//                stage.setResizable(false);
//                stage.setTitle("KlassUp - Connexion");
//                stage.centerOnScreen();
//
//                loginRoot.setOpacity(0);
//                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), loginRoot);
//                fadeIn.setFromValue(0);
//                fadeIn.setToValue(1);
//                fadeIn.play();
//
//                PauseTransition pause = new PauseTransition(Duration.millis(400));
//                pause.setOnFinished(e -> {
//                    if (stage.isShowing()) {
//                        ToastNotification.erreur(stage, message);
//                    }
//                });
//                pause.play();
//            };
//
//            if (rootActuel != null) {
//                FadeTransition fadeOut = new FadeTransition(Duration.millis(180), rootActuel);
//                fadeOut.setFromValue(1);
//                fadeOut.setToValue(0);
//                fadeOut.setOnFinished(e -> appliquer.run());
//                fadeOut.play();
//            } else {
//                appliquer.run();
//            }
//
//        } catch (IOException e) {
//            System.err.println("❌ Erreur redirection login : " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}