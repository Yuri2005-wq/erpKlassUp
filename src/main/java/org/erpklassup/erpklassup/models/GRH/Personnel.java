package org.erpklassup.erpklassup.models.GRH;

import org.erpklassup.erpklassup.models.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POJO aligné sur la table Personnel v2.
 * 32 colonnes métier + les colonnes techniques de BaseEntity (id, version, timestamps, deleted_at).
 *
 * Ne pas mapper les colonnes générées (matriculeInterne_actif, idUtilisateur_actif,
 * numCnps_actif) : MySQL les calcule.
 */
public class Personnel extends BaseEntity {

    // ===== Multi-tenant =====
    private String idEcole;

    // ===== Liens =====
    private String idUtilisateur;
    private String idDepartement;
    private String idResponsable;   // auto-référence : un employé peut être le chef d'un autre
    private String idEchelon;

    // ===== Identification & état civil =====
    private String matriculeInterne;
    private String nom;
    private String prenom;
    private String sexe;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String nationalite;
    private String situationMatrimoniale;
    private int nombreEnfants;
    private String numeroCni;       // remplace cniOuNiu
    private String niu;             // identifiant fiscal séparé
    private String numCnps;

    // ===== Contacts & adresse =====
    private String telephone;
    private String contactUrgence;
    private String contactUrgenceNom;
    private String email;
    private String adresse;
    private String ville;
    private String photo;

    // ===== Poste & horaires =====
    private String typePersonnel;
    private String posteOuFonction;
    private String qualification;
    private String statutContractuel;   // PERMANENT | VACATAIRE | STAGIAIRE...
    private BigDecimal quotaHeuresHebdo;
    private BigDecimal dechargeHeures;
    private LocalDate dateEmbauche;
    private LocalDate dateDepart;
    private String motifDepart;         // DEMISSION | FIN_CONTRAT | LICENCIEMENT | RETRAITE | DECES
    private String statutEmploi;        // ACTIF | SUSPENDU | SORTI

    // ===== Paie =====
    private String modePaiement;        // ESPECES | VIREMENT | MOBILE_MONEY | CHEQUE
    private String nomBanque;
    private String ibanOuRib;
    private String numeroMobileMoney;

    // ===== État =====
    private boolean estActif;

    // ------------------------------------------------------------------
    // Constructeur
    // ------------------------------------------------------------------
    public Personnel() {
        super();
        this.estActif = true;
        this.nombreEnfants = 0;
        this.statutContractuel = "PERMANENT";
        this.statutEmploi = "ACTIF";
        this.modePaiement = "ESPECES";
        this.dechargeHeures = BigDecimal.ZERO;
    }

    // ------------------------------------------------------------------
    // Alias ID hérité de BaseEntity
    // ------------------------------------------------------------------
    public String getIdPersonnel() { return getId(); }
    public void setIdPersonnel(String idPersonnel) { setId(idPersonnel); }

    // ------------------------------------------------------------------
    // Getters / Setters
    // ------------------------------------------------------------------

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getIdDepartement() { return idDepartement; }
    public void setIdDepartement(String idDepartement) { this.idDepartement = idDepartement; }

    public String getIdResponsable() { return idResponsable; }
    public void setIdResponsable(String idResponsable) { this.idResponsable = idResponsable; }

    public String getIdEchelon() { return idEchelon; }
    public void setIdEchelon(String idEchelon) { this.idEchelon = idEchelon; }

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

    public String getLieuNaissance() { return lieuNaissance; }
    public void setLieuNaissance(String lieuNaissance) { this.lieuNaissance = lieuNaissance; }

    public String getNationalite() { return nationalite; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    public String getSituationMatrimoniale() { return situationMatrimoniale; }
    public void setSituationMatrimoniale(String situationMatrimoniale) { this.situationMatrimoniale = situationMatrimoniale; }

    public int getNombreEnfants() { return nombreEnfants; }
    public void setNombreEnfants(int nombreEnfants) { this.nombreEnfants = nombreEnfants; }

    public String getNumeroCni() { return numeroCni; }
    public void setNumeroCni(String numeroCni) { this.numeroCni = numeroCni; }

    public String getNiu() { return niu; }
    public void setNiu(String niu) { this.niu = niu; }

    public String getNumCnps() { return numCnps; }
    public void setNumCnps(String numCnps) { this.numCnps = numCnps; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getContactUrgence() { return contactUrgence; }
    public void setContactUrgence(String contactUrgence) { this.contactUrgence = contactUrgence; }

    public String getContactUrgenceNom() { return contactUrgenceNom; }
    public void setContactUrgenceNom(String contactUrgenceNom) { this.contactUrgenceNom = contactUrgenceNom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getTypePersonnel() { return typePersonnel; }
    public void setTypePersonnel(String typePersonnel) { this.typePersonnel = typePersonnel; }

    public String getPosteOuFonction() { return posteOuFonction; }
    public void setPosteOuFonction(String posteOuFonction) { this.posteOuFonction = posteOuFonction; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getStatutContractuel() { return statutContractuel; }
    public void setStatutContractuel(String statutContractuel) { this.statutContractuel = statutContractuel; }

    public BigDecimal getQuotaHeuresHebdo() { return quotaHeuresHebdo; }
    public void setQuotaHeuresHebdo(BigDecimal quotaHeuresHebdo) { this.quotaHeuresHebdo = quotaHeuresHebdo; }

    public BigDecimal getDechargeHeures() { return dechargeHeures; }
    public void setDechargeHeures(BigDecimal dechargeHeures) { this.dechargeHeures = dechargeHeures; }

    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }

    public LocalDate getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

    public String getMotifDepart() { return motifDepart; }
    public void setMotifDepart(String motifDepart) { this.motifDepart = motifDepart; }

    public String getStatutEmploi() { return statutEmploi; }
    public void setStatutEmploi(String statutEmploi) { this.statutEmploi = statutEmploi; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public String getNomBanque() { return nomBanque; }
    public void setNomBanque(String nomBanque) { this.nomBanque = nomBanque; }

    public String getIbanOuRib() { return ibanOuRib; }
    public void setIbanOuRib(String ibanOuRib) { this.ibanOuRib = ibanOuRib; }

    public String getNumeroMobileMoney() { return numeroMobileMoney; }
    public void setNumeroMobileMoney(String numeroMobileMoney) { this.numeroMobileMoney = numeroMobileMoney; }

    public boolean isEstActif() {
        return "ACTIF".equals(this.statutEmploi);
    }    public void setEstActif(boolean estActif) { this.estActif = estActif; }

    // ------------------------------------------------------------------
    // Helpers d'affichage
    // ------------------------------------------------------------------
    public String getNomComplet() {
        if (prenom != null && !prenom.isBlank()) return prenom + " " + nom;
        return nom;
    }

    public String getInitiales() {
        StringBuilder sb = new StringBuilder();
        if (nom != null && !nom.isBlank()) sb.append(Character.toUpperCase(nom.charAt(0)));
        if (prenom != null && !prenom.isBlank()) sb.append(Character.toUpperCase(prenom.charAt(0)));
        return sb.toString();
    }

    @Override
    public String toString() { return getNomComplet(); }
}