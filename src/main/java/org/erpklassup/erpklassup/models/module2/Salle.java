package org.erpklassup.erpklassup.models.module2;

import org.erpklassup.erpklassup.models.BaseEntity;

import java.util.Objects;

public class Salle extends BaseEntity {

    /* ============================================================
       ENUM — Type de salle
       ============================================================ */
    public enum TypeSalle {
        SALLE("Salle de cours",          "fth-home"),
        LABO("Laboratoire",              "fth-droplet"),
        AMPHI("Amphithéâtre",            "fth-layers"),
        ATELIER("Atelier",               "fth-tool"),
        SALLE_INFO("Salle informatique", "fth-monitor"),
        BIBLIOTHEQUE("Bibliothèque",     "fth-book"),
        COUR("Cour / Extérieur",         "fth-sun");

        private final String libelle;
        private final String icone;

        TypeSalle(String libelle, String icone) {
            this.libelle = libelle;
            this.icone = icone;
        }

        public String getLibelle() { return libelle; }
        public String getIcone()   { return icone; }

        public static TypeSalle fromString(String valeur) {
            if (valeur == null || valeur.isBlank()) return SALLE;
            try {
                return TypeSalle.valueOf(valeur.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return SALLE;
            }
        }
    }

    /* ============================================================
       CHAMPS
       ============================================================ */
    private String idEcole;
    private String codeSalle;
    private String nomSalle;
    private String batiment;
    private int capaciteMax;
    private TypeSalle typeSalle = TypeSalle.SALLE;

    // ✅ NOUVEAU : statut d'occupation
    private boolean occupee = false;

    /* ============================================================
       CONSTRUCTEURS
       ============================================================ */
    public Salle() {
        super();
        this.capaciteMax = 50;
        this.typeSalle = TypeSalle.SALLE;
        this.occupee = false;
    }

    public Salle(String codeSalle, String nomSalle) {
        this();
        this.codeSalle = Objects.requireNonNull(codeSalle, "codeSalle obligatoire");
        this.nomSalle = Objects.requireNonNull(nomSalle, "nomSalle obligatoire");
    }

    public Salle(String idEcole, String codeSalle, String nomSalle,
                 String batiment, int capaciteMax, TypeSalle typeSalle) {
        this();
        this.idEcole = idEcole;
        this.codeSalle = codeSalle;
        this.nomSalle = nomSalle;
        this.batiment = batiment;
        this.capaciteMax = capaciteMax;
        this.typeSalle = typeSalle != null ? typeSalle : TypeSalle.SALLE;
    }

    /* ============================================================
       MÉTHODES MÉTIER
       ============================================================ */

    public String libelleComplet() {
        StringBuilder sb = new StringBuilder();
        if (codeSalle != null) sb.append(codeSalle);
        if (nomSalle != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(nomSalle);
        }
        if (batiment != null && !batiment.isBlank()) {
            sb.append(" (").append(batiment).append(")");
        }
        return sb.toString();
    }

    public boolean peutAccueillir(int nbEleves) {
        return nbEleves <= capaciteMax;
    }

    public boolean estSpecialisee() {
        return typeSalle != TypeSalle.SALLE && typeSalle != TypeSalle.COUR;
    }

    /**
     * ✅ Marque la salle comme occupée.
     */
    public void occuper() {
        this.occupee = true;
    }

    /**
     * ✅ Marque la salle comme libre.
     */
    public void liberer() {
        this.occupee = false;
    }

    /**
     * ✅ Bascule l'état d'occupation.
     */
    public void basculerOccupation() {
        this.occupee = !this.occupee;
    }

    /**
     * ✅ Retourne true si la salle est libre.
     */
    public boolean estLibre() {
        return !occupee;
    }

    /* ============================================================
       GETTERS / SETTERS
       ============================================================ */
    public String getIdSalle() { return getId(); }
    public void setIdSalle(String idSalle) { setId(idSalle); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getCodeSalle() { return codeSalle; }
    public void setCodeSalle(String codeSalle) { this.codeSalle = codeSalle; }

    public String getNomSalle() { return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }

    public String getBatiment() { return batiment; }
    public void setBatiment(String batiment) { this.batiment = batiment; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }

    public TypeSalle getTypeSalle() { return typeSalle; }
    public void setTypeSalle(TypeSalle typeSalle) {
        this.typeSalle = typeSalle != null ? typeSalle : TypeSalle.SALLE;
    }

    // ✅ Nouveau getter/setter
    public boolean isOccupee() { return occupee; }
    public void setOccupee(boolean occupee) { this.occupee = occupee; }

    @Override
    public String toString() {
        return "Salle{" +
                "idSalle='" + getId() + '\'' +
                ", codeSalle='" + codeSalle + '\'' +
                ", nomSalle='" + nomSalle + '\'' +
                ", typeSalle=" + typeSalle +
                ", capaciteMax=" + capaciteMax +
                ", occupee=" + occupee +
                '}';
    }
}