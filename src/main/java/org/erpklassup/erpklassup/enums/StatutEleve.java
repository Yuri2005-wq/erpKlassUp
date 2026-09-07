package org.erpklassup.erpklassup.enums;

public enum StatutEleve {
    NOUVEAU("Nouveau"),
    REINSCRIT("Réinscrit"),
    PARTI("Parti");

    private final String libelle;

    StatutEleve(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }

    public static StatutEleve fromString(String valeur) {
        if (valeur == null) return NOUVEAU;
        for (StatutEleve statut : values()) {
            if (statut.name().equalsIgnoreCase(valeur)) {
                return statut;
            }
        }
        return NOUVEAU;
    }

    @Override
    public String toString() {
        return libelle;
    }
}