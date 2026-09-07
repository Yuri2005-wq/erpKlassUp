package org.erpklassup.erpklassup.models;

public class GroupeEcole extends BaseEntity {

    private String nomGroupe;
    private String nomPromoteur;
    private String contactPromoteur;
    private String emailPromoteur;
    private String siegeSocial;
    private String logoGroupe;
    private boolean isActive;

    public GroupeEcole() {
        super();
        this.isActive = true;
    }

    // Alias ID
    public String getIdGroupe() { return getId(); }
    public void setIdGroupe(String idGroupe) { setId(idGroupe); }

    // Getters et Setters
    public String getNomGroupe() { return nomGroupe; }
    public void setNomGroupe(String nomGroupe) { this.nomGroupe = nomGroupe; }

    public String getNomPromoteur() { return nomPromoteur; }
    public void setNomPromoteur(String nomPromoteur) { this.nomPromoteur = nomPromoteur; }

    public String getContactPromoteur() { return contactPromoteur; }
    public void setContactPromoteur(String contactPromoteur) { this.contactPromoteur = contactPromoteur; }

    public String getEmailPromoteur() { return emailPromoteur; }
    public void setEmailPromoteur(String emailPromoteur) { this.emailPromoteur = emailPromoteur; }

    public String getSiegeSocial() { return siegeSocial; }
    public void setSiegeSocial(String siegeSocial) { this.siegeSocial = siegeSocial; }

    public String getLogoGroupe() { return logoGroupe; }
    public void setLogoGroupe(String logoGroupe) { this.logoGroupe = logoGroupe; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return nomGroupe;
    }
}