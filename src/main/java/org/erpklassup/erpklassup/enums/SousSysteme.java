package org.erpklassup.erpklassup.enums;

public enum SousSysteme {
    FRANCOPHONE("Francophone"),
    ANGLOPHONE("Anglophone"),
    BILINGUE("Bilingue");

    private final String libelle;

    SousSysteme(String libelle) {
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