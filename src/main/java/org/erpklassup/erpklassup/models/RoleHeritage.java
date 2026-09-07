package org.erpklassup.erpklassup.models;

public class RoleHeritage extends BaseEntity {

    private String idRoleParent;
    private String idRoleEnfant;
    private String idEcole;

    public RoleHeritage() {
        super();
    }

    public RoleHeritage(String idRoleParent, String idRoleEnfant) {
        this();
        this.idRoleParent = idRoleParent;
        this.idRoleEnfant = idRoleEnfant;
    }

    public String getIdRoleHeritage() { return getId(); }
    public void setIdRoleHeritage(String idRoleHeritage) { setId(idRoleHeritage); }

    public String getIdRoleParent() { return idRoleParent; }
    public void setIdRoleParent(String idRoleParent) { this.idRoleParent = idRoleParent; }

    public String getIdRoleEnfant() { return idRoleEnfant; }
    public void setIdRoleEnfant(String idRoleEnfant) { this.idRoleEnfant = idRoleEnfant; }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }
}