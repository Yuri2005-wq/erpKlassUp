package org.erpklassup.erpklassup.dto;

import java.util.List;

/**
 * Bilan d'une opération de création multiple de classes.
 *
 * @param creees    nombre de classes créées avec succès
 * @param ignorees  nombre de classes ignorées (doublons)
 * @param erreurs   liste des messages d'erreur (vide si tout OK)
 */
public record ResultatCreationMasse(
        int creees,
        int ignorees,
        List<String> erreurs
) {
    public boolean estSucces() {
        return erreurs.isEmpty() && creees > 0;
    }

    public boolean partiel() {
        return !erreurs.isEmpty() || ignorees > 0;
    }
}