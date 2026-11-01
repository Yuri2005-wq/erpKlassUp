package org.erpklassup.erpklassup.enums;

public enum NiveauClasse {
    PETITE_SECTION("Petite Section"),
    MOYENNE_SECTION("Moyenne Section"),
    GRANDE_SECTION("Grande Section"),
    SIL("SIL"),
    CP("CP"),
    CE1("CE1"),
    CE2("CE2"),
    CM1("CM1"),
    CM2("CM2"),
    NURSERY_1("NURSERY_1"),
    NURSERY_2("NURSERY2"),
    NURSERY_3("NURSERY3"),
    CLASS_1("CLASS_1"),
    CLASS_2("CLASS_2"),
    CLASS_3("CLASS_3"),
    CLASS_4("CLASS_4"),
    CLASS_5("CLASS_5"),
    CLASS_6("CLASS_6");


    private final String libelle;

    NiveauClasse(String libelle) {
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
