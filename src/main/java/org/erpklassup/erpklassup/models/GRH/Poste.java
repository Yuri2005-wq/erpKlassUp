package org.erpklassup.erpklassup.models.GRH;


import org.erpklassup.erpklassup.models.BaseEntity;

public class Poste extends BaseEntity {

    private String idEcole;
    private String idDepartement;
    private String intitule;
    private String categorie;
    private String description;
    private boolean estActif;

    public Poste() {
        super();
        this.estActif = true;
    }

    public String getIdPoste() { return getId(); }
    public void setIdPoste(String id) { setId(id); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEstActif() { return estActif; }
    public void setEstActif(boolean estActif) { this.estActif = estActif; }
    public String getIdDepartement() { return idDepartement; }
    public void setIdDepartement(String idDepartement) { this.idDepartement = idDepartement; }

    @Override
    public String toString() { return intitule; }
}