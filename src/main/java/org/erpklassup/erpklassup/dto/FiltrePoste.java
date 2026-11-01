package org.erpklassup.erpklassup.dto;

public record FiltrePoste(
        String idEcole,
        String terme,
        String idDepartement,
        String categorie,
        Boolean estActif,
        int page,
        int taillePage
){
    public int offset() {
        return Math.max(0, (page - 1) * taillePage);
    }
}