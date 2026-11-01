package org.erpklassup.erpklassup.exception;

import org.erpklassup.erpklassup.models.module2.AnneeScolaire;

/**
 * Levée lorsqu'un service tente une écriture (INSERT/UPDATE/DELETE)
 * sur une entité rattachée à une AnneeScolaire dont le statut ne permet
 * plus la modification (CLOTUREE ou ARCHIVEE).
 *
 * RuntimeException volontairement : permet de l'ajouter aux services
 * existants sans changer leur signature.
 */
public class AnneeVerrouilleeException extends RuntimeException {

    private final String idAnnee;
    private final AnneeScolaire.Statut statutActuel;

    public AnneeVerrouilleeException(String libelleAnnee, String idAnnee, AnneeScolaire.Statut statutActuel) {
        super("Année scolaire " + libelleAnnee + " verrouillée (statut : " + statutActuel + ") — écriture refusée");
        this.idAnnee = idAnnee;
        this.statutActuel = statutActuel;
    }

    public String getIdAnnee() {
        return idAnnee;
    }

    public AnneeScolaire.Statut getStatutActuel() {
        return statutActuel;
    }
}
