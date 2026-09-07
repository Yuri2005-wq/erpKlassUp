package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.models.Permission;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthService {

    private static final int MAX_TENTATIVES = 5;
    private static final int DUREE_VERROUILLAGE_MINUTES = 15;

    private final UtilisateurDAO utilisateurDAO;
    private final PermissionDAO permissionDAO;
    private final UtilisateurRoleDAO utilisateurRoleDAO;
    private final SessionManager sessionManager;
    private final org.erpklassup.erpklassup.dao.ActionPermissionDAO actionPermissionDAO = new org.erpklassup.erpklassup.dao.ActionPermissionDAO();

    public AuthService() {
        this.utilisateurDAO = new UtilisateurDAO();
        this.permissionDAO = new PermissionDAO();
        this.utilisateurRoleDAO = new UtilisateurRoleDAO();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * Connecte un utilisateur avec son username et mot de passe
     */
    public ResultatConnexion seConnecter(String username, String motDePasse, String idEcole) {

        // 1. Chercher l'utilisateur
        Optional<Utilisateur> userOpt = utilisateurDAO.findByUsername(username, idEcole);

        if (!userOpt.isPresent()) {
            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
        }

        Utilisateur user = userOpt.get();

        // 2. Vérifier si le compte est actif
        if (!user.isEstActif()) {
            return new ResultatConnexion(false, "Ce compte est désactivé");
        }

        // 3. Vérifier si le compte est verrouillé
        if (user.isCompteVerrouille()) {
            if (user.getVerrouilleJusqua() != null &&
                    user.getVerrouilleJusqua().isAfter(LocalDateTime.now())) {
                return new ResultatConnexion(false, "Compte verrouillé. Réessayez plus tard.");
            } else {
                utilisateurDAO.deverrouillerCompte(user.getIdUtilisateur());
                user.setCompteVerrouille(false);
            }
        }

        // 4. Vérifier le mot de passe
        if (!PasswordService.verifier(motDePasse, user.getPasswordHash())) {
            utilisateurDAO.incrementerTentativesEchec(user.getIdUtilisateur());

            if (user.getNombreTentativesEchec() + 1 >= MAX_TENTATIVES) {
                utilisateurDAO.verrouillerCompte(user.getIdUtilisateur(), DUREE_VERROUILLAGE_MINUTES);
                return new ResultatConnexion(false,
                        "Compte verrouillé après " + MAX_TENTATIVES + " tentatives échouées");
            }

            return new ResultatConnexion(false, "Identifiant ou mot de passe incorrect");
        }

        // 5. Connexion réussie
        utilisateurDAO.reinitialiserTentativesEchec(user.getIdUtilisateur());

        // 6. ✅ NOUVEAU : Charger TOUS les rôles de l'utilisateur
        List<Role> roles = utilisateurRoleDAO.findRolesByUtilisateur(user.getIdUtilisateur());

        // 7. ✅ NOUVEAU : Charger TOUTES les permissions de TOUS les rôles
        List<Permission> allPermissions = new ArrayList<>();
        for (Role role : roles) {
            List<Permission> permissionsDuRole = permissionDAO.findByRole(role.getIdRole());
            allPermissions.addAll(permissionsDuRole);
        }

        // 8. ✅ Démarrer la session avec TOUTES les permissions
        sessionManager.demarrerSession(user, allPermissions);
        sessionManager.remplacerMappingActions(actionPermissionDAO.chargerMapping());

        return new ResultatConnexion(true, "Connexion réussie", user, allPermissions);
    }

    /**
     * Déconnecte l'utilisateur courant
     */
    public void seDeconnecter() {
        sessionManager.terminerSession();
    }

    /**
     * Vérifie si un utilisateur est connecté
     */
    public boolean estConnecte() {
        return sessionManager.estConnecte();
    }

    /**
     * Classe interne pour le résultat de connexion
     */
    public static class ResultatConnexion {
        private final boolean succes;
        private final String message;
        private final Utilisateur utilisateur;
        private final List<Permission> permissions;

        public ResultatConnexion(boolean succes, String message) {
            this(succes, message, null, null);
        }

        public ResultatConnexion(boolean succes, String message,
                                 Utilisateur utilisateur, List<Permission> permissions) {
            this.succes = succes;
            this.message = message;
            this.utilisateur = utilisateur;
            this.permissions = permissions;
        }

        public boolean isSucces() { return succes; }
        public String getMessage() { return message; }
        public Utilisateur getUtilisateur() { return utilisateur; }
        public List<Permission> getPermissions() { return permissions; }
    }
}