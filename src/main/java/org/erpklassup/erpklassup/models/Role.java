package org.erpklassup.erpklassup.models;

import java.util.ArrayList;
import java.util.List;

public class Role extends BaseEntity {
    private String idEcole;
    private String idGroupe;
    private String nomRole;
    private String description;
    private boolean isActive;
    private List<Permission> permissions;

    public Role() {
        super();
        this.isActive = true;
        this.permissions = new ArrayList<>();
    }

    public Role(String nomRole) {
        this();
        this.nomRole = nomRole;
    }

    public Role(String nomRole, String description) {
        this(nomRole);
        this.description = description;
    }

    public String getIdRole() { return getId(); }
    public void setIdRole(String idRole) { setId(idRole); }
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }
    public String getIdGroupe() { return idGroupe; }
    public void setIdGroupe(String idGroupe) { this.idGroupe = idGroupe; }
    public String getNomRole() { return nomRole; }
    public void setNomRole(String nomRole) { this.nomRole = nomRole; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public List<Permission> getPermissions() { return permissions; }
    public void setPermissions(List<Permission> permissions) { this.permissions = permissions; }
    public void addPermission(Permission permission) { this.permissions.add(permission); }

    @Override
    public String toString() { return nomRole; }
}