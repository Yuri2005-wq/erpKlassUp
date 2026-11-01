package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

public class GroupeMatiere extends BaseEntity {

    private String idEcole;
    private String codeGroupe;
    private String nomGroupe;
    private int ordreAffichage;

    public GroupeMatiere() {
        super();
        this.ordreAffichage = 1;
    }

    public GroupeMatiere(String codeGroupe, String nomGroupe, int ordreAffichage) {
        this();
        this.codeGroupe = codeGroupe;
        this.nomGroupe = nomGroupe;
        this.ordreAffichage = ordreAffichage;
    }

    // Alias ID
    public String getIdGroupe() { return getId(); }
    public void setIdGroupe(String idGroupe) { setId(idGroupe); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getCodeGroupe() { return codeGroupe; }
    public void setCodeGroupe(String codeGroupe) { this.codeGroupe = codeGroupe; }

    public String getNomGroupe() { return nomGroupe; }
    public void setNomGroupe(String nomGroupe) { this.nomGroupe = nomGroupe; }

    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }

    @Override
    public String toString() { return nomGroupe; }
}