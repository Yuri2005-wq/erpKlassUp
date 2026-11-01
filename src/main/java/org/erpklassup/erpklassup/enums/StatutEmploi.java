package org.erpklassup.erpklassup.enums;

/**
 * Statut d'emploi d'un membre du personnel.
 * Correspond à la colonne Personnel.statutEmploi (contrainte CHECK en base).
 */
public enum StatutEmploi {
    ACTIF("Actif"),
    SORTI("Sorti"),
    INACTIF("Inactif"),;

    private final String libelle;

    StatutEmploi(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }

    @Override
    public String toString() { return libelle; }
}
