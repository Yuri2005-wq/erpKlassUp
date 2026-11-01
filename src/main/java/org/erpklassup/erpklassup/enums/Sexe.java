package org.erpklassup.erpklassup.enums;

// Sexe.java
public enum Sexe {
    MASCULIN("MASCULIN"),
    FEMININ("FÉMININ");

    private final String libelle;

    Sexe(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
