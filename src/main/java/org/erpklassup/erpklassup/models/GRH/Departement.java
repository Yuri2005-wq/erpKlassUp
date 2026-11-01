package org.erpklassup.erpklassup.models.GRH;

import org.erpklassup.erpklassup.enums.SousSysteme;
import org.erpklassup.erpklassup.enums.StatutDepartement;
import org.erpklassup.erpklassup.enums.TypeDepartement;
import org.erpklassup.erpklassup.models.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Departement extends BaseEntity {

    private String idEcole;
    private String code;
    private String libelle;
    private String description;
    private TypeDepartement type;
    private SousSysteme sousSysteme;
    private Boolean estConseilEnseignement;
    private String idDepartementParent;
    private Integer niveau;
    private String chemin;
    private Integer ordreAffichage;
    private String idResponsable;
    private String idResponsableAdjoint;
    private StatutDepartement statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motifDesactivation;
    private String codeCentreCout;
    private String codeMineduc;
    private BigDecimal budgetAnnuel;
    private Integer effectifMax;
    private String email;
    private String telephone;
    private String localisation;

    public Departement() {
        super();
        this.type = TypeDepartement.ADMINISTRATIF;
        this.sousSysteme = SousSysteme.BILINGUE;
        this.estConseilEnseignement = false;
        this.statut = StatutDepartement.ACTIF;
    }

    public Departement(String code, String libelle, String idEcole) {
        this();
        this.code = code;
        this.libelle = libelle;
        this.idEcole = idEcole;
    }

    public String getIdDepartement() { return getId(); }
    public void setIdDepartement(String id) { setId(id); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TypeDepartement getType() { return type; }
    public void setType(TypeDepartement type) { this.type = type; }

    public SousSysteme getSousSysteme() { return sousSysteme; }
    public void setSousSysteme(SousSysteme sousSysteme) { this.sousSysteme = sousSysteme; }

    public Boolean getEstConseilEnseignement() { return estConseilEnseignement; }
    public void setEstConseilEnseignement(Boolean estConseilEnseignement) { this.estConseilEnseignement = estConseilEnseignement; }

    public String getIdDepartementParent() { return idDepartementParent; }
    public void setIdDepartementParent(String idDepartementParent) { this.idDepartementParent = idDepartementParent; }

    public Integer getNiveau() { return niveau; }
    public void setNiveau(Integer niveau) { this.niveau = niveau; }

    public String getChemin() { return chemin; }
    public void setChemin(String chemin) { this.chemin = chemin; }

    public Integer getOrdreAffichage() { return ordreAffichage; }
    public void setOrdreAffichage(Integer ordreAffichage) { this.ordreAffichage = ordreAffichage; }

    public String getIdResponsable() { return idResponsable; }
    public void setIdResponsable(String idResponsable) { this.idResponsable = idResponsable; }

    public String getIdResponsableAdjoint() { return idResponsableAdjoint; }
    public void setIdResponsableAdjoint(String idResponsableAdjoint) { this.idResponsableAdjoint = idResponsableAdjoint; }

    public StatutDepartement getStatut() { return statut; }
    public void setStatut(StatutDepartement statut) { this.statut = statut; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getMotifDesactivation() { return motifDesactivation; }
    public void setMotifDesactivation(String motifDesactivation) { this.motifDesactivation = motifDesactivation; }

    public String getCodeCentreCout() { return codeCentreCout; }
    public void setCodeCentreCout(String codeCentreCout) { this.codeCentreCout = codeCentreCout; }

    public String getCodeMineduc() { return codeMineduc; }
    public void setCodeMineduc(String codeMineduc) { this.codeMineduc = codeMineduc; }

    public BigDecimal getBudgetAnnuel() { return budgetAnnuel; }
    public void setBudgetAnnuel(BigDecimal budgetAnnuel) { this.budgetAnnuel = budgetAnnuel; }

    public Integer getEffectifMax() { return effectifMax; }
    public void setEffectifMax(Integer effectifMax) { this.effectifMax = effectifMax; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    @Override
    public String toString() {
        return libelle;
    }
}