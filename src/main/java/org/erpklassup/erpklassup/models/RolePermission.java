package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;

public class RolePermission extends BaseEntity {
    private String idRole;
    private String idPermission;
    private String idEcole;
    private LocalDateTime dateAttribution;
    private String attribuePar;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean estActive;

    public RolePermission() {
        super();
        this.dateAttribution = LocalDateTime.now();
        this.estActive = true;
    }

    public RolePermission(String idRole, String idPermission, String idEcole) {
        this();
        this.idRole = idRole;
        this.idPermission = idPermission;
        this.idEcole = idEcole;
    }

    public String getIdRolePermission() { return getId(); }
    public void setIdRolePermission(String idRolePermission) { setId(idRolePermission); }
    public String getIdRole() { return idRole; }
    public void setIdRole(String idRole) { this.idRole = idRole; }
    public String getIdPermission() { return idPermission; }
    public void setIdPermission(String idPermission) { this.idPermission = idPermission; }
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }
    public LocalDateTime getDateAttribution() { return dateAttribution; }
    public void setDateAttribution(LocalDateTime dateAttribution) { this.dateAttribution = dateAttribution; }
    public String getAttribuePar() { return attribuePar; }
    public void setAttribuePar(String attribuePar) { this.attribuePar = attribuePar; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public boolean isEstActive() { return estActive; }
    public void setEstActive(boolean estActive) { this.estActive = estActive; }
}