package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

public class Niveau extends BaseEntity {

    private String idEcole;
    private String nomNiveau;
    private String systeme; // FRANCOPHONE, ANGLOPHONE
    private String cycle;   // MATERNELLE, PRIMAIRE, PREMIER_CYCLE, SECOND_CYCLE
    private int ordre;

    public Niveau() {
        super();
        this.systeme = "FRANCOPHONE";
        this.ordre = 0;
    }

    public Niveau(String nomNiveau, String cycle) {
        this();
        this.nomNiveau = nomNiveau;
        this.cycle = cycle;
    }

    // Alias ID
    public String getIdNiveau() { return getId(); }
    public void setIdNiveau(String idNiveau) { setId(idNiveau); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getNomNiveau() { return nomNiveau; }
    public void setNomNiveau(String nomNiveau) { this.nomNiveau = nomNiveau; }

    public String getSysteme() { return systeme; }
    public void setSysteme(String systeme) { this.systeme = systeme; }

    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    @Override
    public String toString() { return nomNiveau; }
}
