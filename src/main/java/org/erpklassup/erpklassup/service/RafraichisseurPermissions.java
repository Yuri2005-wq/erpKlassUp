package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import org.erpklassup.erpklassup.dao.*;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RafraichisseurPermissions {

    private final UtilisateurRoleDAO utilisateurRoleDAO = new UtilisateurRoleDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
    private final AuditService auditService = new AuditService();

    /**
     * Vérifie si l'utilisateur ciblé est celui connecté sur cette instance,
     * et rafraîchit immédiatement ses accès en mémoire.
     */
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

        // 1. Contrôle en BDD : Récupérer l'état actuel de l'utilisateur
        Optional<Utilisateur> userOpt = utilisateurDAO.findById(idUtilisateur);

        // Si l'utilisateur n'existe plus ou est supprimé (deleted_at IS NOT NULL)
        if (userOpt.isEmpty()) {
            forcerDeconnexion("Votre compte a été supprimé par l'administrateur.");
            return;
        }

        Utilisateur user = userOpt.get();

        // Si le compte est désactivé
        if (!user.isEstActif()) {
            forcerDeconnexion("Votre compte a été désactivé.");
            return;
        }

        // Si le compte est verrouillé
        if (user.isCompteVerrouille()) {
            forcerDeconnexion("Votre compte a été verrouillé.");
            return;
        }

        // 2. Si le compte est valide, mise à jour des rôles et des permissions
        List<Role> roles = utilisateurRoleDAO.findRolesByUtilisateur(idUtilisateur);
        List<Permission> permissions = new ArrayList<>();
        for (Role role : roles) {
            permissions.addAll(permissionDAO.findByRole(role.getIdRole()));
        }

        SessionManager.getInstance().remplacerPermissions(permissions);
        SessionManager.getInstance().remplacerMappingActions(actionPermissionDAO.chargerMapping());
    }

    private void forcerDeconnexion(String message) {
        // 2. Traçage d'audit AVANT de détruire la session mémoire
        auditService.tracerActionAsync(
                "AUTHENTIFICATION",
                "DECONNEXION_FORCEE",
                "Déconnexion automatique : " + message
        );

        Platform.runLater(() -> {
            SessionManager sessionManager = SessionManager.getInstance();

            // 3. Invalidation de la session (BDD + Mémoire)
            sessionManager.terminerSession();

            // Nettoyage de la mémoire des vues et redirection
            if (sessionManager.getViewRegistry() != null) {
                sessionManager.getViewRegistry().toutReinitialiser();
                sessionManager.getViewRegistry().afficherVue("/org/erpklassup/erpklassup/views/login.fxml");
            }

            // Notification Toast
            if (sessionManager.getStagePrincipal() != null) {
                ToastNotification.erreur(sessionManager.getStagePrincipal(), message);
            }
        });
    }
    }





//    public void rafraichir() {
//        String idUtilisateur = SessionManager.getInstance().getIdUtilisateurCourant();
//        if (idUtilisateur == null) return;
//
//        List<Role> roles = utilisateurRoleDAO.findRolesByUtilisateur(idUtilisateur);
//        List<Permission> permissions = new ArrayList<>();
//        for (Role role : roles) {
//            permissions.addAll(permissionDAO.findByRole(role.getIdRole()));
//        }
//
//        // Met à jour la session active et réveille tous les écouteurs de vues FXML
//        SessionManager.getInstance().remplacerPermissions(permissions);
//        SessionManager.getInstance().remplacerMappingActions(actionPermissionDAO.chargerMapping());
//    }







//package org.erpklassup.erpklassup.service;
//
//import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
//import org.erpklassup.erpklassup.dao.PermissionDAO;
//import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
//import org.erpklassup.erpklassup.models.Permission;
//import org.erpklassup.erpklassup.models.Role;
//import org.erpklassup.erpklassup.service.SessionManager;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class RafraichisseurPermissions {
//
//    private final UtilisateurRoleDAO utilisateurRoleDAO = new UtilisateurRoleDAO();
//    private final PermissionDAO permissionDAO = new PermissionDAO();
//    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();
//
//    public void rafraichir() {
//        String idUtilisateur = SessionManager.getInstance().getIdUtilisateurCourant();
//        if (idUtilisateur == null) return;
//
//        List<Role> roles = utilisateurRoleDAO.findRolesByUtilisateur(idUtilisateur);
//        List<Permission> permissions = new ArrayList<>();
//        for (Role role : roles) permissions.addAll(permissionDAO.findByRole(role.getIdRole()));
//
//        SessionManager.getInstance().remplacerPermissions(permissions);
//        SessionManager.getInstance().remplacerMappingActions(actionPermissionDAO.chargerMapping());
//    }
//}