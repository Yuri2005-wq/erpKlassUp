package org.erpklassup.erpklassup.models.module2;


import org.erpklassup.erpklassup.models.BaseEntity;

public class AffectationMatiere extends BaseEntity {

    private String idEcole;
    private String idClasse;
    private String idMatiere;
    private String idPersonnel;

    public AffectationMatiere() {
        super();
    }

    public AffectationMatiere(String idClasse, String idMatiere, String idPersonnel) {
        this();
        this.idClasse = idClasse;
        this.idMatiere = idMatiere;
        this.idPersonnel = idPersonnel;
    }

    // Alias ID
    public String getIdAffectation() { return getId(); }
    public void setIdAffectation(String idAffectation) { setId(idAffectation); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdClasse() { return idClasse; }
    public void setIdClasse(String idClasse) { this.idClasse = idClasse; }

    public String getIdMatiere() { return idMatiere; }
    public void setIdMatiere(String idMatiere) { this.idMatiere = idMatiere; }

    public String getIdPersonnel() { return idPersonnel; }
    public void setIdPersonnel(String idPersonnel) { this.idPersonnel = idPersonnel; }

    @Override
    public String toString() { return "AffectationMatiere [idClasse=" + idClasse + ", idMatiere=" + idMatiere + "]"; }
}