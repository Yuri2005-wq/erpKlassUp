package org.erpklassup.erpklassup.dto;

import org.erpklassup.erpklassup.enums.StatutEmploi;

/**
 * Ligne d'affichage pour la liste du personnel.
 * Ne contient que les champs utiles à la vue : le reste est chargé à la sélection.
 */

public record PersonnelLigne(
        String idPersonnel,
        String matricule,
        String nom,
        String prenom,
        String nomComplet,
        String initiales,
        String poste,
        String nomDepartement,
        String email,
        String telephone,
        String photoPath,
        String statutEmploi,        // ACTIF | SUSPENDU | SORTI
        boolean estActif
) {


    /** Statut typé pour l'UI (filtre, badge, couleur). */
    public StatutEmploi statut() {
        if (!estActif) return  StatutEmploi.INACTIF;
//        if (!estActif) return  StatutEmploi.ACTIF;
        return StatutEmploi.ACTIF;
    }
}