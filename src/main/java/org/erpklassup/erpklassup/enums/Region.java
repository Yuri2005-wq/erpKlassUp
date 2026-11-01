package org.erpklassup.erpklassup.enums;

public enum Region {
    ADAMAOUA("Adamaoua"),
    CENTRE("Centre"),
    EST("Est"),
    EXTREME_NORD("Extrême-Nord"),
    LITTORAL("Littoral"),
    NORD("Nord"),
    NORD_OUEST("Nord-Ouest"),
    OUEST("Ouest"),
    SUD("Sud"),
    SUD_OUEST("Sud-Ouest"),
    AUTRE("Autre");

    private final String libelle;

    Region(String libelle) {
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