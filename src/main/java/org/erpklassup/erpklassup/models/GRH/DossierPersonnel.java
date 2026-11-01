package org.erpklassup.erpklassup.models.GRH;


import org.erpklassup.erpklassup.models.BaseEntity;

public class DossierPersonnel extends BaseEntity {

    private String idEcole;
    private String idPersonnel;      // 1-1 avec Personnel
    private String idDepartement;
    private String idSuperieur;      // -> Personnel
    private String idGrade;          // -> GradeSalarial
    private String numeroCNPS;
    private int nombreEnfantsCharge;
    private String modePaiementPrefere; // VIREMENT | ORANGE_MONEY | MTN_MOMO | ESPECES | CHEQUE
    private String banque;
    private String numeroCompte;
    private String numeroMobileMoney;

    public DossierPersonnel() {
        super();
        this.nombreEnfantsCharge = 0;
    }

    public String getIdDossier() { return getId(); }
    public void setIdDossier(String id) { setId(id); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdPersonnel() { return idPersonnel; }
    public void setIdPersonnel(String idPersonnel) { this.idPersonnel = idPersonnel; }

    public String getIdDepartement() { return idDepartement; }
    public void setIdDepartement(String idDepartement) { this.idDepartement = idDepartement; }

    public String getIdSuperieur() { return idSuperieur; }
    public void setIdSuperieur(String idSuperieur) { this.idSuperieur = idSuperieur; }

    public String getIdGrade() { return idGrade; }
    public void setIdGrade(String idGrade) { this.idGrade = idGrade; }

    public String getNumeroCNPS() { return numeroCNPS; }
    public void setNumeroCNPS(String numeroCNPS) { this.numeroCNPS = numeroCNPS; }

    public int getNombreEnfantsCharge() { return nombreEnfantsCharge; }
    public void setNombreEnfantsCharge(int n) { this.nombreEnfantsCharge = n; }

    public String getModePaiementPrefere() { return modePaiementPrefere; }
    public void setModePaiementPrefere(String m) { this.modePaiementPrefere = m; }

    public String getBanque() { return banque; }
    public void setBanque(String banque) { this.banque = banque; }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getNumeroMobileMoney() { return numeroMobileMoney; }
    public void setNumeroMobileMoney(String numeroMobileMoney) { this.numeroMobileMoney = numeroMobileMoney; }

    @Override
    public String toString() { return "Dossier[" + idPersonnel + "]"; }
}