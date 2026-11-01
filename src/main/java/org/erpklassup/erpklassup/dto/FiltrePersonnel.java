package org.erpklassup.erpklassup.dto;

import org.erpklassup.erpklassup.enums.StatutEmploi;

/**
 * Critères de recherche pour la liste du personnel.
 * Tous les filtres sont optionnels sauf idEcole.
 * Le champ statutEmploi est typé (enum) pour cohérence avec FiltreUtilisateur.statut.
 */
public record FiltrePersonnel(
        String idEcole,
        String idDepartement,
        String typePersonnel,
        StatutEmploi statutEmploi,
        String terme,
        int page,
        int taillePage
) {
    public static final int TAILLE_PAGE_DEFAUT = 20;

    public FiltrePersonnel {
        if (page < 1) page = 1;
        if (taillePage < 1) taillePage = TAILLE_PAGE_DEFAUT;
    }

    public int offset() {
        return (page - 1) * taillePage;
    }
}