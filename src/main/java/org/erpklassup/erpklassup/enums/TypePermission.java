package org.erpklassup.erpklassup.enums;

public enum TypePermission {
    READ("Lecture"),
    CREATE("Création"),
    WRITE("Modification"),
    DELETE("Suppression"),
    SPECIAL("Spéciale");

    private final String libelle;

    TypePermission(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }

    public static TypePermission fromString(String valeur) {
        if (valeur == null) return SPECIAL;
        for (TypePermission type : values()) {
            if (type.name().equalsIgnoreCase(valeur)) {
                return type;
            }
        }
        return SPECIAL;
    }

    @Override
    public String toString() {
        return libelle;
    }
}