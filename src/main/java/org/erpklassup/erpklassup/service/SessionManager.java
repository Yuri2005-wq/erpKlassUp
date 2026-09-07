package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.models.Utilisateur;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class SessionManager {

    private static SessionManager instance;
    private Utilisateur utilisateurCourant;
    private final Set<String> codesPermissions = new HashSet<>();
    private final Map<String, String> mappingActions = new HashMap<>(); // codeAction -> codePermission
    private final List<Runnable> ecouteursPermissions = new CopyOnWriteArrayList<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void demarrerSession(Utilisateur user, List<Permission> permissions) {
        this.utilisateurCourant = user;
        remplacerPermissions(permissions);
    }

    public void remplacerPermissions(List<Permission> permissions) {
        codesPermissions.clear();
        if (permissions != null) for (Permission p : permissions) codesPermissions.add(p.getCodePermission());
        notifierChangementPermissions();
    }

    public void remplacerMappingActions(Map<String, String> mapping) {
        mappingActions.clear();
        if (mapping != null) mappingActions.putAll(mapping);
        notifierChangementPermissions();
    }

    public boolean estConnecte() { return utilisateurCourant != null; }
    public boolean aLaPermission(String codePermission) { return codesPermissions.contains(codePermission); }

    /**
     * Vérifie l'accès via un code d'action (ex: "UTILISATEUR_SUPPRIMER"), résolu vers sa
     * permission via ActionPermission. Fail-closed : une action sans mapping configuré
     * en base est refusée par défaut, jamais autorisée par accident.
     */
    public boolean peutExecuterAction(String codeAction) {
        String codePermission = mappingActions.get(codeAction);
        if (codePermission == null) {
            System.err.println("Aucune permission associée à l'action : " + codeAction + " (accès refusé par défaut)");
            return false;
        }
        return aLaPermission(codePermission);
    }

    public void terminerSession() {
        utilisateurCourant = null;
        codesPermissions.clear();
        mappingActions.clear();
        notifierChangementPermissions();
    }

    public Utilisateur getUtilisateurCourant() { return utilisateurCourant; }
    public String getIdEcoleCourante() { return utilisateurCourant != null ? utilisateurCourant.getIdEcole() : null; }
    public String getIdUtilisateurCourant() { return utilisateurCourant != null ? utilisateurCourant.getIdUtilisateur() : null; }

    public void ecouterChangementsPermissions(Runnable ecouteur) { ecouteursPermissions.add(ecouteur); }
    public void arreterEcoute(Runnable ecouteur) { ecouteursPermissions.remove(ecouteur); }

    private void notifierChangementPermissions() {
        for (Runnable ecouteur : ecouteursPermissions) {
            Platform.runLater(() -> { try { ecouteur.run(); } catch (Exception e) { e.printStackTrace(); } });
        }
    }
}