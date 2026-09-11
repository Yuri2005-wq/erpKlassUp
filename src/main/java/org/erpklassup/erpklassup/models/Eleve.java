package org.erpklassup.erpklassup.models;

import java.time.LocalDate;

public class Eleve extends BaseEntity {
    private String idEcole;
    private String idUtilisateur;   // nullable — élève sans compte tant que le portail n'est pas activé
    private String matricule;
    private String nom;
    private String prenom;
    private String sexe;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String adresse;
    private String nationalite;
    private String nomTuteur;
    private String telephoneTuteur;
    private String emailTuteur;
    private String photoPath;
    private String antecedentsMedicaux;

    public Eleve() {
        super();
        this.nationalite = "Camerounaise";
    }

    public String getIdEleve() { return getId(); }
    public void setIdEleve(String idEleve) { setId(idEleve); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getLieuNaissance() { return lieuNaissance; }
    public void setLieuNaissance(String lieuNaissance) { this.lieuNaissance = lieuNaissance; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getNationalite() { return nationalite; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    public String getNomTuteur() { return nomTuteur; }
    public void setNomTuteur(String nomTuteur) { this.nomTuteur = nomTuteur; }

    public String getTelephoneTuteur() { return telephoneTuteur; }
    public void setTelephoneTuteur(String telephoneTuteur) { this.telephoneTuteur = telephoneTuteur; }

    public String getEmailTuteur() { return emailTuteur; }
    public void setEmailTuteur(String emailTuteur) { this.emailTuteur = emailTuteur; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getAntecedentsMedicaux() { return antecedentsMedicaux; }
    public void setAntecedentsMedicaux(String antecedentsMedicaux) { this.antecedentsMedicaux = antecedentsMedicaux; }

    public String getNomComplet() {
        if (prenom != null && !prenom.isEmpty()) return prenom + " " + nom;
        return nom;
    }

    @Override
    public String toString() { return getNomComplet(); }
}