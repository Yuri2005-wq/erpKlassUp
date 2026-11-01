package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.module2.AnneeScolaireDAO;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;

import java.util.Optional;

/**
 * Vérifie périodiquement que l'AnneeScolaire actuellement affichée dans
 * l'UI (SessionManager.getAnneeActive()) est toujours à jour : le statut
 * a pu changer (clôture décidée par un collègue sur un autre poste,
 * reçue ici via la synchronisation) ou le flag "courante" a pu être
 * déplacé sur une autre année de la même école.
 *
 * Suit exactement le pattern de RafraichisseurPermissions : une méthode
 * verifier() appelée périodiquement par le scheduler déjà présent dans
 * SessionManager, sur le thread daemon dédié "ERP-Session-Background-Thread".
 *
 * Ne fait AUCUNE écriture — lecture seule, purement informatif pour l'UI.
 * Le verrouillage réel des écritures reste porté par AnneeAccessGuard,
 * qui lit AnneeScolaireCache indépendamment de cette classe.
 */
public class RafraichisseurAnneeScolaire {

    private final AnneeScolaireDAO anneeScolaireDAO = new AnneeScolaireDAO();

    public void verifier() {
        String idEcole = SessionManager.getInstance().getIdEcoleCourante();
        if (idEcole == null) return;

        AnneeScolaire anneeAffichee = SessionManager.getInstance().getAnneeActive();

        // 1. Chercher l'année courante actuelle en BDD pour l'école
        Optional<AnneeScolaire> optCourante = anneeScolaireDAO.findCourante(idEcole);

        if (optCourante.isEmpty()) {
            if (anneeAffichee != null) {
                System.err.println("⚠️ Aucune année scolaire courante trouvée en BDD.");
                SessionManager.getInstance().setAnneeActive(null);
            }
            return;
        }

        AnneeScolaire fraiche = optCourante.get();

        // 2. Vérifier s'il y a eu un changement (changement d'ID d'année active, ou de statut)
        boolean idChange = (anneeAffichee == null) || !fraiche.getId().equals(anneeAffichee.getId());
        boolean statutChange = (anneeAffichee != null) && (fraiche.getStatut() != anneeAffichee.getStatut());

        if (idChange || statutChange) {
            System.out.println("🔄 Nouvelle année active détectée : " + fraiche.getLibelle()
                    + " (" + fraiche.getStatut() + ")");

            if (anneeAffichee != null) {
                AnneeScolaireCache.invalidate(anneeAffichee.getId());
            }
            AnneeScolaireCache.invalidate(fraiche.getId());

            // Notification globale : met à jour la session ET déclenche Platform.runLater pour tous les écouteurs UI
            SessionManager.getInstance().setAnneeActive(fraiche);
        }
    }
}
