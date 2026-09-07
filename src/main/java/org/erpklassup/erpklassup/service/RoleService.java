package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.PermissionDAO;
import org.erpklassup.erpklassup.dao.RoleDAO;
import org.erpklassup.erpklassup.dao.RolePermissionDAO;
import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.models.Role;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Role + Permission + RolePermission sont fortement couplés : un seul service, pas trois. */
public class RoleService extends ServiceAsyncBase {

    private final RoleDAO roleDAO;
    private final PermissionDAO permissionDAO;
    private final RolePermissionDAO rolePermissionDAO;

    public RoleService() { this(new RoleDAO(), new PermissionDAO(), new RolePermissionDAO()); }

    public RoleService(RoleDAO roleDAO, PermissionDAO permissionDAO, RolePermissionDAO rolePermissionDAO) {
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
        this.rolePermissionDAO = rolePermissionDAO;
    }

    public void listerRolesAsync(String idEcole, Consumer<List<RoleOption>> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> roleDAO.listerParEcole(idEcole), onSucces, onErreur);
    }

    public void listerPermissionsAsync(Consumer<List<PermissionOption>> onSucces, Consumer<Throwable> onErreur) {
        executer(permissionDAO::listerToutes, onSucces, onErreur);
    }

    public void chargerPermissionsDuRoleAsync(String idRole, Consumer<Set<String>> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> rolePermissionDAO.listerIdsActifsParRole(idRole), onSucces, onErreur);
    }

    public void creerRoleAsync(String idEcole, String nom, String description, Consumer<Role> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> {
            Role role = new Role(nom, idEcole);
            role.setDescription(description);
            if (!roleDAO.create(role)) throw new IllegalStateException("Impossible de créer le rôle (nom déjà utilisé ?)");
            return role;
        }, onSucces, onErreur);
    }

    public void renommerRoleAsync(String idRole, String nouveauNom, String nouvelleDescription, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (!roleDAO.renommer(idRole, nouveauNom, nouvelleDescription)) throw new IllegalStateException("Impossible de renommer le rôle.");
        }, onSucces, onErreur);
    }

    public void supprimerRoleAsync(String idRole, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (!roleDAO.softDelete(idRole)) throw new IllegalStateException("Impossible de supprimer le rôle.");
        }, onSucces, onErreur);
    }

    public void enregistrerPermissionsAsync(String idRole, String idEcole, Set<String> idsPermissionCochees,
                                            String attribuePar, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> rolePermissionDAO.synchroniser(idRole, idEcole, idsPermissionCochees, attribuePar), onSucces, onErreur);
    }
}