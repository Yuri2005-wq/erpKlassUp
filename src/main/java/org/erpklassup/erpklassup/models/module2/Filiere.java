package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

public class Filiere extends BaseEntity {

    private String idEcole;
    private String codeFiliere;
    private String nomFiliere;

    public Filiere() {
        super();
    }

    public Filiere(String codeFiliere, String nomFiliere) {
        this();
        this.codeFiliere = codeFiliere;
        this.nomFiliere = nomFiliere;
    }

    // Alias ID
    public String getIdFiliere() { return getId(); }
    public void setIdFiliere(String idFiliere) { setId(idFiliere); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getCodeFiliere() { return codeFiliere; }
    public void setCodeFiliere(String codeFiliere) { this.codeFiliere = codeFiliere; }

    public String getNomFiliere() { return nomFiliere; }
    public void setNomFiliere(String nomFiliere) { this.nomFiliere = nomFiliere; }

    @Override
    public String toString() { return nomFiliere; }
}
