package org.erpklassup.erpklassup.enums;

public enum Sexe {
    MASCULIN("Masculin"),
    FEMININ("Féminin");

    private final String libelle;

    Sexe(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }

    public static Sexe fromString(String valeur) {
        if (valeur == null) return MASCULIN;
        for (Sexe sexe : values()) {
            if (sexe.name().equalsIgnoreCase(valeur)) {
                return sexe;
            }
        }
        return MASCULIN;
    }

    @Override
    public String toString() {
        return libelle;
    }
}