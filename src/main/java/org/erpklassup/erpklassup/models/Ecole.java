package org.erpklassup.erpklassup.models;

public class Ecole extends BaseEntity {

    private String idGroupe;
    private String codeEcole;
    private String nomEcole;
    private String numeroAgrement;
    private String numeroArreteOuverture;
    private String nomPromoteur;
    private String inspectionRef;
    private String delegationDept;
    private String region;
    private String sousSysteme;
    private String niveauEnseignement;
    private String adresse;
    private String telephone;
    private String email;
    private String devise;
    private String logoPath;
    private String cleSecrete;
    private boolean isActive;

    public Ecole() {
        super();
        this.isActive = true;
    }

    public Ecole(String codeEcole, String nomEcole) {
        this();
        this.codeEcole = codeEcole;
        this.nomEcole = nomEcole;
    }

    // Alias ID
    public String getIdEcole() { return getId(); }
    public void setIdEcole(String idEcole) { setId(idEcole); }

    // Getters et Setters
    public String getIdGroupe() { return idGroupe; }
    public void setIdGroupe(String idGroupe) { this.idGroupe = idGroupe; }

    public String getCodeEcole() { return codeEcole; }
    public void setCodeEcole(String codeEcole) { this.codeEcole = codeEcole; }

    public String getNomEcole() { return nomEcole; }
    public void setNomEcole(String nomEcole) { this.nomEcole = nomEcole; }

    public String getNumeroAgrement() { return numeroAgrement; }
    public void setNumeroAgrement(String numeroAgrement) { this.numeroAgrement = numeroAgrement; }

    public String getNumeroArreteOuverture() { return numeroArreteOuverture; }
    public void setNumeroArreteOuverture(String numeroArreteOuverture) { this.numeroArreteOuverture = numeroArreteOuverture; }

    public String getNomPromoteur() { return nomPromoteur; }
    public void setNomPromoteur(String nomPromoteur) { this.nomPromoteur = nomPromoteur; }

    public String getInspectionRef() { return inspectionRef; }
    public void setInspectionRef(String inspectionRef) { this.inspectionRef = inspectionRef; }

    public String getDelegationDept() { return delegationDept; }
    public void setDelegationDept(String delegationDept) { this.delegationDept = delegationDept; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getSousSysteme() { return sousSysteme; }
    public void setSousSysteme(String sousSysteme) { this.sousSysteme = sousSysteme; }

    public String getNiveauEnseignement() { return niveauEnseignement; }
    public void setNiveauEnseignement(String niveauEnseignement) { this.niveauEnseignement = niveauEnseignement; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getCleSecrete() { return cleSecrete; }
    public void setCleSecrete(String cleSecrete) { this.cleSecrete = cleSecrete; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return nomEcole;
    }
}