package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PeriodeEvaluation extends BaseEntity {

    /* ============================================================
       CONSTANTES DE TYPE — évite les chaînes magiques dispersées
       dans le Service/Controller et centralise les valeurs valides
       de l'ENUM SQL typePeriode.
       ============================================================ */
    public static final String TYPE_SEQUENCE  = "SEQUENCE";
    public static final String TYPE_TRIMESTRE = "TRIMESTRE";
    public static final String TYPE_SEMESTRE  = "SEMESTRE";
    public static final String TYPE_ANNEE     = "ANNEE";

    private String idEcole;
    private String idAnnee;
    private String idPeriodeParent;
    private String codePeriode;
    private String libelle;
    private String typePeriode;
    private int numeroOrdre;
    private BigDecimal poids;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean estCloturee;

    /* ============================================================
       ✅ CHAMP TRANSITOIRE POUR L'ARBORESCENCE
       ============================================================
       Ce champ N'EST PAS persisté en base. Il est rempli par le DAO
       lors de l'appel à findArborescence() pour permettre de naviguer
       dans la hiérarchie Trimestre → Séquences.
       ============================================================ */
    private transient List<PeriodeEvaluation> sousPeriodes = new ArrayList<>();

    public PeriodeEvaluation() {
        super();
        this.poids = BigDecimal.valueOf(1.00);
        this.estCloturee = false;
    }

    public PeriodeEvaluation(String codePeriode, String libelle, String typePeriode, int numeroOrdre) {
        this();
        this.codePeriode = codePeriode;
        this.libelle = libelle;
        this.typePeriode = typePeriode;
        this.numeroOrdre = numeroOrdre;
    }

    /* ============================================================
       ✅ MÉTHODES ARBORESCENCE
       ============================================================ */

    public List<PeriodeEvaluation> getSousPeriodes() {
        if (sousPeriodes == null) {
            sousPeriodes = new ArrayList<>();
        }
        return sousPeriodes;
    }

    public void setSousPeriodes(List<PeriodeEvaluation> sousPeriodes) {
        this.sousPeriodes = (sousPeriodes != null) ? sousPeriodes : new ArrayList<>();
    }

    public void ajouterSousPeriode(PeriodeEvaluation sousPeriode) {
        if (sousPeriode == null) return;
        getSousPeriodes().add(sousPeriode);
    }

    public boolean aDesSousPeriodes() {
        return sousPeriodes != null && !sousPeriodes.isEmpty();
    }

    /**
     * Retourne true si cette période est une racine (pas de parent).
     * Couvre les 3 types qui peuvent légitimement être une racine :
     * TRIMESTRE, SEMESTRE (systèmes semestriels) et ANNEE (systèmes
     * à période unique, sans découpage en trimestres/semestres).
     */
    public boolean estRacine() {
        return idPeriodeParent == null
                && (TYPE_TRIMESTRE.equals(typePeriode)
                || TYPE_SEMESTRE.equals(typePeriode)
                || TYPE_ANNEE.equals(typePeriode));
    }

    public boolean estSousPeriode() {
        return idPeriodeParent != null;
    }

    /* ============================================================
       AUTRES MÉTHODES MÉTIER
       ============================================================ */

    public boolean contient(LocalDate date) {
        if (date == null || dateDebut == null || dateFin == null) return false;
        return !date.isBefore(dateDebut) && !date.isAfter(dateFin);
    }

    public boolean autoriseSaisieNotes() {
        return !estCloturee && getDeletedAt() == null;
    }

    public void cloturer() {
        this.estCloturee = true;
        this.setUpdatedAt(java.time.LocalDateTime.now());
    }

    public void reouvrir() {
        this.estCloturee = false;
        this.setUpdatedAt(java.time.LocalDateTime.now());
    }

    /**
     * Validation métier au niveau du modèle — vérifie les mêmes règles
     * que les contraintes SQL (NOT NULL, CHECK) mais en amont, pour
     * remonter un message clair au lieu d'attendre l'exception JDBC.
     * Retourne une liste vide si le modèle est valide.
     */
    public List<String> validerChamps() {
        List<String> erreurs = new ArrayList<>();

        if (idAnnee == null || idAnnee.isBlank()) {
            erreurs.add("L'année scolaire est obligatoire.");
        }
        if (codePeriode == null || codePeriode.isBlank()) {
            erreurs.add("Le code de la période est obligatoire.");
        }
        if (libelle == null || libelle.isBlank()) {
            erreurs.add("Le libellé est obligatoire.");
        }
        if (typePeriode == null || !(TYPE_SEQUENCE.equals(typePeriode) || TYPE_TRIMESTRE.equals(typePeriode)
                || TYPE_SEMESTRE.equals(typePeriode) || TYPE_ANNEE.equals(typePeriode))) {
            erreurs.add("Le type de période est invalide.");
        }
        if (dateDebut == null) {
            erreurs.add("La date de début est obligatoire.");
        }
        if (dateFin == null) {
            erreurs.add("La date de fin est obligatoire.");
        }
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            erreurs.add("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (poids == null || poids.signum() <= 0) {
            erreurs.add("Le poids doit être strictement positif.");
        }
        // Une racine ne doit pas avoir de parent ; une séquence doit en avoir un.
        if (TYPE_SEQUENCE.equals(typePeriode) && idPeriodeParent == null) {
            erreurs.add("Une séquence doit obligatoirement être rattachée à un trimestre ou semestre parent.");
        }
        if (!TYPE_SEQUENCE.equals(typePeriode) && idPeriodeParent != null && TYPE_ANNEE.equals(typePeriode)) {
            erreurs.add("Une période de type ANNEE ne peut pas avoir de parent.");
        }

        return erreurs;
    }

    public boolean estValide() {
        return validerChamps().isEmpty();
    }

    /* ============================================================
       GETTERS / SETTERS
       ============================================================ */
    public String getIdPeriode() { return getId(); }
    public void setIdPeriode(String idPeriode) { setId(idPeriode); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdAnnee() { return idAnnee; }
    public void setIdAnnee(String idAnnee) { this.idAnnee = idAnnee; }

    public String getIdPeriodeParent() { return idPeriodeParent; }
    public void setIdPeriodeParent(String idPeriodeParent) { this.idPeriodeParent = idPeriodeParent; }

    public String getCodePeriode() { return codePeriode; }
    public void setCodePeriode(String codePeriode) { this.codePeriode = codePeriode; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getTypePeriode() { return typePeriode; }
    public void setTypePeriode(String typePeriode) { this.typePeriode = typePeriode; }

    public int getNumeroOrdre() { return numeroOrdre; }
    public void setNumeroOrdre(int numeroOrdre) { this.numeroOrdre = numeroOrdre; }

    public BigDecimal getPoids() { return poids; }
    public void setPoids(BigDecimal poids) { this.poids = poids; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public boolean isEstCloturee() { return estCloturee; }
    public void setEstCloturee(boolean estCloturee) { this.estCloturee = estCloturee; }

    @Override
    public String toString() {
        return "PeriodeEvaluation{" +
                "codePeriode='" + codePeriode + '\'' +
                ", libelle='" + libelle + '\'' +
                ", typePeriode='" + typePeriode + '\'' +
                ", sousPeriodes=" + (sousPeriodes != null ? sousPeriodes.size() : 0) +
                '}';
    }
}