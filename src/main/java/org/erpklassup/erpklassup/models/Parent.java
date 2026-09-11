package org.erpklassup.erpklassup.models;

public class Parent extends BaseEntity {
    private String idEcole;
    private String idUtilisateur;   // nullable — parent sans accès portail tant que non activé
    private String nom;
    private String prenom;
    private String sexe;
    private String telephone;
    private String email;
    private String profession;
    private String adresse;
    private String photo;
    private boolean estActif;

    public Parent() {
        super();
        this.estActif = true;
    }

    public String getIdParent() { return getId(); }
    public void setIdParent(String idParent) { setId(idParent); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public boolean isEstActif() { return estActif; }
    public void setEstActif(boolean estActif) { this.estActif = estActif; }

    public String getNomComplet() {
        if (prenom != null && !prenom.isEmpty()) return prenom + " " + nom;
        return nom;
    }

    @Override
    public String toString() { return getNomComplet(); }
}