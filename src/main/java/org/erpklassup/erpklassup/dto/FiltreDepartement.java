package org.erpklassup.erpklassup.dto;

import org.erpklassup.erpklassup.enums.StatutDepartement;
import org.erpklassup.erpklassup.enums.TypeDepartement;

public record FiltreDepartement(
        String idEcole,
        String terme,
        TypeDepartement type,
        String idResponsable,
        StatutDepartement statut,
        int page,
        int taillePage
) {
    public int offset() {
        return Math.max(0, (page - 1) * taillePage);
    }
}