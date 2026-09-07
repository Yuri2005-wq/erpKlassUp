package org.erpklassup.erpklassup.models;

public class ActionPermission extends BaseEntity {

    private String codeAction;
    private String libelleAction;
    private String idPermission;
    private String module;
    private boolean estActive;

    public ActionPermission() {
        super();
        this.estActive = true;
    }

    public ActionPermission(String codeAction, String libelleAction, String idPermission, String module) {
        this();
        this.codeAction = codeAction;
        this.libelleAction = libelleAction;
        this.idPermission = idPermission;
        this.module = module;
    }

    public String getIdActionPermission() { return getId(); }
    public void setIdActionPermission(String idActionPermission) { setId(idActionPermission); }

    public String getCodeAction() { return codeAction; }
    public void setCodeAction(String codeAction) { this.codeAction = codeAction; }

    public String getLibelleAction() { return libelleAction; }
    public void setLibelleAction(String libelleAction) { this.libelleAction = libelleAction; }

    public String getIdPermission() { return idPermission; }
    public void setIdPermission(String idPermission) { this.idPermission = idPermission; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public boolean isEstActive() { return estActive; }
    public void setEstActive(boolean estActive) { this.estActive = estActive; }
}