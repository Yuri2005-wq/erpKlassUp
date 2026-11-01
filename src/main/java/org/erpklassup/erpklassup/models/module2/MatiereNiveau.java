package org.erpklassup.erpklassup.models.module2;


import org.erpklassup.erpklassup.models.BaseEntity;

import java.math.BigDecimal;

public class MatiereNiveau extends BaseEntity {

    private String idEcole;
    private String idMatiere;
    private String idNiveau;
    private String idFiliere;
    private BigDecimal coefficient;
    private boolean obligatoire;
    private int ordreAffichage;

    // Champs de confort : alimentes par jointure en LECTURE SEULE dans le DAO
    // (Matiere / Niveau / Filiere), jamais ecrits en base directement.
    private String nomMatiere;
    private String nomNiveau;
    private String cycle;      // MATERNELLE, PRIMAIRE, PREMIER_CYCLE, SECOND_CYCLE
    private String systeme;    // FRANCOPHONE, ANGLOPHONE
    private String nomFiliere;
    private String codeFiliere;

    public MatiereNiveau() {
        super();
        this.coefficient = BigDecimal.valueOf(1.00);
        this.obligatoire = true;
        this.ordreAffichage = 0;
    }

    public MatiereNiveau(String idMatiere, String idNiveau, BigDecimal coefficient) {
        this();
        this.idMatiere = idMatiere;
        this.idNiveau = idNiveau;
        this.coefficient = coefficient;
    }

    // Alias ID
    public String getIdMatiereNiveau() { return getId(); }
    public void setIdMatiereNiveau(String idMatiereNiveau) { setId(idMatiereNiveau); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdMatiere() { return idMatiere; }
    public void setIdMatiere(String idMatiere) { this.idMatiere = idMatiere; }

    public String getIdNiveau() { return idNiveau; }
    public void setIdNiveau(String idNiveau) { this.idNiveau = idNiveau; }

    public String getIdFiliere() { return idFiliere; }
    public void setIdFiliere(String idFiliere) { this.idFiliere = idFiliere; }

    public BigDecimal getCoefficient() { return coefficient; }
    public void setCoefficient(BigDecimal coefficient) { this.coefficient = coefficient; }

    public boolean isObligatoire() { return obligatoire; }
    public void setObligatoire(boolean obligatoire) { this.obligatoire = obligatoire; }

    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }

    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }

    public String getNomNiveau() { return nomNiveau; }
    public void setNomNiveau(String nomNiveau) { this.nomNiveau = nomNiveau; }

    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }

    public String getSysteme() { return systeme; }
    public void setSysteme(String systeme) { this.systeme = systeme; }

    public String getNomFiliere() { return nomFiliere; }
    public void setNomFiliere(String nomFiliere) { this.nomFiliere = nomFiliere; }

    @Override
    public String toString() { return "MatiereNiveau [idMatiere=" + idMatiere + ", idNiveau=" + idNiveau + "]"; }

    public String getCodeFiliere() {
        return codeFiliere;
    }

    public void setCodeFiliere(String codeFiliere) {
        this.codeFiliere = codeFiliere;
    }
}