package org.erpklassup.erpklassup.dto;

public enum StatutCompte {
    ACTIF("Actif"), INACTIF("Inactif"), VERROUILLE("Verrouillé");
    private final String libelle;
    StatutCompte(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}