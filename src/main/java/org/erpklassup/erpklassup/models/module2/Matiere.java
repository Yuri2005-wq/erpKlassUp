package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

import java.util.Objects;

public class Matiere extends BaseEntity {

    private String idEcole;
    private String idMatiereParent;
    private String idGroupeMatiere; // FK vers GroupeMatiere.idGroupe (remplace l'ancien enum groupeMatiere)
    private String codeMatiere;
    private String nomMatiere;
    private String domaine;
    private int ordreAffichage;
    private String description;

    // Champs de confort : alimentes par jointure en LECTURE SEULE dans le DAO,
    // jamais ecrits en base directement (pas de colonne correspondante sur Matiere).
    private String nomMatiereParent;
    private String nomGroupeMatiere;

    public Matiere() {
        super();
        this.ordreAffichage = 0;
    }

    public Matiere(String codeMatiere, String nomMatiere) {
        this();
        this.codeMatiere = codeMatiere;
        this.nomMatiere = nomMatiere;
    }

    // Alias ID
    public String getIdMatiere() { return getId(); }
    public void setIdMatiere(String idMatiere) { setId(idMatiere); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdMatiereParent() { return idMatiereParent; }
    public void setIdMatiereParent(String idMatiereParent) { this.idMatiereParent = idMatiereParent; }

    public String getIdGroupeMatiere() { return idGroupeMatiere; }
    public void setIdGroupeMatiere(String idGroupeMatiere) { this.idGroupeMatiere = idGroupeMatiere; }

    public String getCodeMatiere() { return codeMatiere; }
    public void setCodeMatiere(String codeMatiere) { this.codeMatiere = codeMatiere; }

    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }

    public String getDomaine() { return domaine; }
    public void setDomaine(String domaine) { this.domaine = domaine; }

    public int getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(int ordreAffichage) { this.ordreAffichage = ordreAffichage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNomMatiereParent() { return nomMatiereParent; }
    public void setNomMatiereParent(String nomMatiereParent) { this.nomMatiereParent = nomMatiereParent; }

    public String getNomGroupeMatiere() { return nomGroupeMatiere; }
    public void setNomGroupeMatiere(String nomGroupeMatiere) { this.nomGroupeMatiere = nomGroupeMatiere; }

    public boolean estRacine() { return idMatiereParent == null || idMatiereParent.isBlank(); }

    @Override
    public String toString() { return nomMatiere; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Matiere m)) return false;
        return Objects.equals(getId(), m.getId())
                && Objects.equals(getNomMatiere(), m.getNomMatiere())
                && Objects.equals(getCodeMatiere(), m.getCodeMatiere())
                && Objects.equals(getDomaine(), m.getDomaine())
                && Objects.equals(getOrdreAffichage(), m.getOrdreAffichage())
                && Objects.equals(getIdMatiereParent(), m.getIdMatiereParent())
                && Objects.equals(getIdGroupeMatiere(), m.getIdGroupeMatiere());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNomMatiere(), getCodeMatiere(),
                getDomaine(), getOrdreAffichage(), getIdMatiereParent(), getIdGroupeMatiere());
    }
}