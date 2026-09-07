package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;

public class UtilisateurRole extends BaseEntity {

    private String idUtilisateur;
    private String idRole;
    private String idEcole;
    private LocalDateTime dateAttribution;
    private String attribuePar;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean estActive;

    public UtilisateurRole() {
        super();
        this.dateAttribution = LocalDateTime.now();
        this.estActive = true;
    }

    public UtilisateurRole(String idUtilisateur, String idRole) {
        this();
        this.idUtilisateur = idUtilisateur;
        this.idRole = idRole;
    }

    public UtilisateurRole(String idUtilisateur, String idRole, String idEcole) {
        this(idUtilisateur, idRole);
        this.idEcole = idEcole;
    }

    // Alias ID
    public String getIdUtilisateurRole() { return getId(); }
    public void setIdUtilisateurRole(String idUtilisateurRole) { setId(idUtilisateurRole); }

    // Getters et Setters
    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getIdRole() { return idRole; }
    public void setIdRole(String idRole) { this.idRole = idRole; }

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