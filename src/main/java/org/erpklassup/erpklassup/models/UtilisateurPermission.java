package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;

public class UtilisateurPermission extends BaseEntity {

    private String idUtilisateur;
    private String idPermission;
    private LocalDateTime dateAttribution;
    private String attribuePar;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String motifAttribution;
    private boolean estActive;

    public UtilisateurPermission() {
        super();
        this.dateAttribution = LocalDateTime.now();
        this.estActive = true;
    }

    public UtilisateurPermission(String idUtilisateur, String idPermission) {
        this();
        this.idUtilisateur = idUtilisateur;
        this.idPermission = idPermission;
    }

    public String getIdUtilisateurPermission() { return getId(); }
    public void setIdUtilisateurPermission(String id) { setId(id); }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getIdPermission() { return idPermission; }
    public void setIdPermission(String idPermission) { this.idPermission = idPermission; }

    public LocalDateTime getDateAttribution() { return dateAttribution; }
    public void setDateAttribution(LocalDateTime date) { this.dateAttribution = date; }

    public String getAttribuePar() { return attribuePar; }
    public void setAttribuePar(String attribuePar) { this.attribuePar = attribuePar; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime date) { this.dateDebut = date; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime date) { this.dateFin = date; }

    public String getMotifAttribution() { return motifAttribution; }
    public void setMotifAttribution(String motif) { this.motifAttribution = motif; }

    public boolean isEstActive() { return estActive; }
    public void setEstActive(boolean estActive) { this.estActive = estActive; }
}