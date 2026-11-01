package org.erpklassup.erpklassup.enums;

public enum TypeDepartement {
    ADMINISTRATIF("Administratif"),
    PEDAGOGIQUE("Pédagogique"),
    TECHNIQUE("Technique"),
    SUPPORT("Support");

    private final String libelle;

    TypeDepartement(String libelle) {
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