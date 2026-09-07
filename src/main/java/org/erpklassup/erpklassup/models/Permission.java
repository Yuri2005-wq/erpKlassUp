package org.erpklassup.erpklassup.models;

public class Permission extends BaseEntity {

    private String codePermission;
    private String libelle;
    private String categorie;

    public Permission() {
        super();
    }

    public Permission(String codePermission, String libelle) {
        this();
        this.codePermission = codePermission;
        this.libelle = libelle;
    }

    // Alias ID
    public String getIdPermission() { return getId(); }
    public void setIdPermission(String idPermission) { setId(idPermission); }

    // Getters et Setters
    public String getCodePermission() { return codePermission; }
    public void setCodePermission(String codePermission) { this.codePermission = codePermission; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    @Override
    public String toString() { return libelle; }
}