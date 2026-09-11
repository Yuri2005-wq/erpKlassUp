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

/**
 * Role + Permission + RolePermission sont fortement couplés : un seul service.
 * Intègre le rafraîchissement à chaud des permissions applicatives.
 */
public class RoleService extends ServiceAsyncBase {

    private final RoleDAO roleDAO;
    private final PermissionDAO permissionDAO;
    private final RolePermissionDAO rolePermissionDAO;
    private final RafraichisseurPermissions rafraichisseur; // ✅ AJOUT : Composant pour impacter la session en mémoire

    public RoleService() {
        this(new RoleDAO(), new PermissionDAO(), new RolePermissionDAO(), new RafraichisseurPermissions());
    }

    public RoleService(RoleDAO roleDAO, PermissionDAO permissionDAO, RolePermissionDAO rolePermissionDAO, RafraichisseurPermissions rafraichisseur) {
        this.roleDAO = roleDAO;
        this.permissionDAO = permissionDAO;
        this.rolePermissionDAO = rolePermissionDAO;
        this.rafraichisseur = rafraichisseur;
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
            if (idEcole == null || idEcole.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école ne peut pas être nul ou vide.");
            }
            if (nom == null || nom.isBlank()) {
                throw new IllegalArgumentException("Le nom du rôle est obligatoire.");
            }

            // Instanciation avec le nouveau constructeur : (nomRole, idEcole, description)
            Role role = new Role(nom, idEcole, description);

            if (!roleDAO.create(role)) {
                throw new IllegalStateException("Impossible de créer le rôle en base de données (nom déjà utilisé ?).");
            }

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
            // ✅ CORRECTION : Si des rôles sont supprimés, les droits en mémoire sont synchronisés immédiatement
            rafraichisseur.rafraichir();
        }, onSucces, onErreur);
    }

    public void enregistrerPermissionsAsync(String idRole, String idEcole, Set<String> idsPermissionCochees,
                                            String attribuePar, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // 1. Sauvegarde en BDD
            rolePermissionDAO.synchroniser(idRole, idEcole, idsPermissionCochees, attribuePar);
            // 2. ✅ CORRECTION : Application immédiate de la modification des permissions sur la session en cours
            rafraichisseur.rafraichir();
        }, onSucces, onErreur);
    }
}