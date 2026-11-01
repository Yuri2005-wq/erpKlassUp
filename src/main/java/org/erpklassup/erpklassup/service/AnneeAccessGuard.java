package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.exception.AnneeVerrouilleeException;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;

/**
 * Point de passage obligé avant toute écriture sur une entité rattachée
 * à une AnneeScolaire (Note, Paiement, Inscription, Absence, Evaluation,
 * Classe, Tarification...).
 *
 * Usage dans un service existant, en première ligne de la méthode d'écriture :
 *
 *   public void enregistrerNote(Note note) {
 *       AnneeAccessGuard.verifierEcriture(note.getIdAnnee());
 *       // ... logique métier inchangée
 *   }
 */
public final class AnneeAccessGuard {

    private AnneeAccessGuard() {}

    /**
     * @throws AnneeVerrouilleeException si l'année est CLOTUREE ou ARCHIVEE
     */

    public static void verifierEcriture(String idAnnee) {
        AnneeScolaire annee = AnneeScolaireCache.get(idAnnee);
        if (!autoriseEcriture(annee.getStatut())) {
            throw new AnneeVerrouilleeException(annee.getLibelle(), annee.getId(), annee.getStatut());
        }
    }

    /** Variante silencieuse pour l'UI (désactiver un bouton sans lever d'exception). */
    public static boolean estModifiable(String idAnnee) {
        return autoriseEcriture(AnneeScolaireCache.get(idAnnee).getStatut());
    }

    private static boolean autoriseEcriture(AnneeScolaire.Statut statut) {
        return statut == AnneeScolaire.Statut.PREPARATION || statut == AnneeScolaire.Statut.EN_COURS;
    }
}
