package org.erpklassup.erpklassup.enums;

public enum StatutDepartement {
    PLANIFIE("Planifié"),
    ACTIF("Actif"),
    INACTIF("Inactif"),
    ARCHIVE("Archivé");

    private final String libelle;

    StatutDepartement(String libelle) {
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