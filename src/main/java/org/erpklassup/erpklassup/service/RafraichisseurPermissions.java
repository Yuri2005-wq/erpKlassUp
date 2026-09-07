package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.ActionPermissionDAO;
import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.service.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class RafraichisseurPermissions {

    private final UtilisateurRoleDAO utilisateurRoleDAO = new UtilisateurRoleDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final ActionPermissionDAO actionPermissionDAO = new ActionPermissionDAO();

    public void rafraichir() {
        String idUtilisateur = SessionManager.getInstance().getIdUtilisateurCourant();
        if (idUtilisateur == null) return;

        List<Role> roles = utilisateurRoleDAO.findRolesByUtilisateur(idUtilisateur);
        List<Permission> permissions = new ArrayList<>();
        for (Role role : roles) permissions.addAll(permissionDAO.findByRole(role.getIdRole()));

        SessionManager.getInstance().remplacerPermissions(permissions);
        SessionManager.getInstance().remplacerMappingActions(actionPermissionDAO.chargerMapping());
    }
}