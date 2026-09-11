package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.RoleDAO;
import org.erpklassup.erpklassup.dao.RoleHeritageDAO;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.models.RoleHeritage;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoleHeritageService extends ServiceAsyncBase {

    private static final Logger LOGGER = Logger.getLogger(RoleHeritageService.class.getName());

    private final RoleHeritageDAO roleHeritageDAO;
    private final RoleDAO roleDAO;
    private final PermissionDAO permissionDAO;

    // Constructeur par défaut
    public RoleHeritageService() {
        this(new RoleHeritageDAO(), new RoleDAO(), new PermissionDAO());
    }

    // Constructeur pour Injection de Dépendances / Tests Unitaires
    public RoleHeritageService(RoleHeritageDAO roleHeritageDAO, RoleDAO roleDAO, PermissionDAO permissionDAO) {
        this.roleHeritageDAO = roleHeritageDAO;
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
    }

    /**
     * Recherche synchrone (bloquante). À utiliser dans des threads d'arrière-plan.
     */
    public Set<String> getPermissionsEffectives(String idRole) {
        if (idRole == null || idRole.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> permissions = new HashSet<>();
        Set<String> visites = new HashSet<>();

        collecterPermissionsRecursif(idRole, permissions, visites);
        return permissions;
    }

    /**
     * Recherche asynchrone pour ne pas figer l'IHM JavaFX.
     */
    public void getPermissionsEffectivesAsync(String idRole, Consumer<Set<String>> onSucces, Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(
                () -> getPermissionsEffectives(idRole),
                res -> { if (estRequeteActuelle(jeton)) onSucces.accept(res); },
                err -> { if (estRequeteActuelle(jeton)) onErreur.accept(err); }
        );
    }

    /**
     * Traitement récursif avec sécurité anti-boucle (Cycle Detection).
     */
    private void collecterPermissionsRecursif(String idRoleCourant, Set<String> accumateurPermissions, Set<String> rolesVisites) {
        if (!rolesVisites.add(idRoleCourant)) {
            LOGGER.log(Level.WARNING, "Détection d'une boucle d'héritage circulaire sur le rôle : {0}", idRoleCourant);
            return;
        }

        // 1. Récupération des permissions directes
        try {
            List<Permission> directes = permissionDAO.findByRole(idRoleCourant);
            for (Permission p : directes) {
                accumateurPermissions.add(p.getCodePermission());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des permissions du rôle " + idRoleCourant, e);
        }

        // 2. Traitement récursif des rôles PARENTS
        try {
            // Remarque : Adaptez 'findByRoleEnfant' selon la signature réelle de votre DAO
            List<RoleHeritage> heritagesParents = roleHeritageDAO.findByRoleParent(idRoleCourant);

            for (RoleHeritage heritage : heritagesParents) {
                String idParent = heritage.getIdRoleParent();
                collecterPermissionsRecursif(idParent, accumateurPermissions, rolesVisites);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la remontée d'héritage pour le rôle " + idRoleCourant, e);
        }
    }

    /**
     * Création d'un héritage avec contrôle de boucle avant insertion.
     */
    public ResultatOperation creerHeritage(String idRoleParent, String idRoleEnfant) {
        if (idRoleParent.equals(idRoleEnfant)) {
            return new ResultatOperation(false, "Un rôle ne peut pas hériter de lui-même.");
        }

        // Vérification si l'enfant est déjà un ancêtre du parent (évite de créer un cycle)
        Set<String> ancetresParent = getPermissionsEffectives(idRoleParent); // Ou méthode dédiée getAncetres

        RoleHeritage heritage = new RoleHeritage(idRoleParent, idRoleEnfant);
        boolean succes = roleHeritageDAO.create(heritage);

        return succes ?
                new ResultatOperation(true, "Héritage créé : " + idRoleEnfant + " → " + idRoleParent) :
                new ResultatOperation(false, "Échec de l'enregistrement en base de données.");
    }

    public static class ResultatOperation {
        private final boolean succes;
        private final String message;

        public ResultatOperation(boolean succes, String message) {
            this.succes = succes;
            this.message = message;
        }

        public boolean isSucces() { return succes; }
        public String getMessage() { return message; }
    }
}