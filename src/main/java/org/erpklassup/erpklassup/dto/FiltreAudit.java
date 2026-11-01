package org.erpklassup.erpklassup.dto;

import java.time.LocalDate;

/**
 * Filtre de recherche pour la table {@code LogsAudit}.
 *
 * <p>Permet de filtrer les lignes d'audit selon plusieurs critères :
 * <ul>
 *   <li><b>idEcole</b> : identifiant de l'école (obligatoire)</li>
 *   <li><b>idUtilisateurAuteur</b> : filtrer les actions faites PAR un utilisateur
 *       (à utiliser pour l'onglet "Mes activités")</li>
 *   <li><b>terme</b> : recherche textuelle sur l'auteur, l'action ou les détails</li>
 *   <li><b>dateDebut</b> / <b>dateFin</b> : plage de dates</li>
 *   <li><b>categorie</b> : filtrer par catégorie d'action</li>
 * </ul>
 * </p>
 */
public record FiltreAudit(
        String idEcole,
        String idUtilisateurAuteur,
        String terme,
        LocalDate dateDebut,
        LocalDate dateFin,
        String categorie
) {

    /* ============================================================
       CONSTRUCTEURS DE COMPATIBILITÉ
       ============================================================ */

    /**
     * Constructeur historique (sans filtre par auteur).
     * Utilisé par les écrans admin qui consultent toutes les actions.
     */
    public FiltreAudit(String idEcole, String terme,
                       LocalDate dateDebut, LocalDate dateFin,
                       String categorie) {
        this(idEcole, null, terme, dateDebut, dateFin, categorie);
    }

    /**
     * Constructeur simple (juste école + terme).
     * Pratique pour les recherches rapides.
     */
    public FiltreAudit(String idEcole, String terme) {
        this(idEcole, null, terme, null, null, null);
    }

    /* ============================================================
       HELPERS
       ============================================================ */

    /**
     * Retourne true si un filtre par auteur est actif.
     */
    public boolean aUnFiltreAuteur() {
        return idUtilisateurAuteur != null && !idUtilisateurAuteur.isBlank();
    }
}
//package org.erpklassup.erpklassup.dto;
//
//import java.time.LocalDate;
//
//public record FiltreAudit(
//        String idEcole,
//        String terme,
//        LocalDate dateDebut,
//        LocalDate dateFin,
//        String categorie
//) {}