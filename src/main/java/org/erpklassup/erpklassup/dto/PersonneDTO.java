package org.erpklassup.erpklassup.dto;

/**
 * Représente une personne (Personnel, Élève ou Parent) sans compte utilisateur,
 * retournée par la recherche de l'étape 2 de la création de compte.
 */
public class PersonneDTO {

    private final String matricule;
    private final String nom;
    private final String prenom;
    private final String infoSup;

    public PersonneDTO(String matricule, String nom, String prenom, String infoSup) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.infoSup = infoSup;
    }

    public String getMatricule() {
        return matricule;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getInfoSup() {
        return infoSup;
    }
}