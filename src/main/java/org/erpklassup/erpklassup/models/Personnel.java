package org.erpklassup.erpklassup.models;

import java.time.LocalDate;

public class Personnel extends BaseEntity {
    private String idEcole;
    private String idUtilisateur;
    private String matriculeInterne;
    private String nom;
    private String prenom;
    private String sexe;
    private LocalDate dateNaissance;
    private String telephone;
    private String contactUrgence;
    private String email;
    private String cniOuNiu;
    private String typePersonnel;
    private String posteOuFonction;
    private String qualification;
    private String statutContractuel;
    private LocalDate dateEmbauche;
    private String photo;
    private boolean estActif;

    public Personnel() {
        super();
        this.estActif = true;
        this.statutContractuel = "PERMANENT";
    }

    public String getIdPersonnel() { return getId(); }
    public void setIdPersonnel(String idPersonnel) { setId(idPersonnel); }
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }
    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    public String getMatriculeInterne() { return matriculeInterne; }
    public void setMatriculeInterne(String matriculeInterne) { this.matriculeInterne = matriculeInterne; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getContactUrgence() { return contactUrgence; }
    public void setContactUrgence(String contactUrgence) { this.contactUrgence = contactUrgence; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCniOuNiu() { return cniOuNiu; }
    public void setCniOuNiu(String cniOuNiu) { this.cniOuNiu = cniOuNiu; }
    public String getTypePersonnel() { return typePersonnel; }
    public void setTypePersonnel(String typePersonnel) { this.typePersonnel = typePersonnel; }
    public String getPosteOuFonction() { return posteOuFonction; }
    public void setPosteOuFonction(String posteOuFonction) { this.posteOuFonction = posteOuFonction; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getStatutContractuel() { return statutContractuel; }
    public void setStatutContractuel(String statutContractuel) { this.statutContractuel = statutContractuel; }
    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }
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