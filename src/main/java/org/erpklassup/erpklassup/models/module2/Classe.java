package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.enums.NiveauClasse;
import org.erpklassup.erpklassup.models.BaseEntity;

import java.util.Objects;
import java.util.UUID;

public class Classe extends BaseEntity {
    public enum Vacation {
        MATIN("Matin"),
        APRES_MIDI("Après-midi"),
        SOIR("Soir"),
        JOURNEE_CONTINUE("Journée continue"),
        WEEKEND("Week-end");

        private final String libelle;

        Vacation(String libelle) { this.libelle = libelle; }

        public String getLibelle() { return libelle; }
    }

    /* ============================================================
       CHAMPS
       ============================================================ */
    private String idClasse;
    private String idEcole;
    private String idNiveau;
    private String idFiliere;         // null si 1er cycle
    private String idAnnee;
    private String idEnseignantTitulaire;
    private String idSalle;
    private String codeClasse;        // '6A', 'TleC1'
    private String nomClasse;
    private String nomSalle;         // '6ème A'
    private Vacation vacation = Vacation.JOURNEE_CONTINUE;
    private int capacite = 40;
    /* Champs transitoires (non persistés) pour l'affichage */
    private transient String nomNiveau;
    private transient String codeFiliere;
    private transient String libelleAnnee;
    private transient int effectifActuel;

    /* ============================================================
       CONSTRUCTEURS
       ============================================================ */
    public Classe() {
        super();
    }

    public Classe(String idEcole, String idNiveau, String idAnnee,
                  String codeClasse, String nomClasse, int capacite) {
        this.idEcole = Objects.requireNonNull(idEcole, "idEcole obligatoire");
        this.idNiveau = Objects.requireNonNull(idNiveau, "idNiveau obligatoire");
        this.idAnnee = Objects.requireNonNull(idAnnee, "idAnnee obligatoire");
        this.codeClasse = Objects.requireNonNull(codeClasse, "codeClasse obligatoire");
        this.nomClasse = Objects.requireNonNull(nomClasse, "nomClasse obligatoire");

        if (capacite <= 0) {
            throw new IllegalArgumentException("La capacité doit être > 0");
        }
        this.capacite = capacite;
    }
    public String getNomSalle() {
        return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }

    public boolean estComplete() {
        return effectifActuel >= capacite;
    }

    public double getTauxRemplissage() {
        if (capacite == 0) return 0;
        return (effectifActuel * 100.0) / capacite;
    }

    public boolean estSurchargee() {
        return getTauxRemplissage() > 100;
    }

    public boolean necessiteFiliere() {
        return idFiliere != null && !idFiliere.isBlank();
    }


    public String libelleComplet() {
        StringBuilder sb = new StringBuilder(nomClasse);
        if (libelleAnnee != null) {
            sb.append(" — ").append(libelleAnnee);
        }
        return sb.toString();
    }

    /**
     * Vérifie qu'on peut encore inscrire un élève.
     */
    public boolean peutAccueillir() {
        return !estComplete() && deletedAt == null;
    }

    /* ============================================================
       GETTERS / SETTERS
       ============================================================ */
    public String getIdClasse() { return getId(); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdNiveau() { return idNiveau; }
    public void setIdNiveau(String idNiveau) { this.idNiveau = idNiveau; }

    public String getIdFiliere() { return idFiliere; }
    public void setIdFiliere(String idFiliere) { this.idFiliere = idFiliere; }

    public String getIdAnnee() { return idAnnee; }
    public void setIdAnnee(String idAnnee) { this.idAnnee = idAnnee; }

    public String getIdEnseignantTitulaire() { return idEnseignantTitulaire; }
    public void setIdEnseignantTitulaire(String idEnseignantTitulaire) {
        this.idEnseignantTitulaire = idEnseignantTitulaire;
    }

    public String getIdSalle() { return idSalle; }
    public void setIdSalle(String idSalle) { this.idSalle = idSalle; }

    public String getCodeClasse() { return codeClasse; }
    public void setCodeClasse(String codeClasse) { this.codeClasse = codeClasse; }

    public String getNomClasse() { return nomClasse; }
    public void setNomClasse(String nomClasse) { this.nomClasse = nomClasse; }

    public Vacation getVacation() { return vacation; }
    public void setVacation(Vacation vacation) { this.vacation = vacation; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getNomNiveau() { return nomNiveau; }
    public void setNomNiveau(String nomNiveau) { this.nomNiveau = nomNiveau; }

    public String getCodeFiliere() { return codeFiliere; }
    public void setCodeFiliere(String codeFiliere) { this.codeFiliere = codeFiliere; }

    public String getLibelleAnnee() { return libelleAnnee; }
    public void setLibelleAnnee(String libelleAnnee) { this.libelleAnnee = libelleAnnee; }

    public int getEffectifActuel() { return effectifActuel; }
    public void setEffectifActuel(int effectifActuel) { this.effectifActuel = effectifActuel; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Classe classe)) return false;
        return Objects.equals(idClasse, classe.idClasse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idClasse);
    }

    @Override
    public String toString() {
        return "Classe{" +
                "idClasse='" + idClasse + '\'' +
                ", codeClasse='" + codeClasse + '\'' +
                ", nomClasse='" + nomClasse + '\'' +
                ", vacation=" + vacation +
                ", capacite=" + capacite +
                '}';
    }
}