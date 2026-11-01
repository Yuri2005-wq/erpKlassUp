package org.erpklassup.erpklassup.dto;

public record PosteLigne(
        String idPoste,
        String intitule,
        String categorie,
        String description,
        String idDepartement,
        String departementLibelle,
        int effectifActuel,
        boolean estActif
) {
}