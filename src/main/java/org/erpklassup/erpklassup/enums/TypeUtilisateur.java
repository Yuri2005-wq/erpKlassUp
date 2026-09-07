package org.erpklassup.erpklassup.enums;

public enum TypeUtilisateur {
    SUPER_ADMIN("Super Administrateur"),
    ADMIN_GROUPE("Administrateur Groupe"),
    ADMIN_ECOLE("Administrateur École"),
    PERSONNEL("Personnel"),
    PARENT("Parent"),
    ELEVE("Élève");

    private final String libelle;

    TypeUtilisateur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }

    public static TypeUtilisateur fromString(String valeur) {
        if (valeur == null) return PERSONNEL;
        for (TypeUtilisateur type : values()) {
            if (type.name().equalsIgnoreCase(valeur)) {
                return type;
            }
        }
        return PERSONNEL;
    }

    @Override
    public String toString() {
        return libelle;
    }
}