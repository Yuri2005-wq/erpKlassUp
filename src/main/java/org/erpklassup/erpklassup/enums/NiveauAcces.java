package org.erpklassup.erpklassup.enums;

public enum NiveauAcces {
    LECTURE("Lecture seule"),
    ECRITURE("Lecture et écriture"),
    ADMIN("Administration complète");

    private final String libelle;

    NiveauAcces(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }

    public static NiveauAcces fromString(String valeur) {
        if (valeur == null) return LECTURE;
        for (NiveauAcces niveau : values()) {
            if (niveau.name().equalsIgnoreCase(valeur)) {
                return niveau;
            }
        }
        return LECTURE;
    }

    @Override
    public String toString() {
        return libelle;
    }
}