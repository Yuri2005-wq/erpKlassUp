package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.AnneeScolaireDAO;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.service.AnneeAccessGuard;
import org.erpklassup.erpklassup.service.AnneeScolaireCache;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.RafraichisseurAnneeScolaire;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;
import org.erpklassup.erpklassup.service.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service centralisé pour la gestion des années scolaires (CRUD + Cycle de vie).
 * Hérite de ServiceAsyncBase pour exécuter les opérations DAO/I/O de manière asynchrone
 * sans bloquer le JavaFX Application Thread.
 */

public class AnneeScolaireService extends ServiceAsyncBase {

    private final AnneeScolaireDAO anneeDAO;
    private final AuditService auditService;
    private final RafraichisseurAnneeScolaire rafraichisseur;

    public AnneeScolaireService() {
        this(new AnneeScolaireDAO(), new AuditService(), new RafraichisseurAnneeScolaire());
    }

    public AnneeScolaireService(AnneeScolaireDAO anneeDAO,
                                AuditService auditService,
                                RafraichisseurAnneeScolaire rafraichisseur) {
        this.anneeDAO = anneeDAO;
        this.auditService = auditService;
        this.rafraichisseur = rafraichisseur;
    }

    /* ============================================================
       1. LECTURES & RECHERCHES (Asynchrones & Synchrones)
       ============================================================ */

    public List<AnneeScolaire> listerAnnees(String idEcole) {
        return anneeDAO.findByEcole(idEcole);
    }

    public void listerAnneesAsync(String idEcole,
                                  Consumer<List<AnneeScolaire>> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> anneeDAO.findByEcole(idEcole), onSucces, onErreur);
    }

    public void rechercherAsync(String idEcole, String terme,
                                Consumer<List<AnneeScolaire>> onSucces,
                                Consumer<Throwable> onErreur) {
        executer(() -> anneeDAO.rechercher(idEcole, terme), onSucces, onErreur);
    }

        public List<AnneeScolaire> rechercher(String idEcole, String terme) {
        return anneeDAO.rechercher(idEcole, terme);
    }

    public void chargerCouranteAsync(String idEcole,
                                     Consumer<Optional<AnneeScolaire>> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> anneeDAO.findCourante(idEcole), onSucces, onErreur);
    }

    /* ============================================================
       2. ÉCRITURES & CRUD (Création, Modification, Suppression)
       ============================================================ */

    public void creerAsync(AnneeScolaire annee,
                           Runnable onSucces,
                           Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // Validation de saisie
            if (annee.getIdEcole() == null || annee.getIdEcole().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école est obligatoire.");
            }
            if (annee.getLibelle() == null || annee.getLibelle().isBlank()) {
                throw new IllegalArgumentException("Le libellé de l'année est obligatoire.");
            }
            if (annee.getDateDebut() == null || annee.getDateFin() == null) {
                throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
            }
            if (!annee.getDateFin().isAfter(annee.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
            }

            // Vérification unicité libellé
            if (anneeDAO.libelleExiste(annee.getIdEcole(), annee.getLibelle(), null)) {
                throw new IllegalStateException("Une année avec le libellé « " + annee.getLibelle() + " » existe déjà.");
            }

            // Si définie comme courante -> mise à jour des autres
            if (annee.isEstCourante()) {
                anneeDAO.definirCourante(annee.getId(), annee.getIdEcole());
            }

            // Insertion en base
            if (!anneeDAO.create(annee)) {
                throw new IllegalStateException("Impossible de créer l'année scolaire.");
            }

            appliquerApresChangement(annee);
        }, onSucces, onErreur);
    }

    public void modifierAsync(AnneeScolaire annee,
                              Runnable onSucces,
                              Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // Sécurité : Vérifie si l'année autorise les modifications
            AnneeAccessGuard.verifierEcriture(annee.getId());

            if (annee.getId() == null || annee.getId().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'année est obligatoire.");
            }
            if (annee.getLibelle() == null || annee.getLibelle().isBlank()) {
                throw new IllegalArgumentException("Le libellé de l'année est obligatoire.");
            }
            if (!annee.getDateFin().isAfter(annee.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
            }

            if (anneeDAO.libelleExiste(annee.getIdEcole(), annee.getLibelle(), annee.getId())) {
                throw new IllegalStateException("Une autre année avec le libellé « " + annee.getLibelle() + " » existe déjà.");
            }

            if (!anneeDAO.update(annee)) {
                throw new IllegalStateException("Échec de la mise à jour de l'année scolaire.");
            }

            appliquerApresChangement(annee);
        }, onSucces, onErreur);
    }

    public void definirCouranteAsync(String idAnnee, String idEcole,
                                     Runnable onSucces,
                                     Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (idAnnee == null || idAnnee.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'année est obligatoire.");
            }

            if (!anneeDAO.definirCourante(idAnnee, idEcole)) {
                throw new IllegalStateException("Impossible de définir l'année courante.");
            }

            // Invalidation du cache et rafraîchissement global
            AnneeScolaireCache.invalidate(idAnnee);
            rafraichisseur.verifier();
        }, onSucces, onErreur);
    }

    public void supprimerAsync(String idAnnee,
                               Runnable onSucces,
                               Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            AnneeAccessGuard.verifierEcriture(idAnnee);

            long nbClasses = anneeDAO.compterClasses(idAnnee);
            if (nbClasses > 0) {
                throw new IllegalStateException("Impossible de supprimer cette année : " + nbClasses + " classe(s) y sont rattachées.");
            }

            if (!anneeDAO.softDelete(idAnnee)) {
                throw new IllegalStateException("Impossible de supprimer l'année scolaire.");
            }

            AnneeScolaireCache.invalidate(idAnnee);
            rafraichisseur.verifier();
        }, onSucces, onErreur);
    }

    /* ============================================================
       3. CYCLE DE VIE (Clôture, Réouverture, Archivage)
       ============================================================ */

    public void cloturerAnneeAsync(String idAnnee,
                                   Runnable onSucces,
                                   Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (!SessionManager.getInstance().aLaPermission("ANNEE_CLOTURER")) {
                throw new SecurityException("Permission ANNEE_CLOTURER requise");
            }

            AnneeScolaire annee = anneeDAO.findById(idAnnee)
                    .orElseThrow(() -> new IllegalArgumentException("AnneeScolaire introuvable : " + idAnnee));

            if (annee.getStatut() != AnneeScolaire.Statut.EN_COURS) {
                throw new IllegalStateException("Impossible de clôturer : statut actuel = " + annee.getStatut() + " (attendu EN_COURS)");
            }

            annee.setStatut(AnneeScolaire.Statut.CLOTUREE);
            if (!anneeDAO.update(annee)) {
                throw new IllegalStateException("Échec de la clôture (voir logs DAO)");
            }

            appliquerApresChangement(annee);

            auditService.tracerActionAsync(
                    "ANNEE_SCOLAIRE", "CLOTURE_ANNEE",
                    "Année scolaire " + annee.getLibelle() + " (idAnnee=" + idAnnee + ") clôturée"
            );
        }, onSucces, onErreur);
    }

    public void reouvrirAnneeAsync(String idAnnee, String motif,
                                   Runnable onSucces,
                                   Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (!SessionManager.getInstance().aLaPermission("ANNEE_REOUVRIR")) {
                throw new SecurityException("Permission ANNEE_REOUVRIR requise");
            }
            if (motif == null || motif.isBlank()) {
                throw new IllegalArgumentException("Un motif de réouverture est obligatoire (traçabilité audit)");
            }

            AnneeScolaire annee = anneeDAO.findById(idAnnee)
                    .orElseThrow(() -> new IllegalArgumentException("AnneeScolaire introuvable : " + idAnnee));

            if (annee.getStatut() != AnneeScolaire.Statut.CLOTUREE) {
                throw new IllegalStateException("Impossible de réouvrir : statut actuel = " + annee.getStatut() + " (attendu CLOTUREE)");
            }

            annee.setStatut(AnneeScolaire.Statut.EN_COURS);
            if (!anneeDAO.update(annee)) {
                throw new IllegalStateException("Échec de la réouverture (voir logs DAO)");
            }

            appliquerApresChangement(annee);

            auditService.tracerActionAsync(
                    "ANNEE_SCOLAIRE", "REOUVERTURE_ANNEE",
                    "Année scolaire " + annee.getLibelle() + " (idAnnee=" + idAnnee + ") réouverte. Motif : " + motif
            );
        }, onSucces, onErreur);
    }

    public void archiverAnneeAsync(String idAnnee,
                                   Runnable onSucces,
                                   Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (!SessionManager.getInstance().aLaPermission("ANNEE_ARCHIVER")) {
                throw new SecurityException("Permission ANNEE_ARCHIVER requise");
            }

            AnneeScolaire annee = anneeDAO.findById(idAnnee)
                    .orElseThrow(() -> new IllegalArgumentException("AnneeScolaire introuvable : " + idAnnee));

            if (annee.getStatut() != AnneeScolaire.Statut.CLOTUREE) {
                throw new IllegalStateException("Impossible d'archiver : statut actuel = " + annee.getStatut() + " (attendu CLOTUREE)");
            }

            annee.setStatut(AnneeScolaire.Statut.ARCHIVEE);
            if (!anneeDAO.update(annee)) {
                throw new IllegalStateException("Échec de l'archivage (voir logs DAO)");
            }

            appliquerApresChangement(annee);

            auditService.tracerActionAsync(
                    "ANNEE_SCOLAIRE", "ARCHIVAGE_ANNEE",
                    "Année scolaire " + annee.getLibelle() + " (idAnnee=" + idAnnee + ") archivée"
            );
        }, onSucces, onErreur);
    }

    /* ============================================================
       4. OUTILS ET MÉTHODES PRIVÉES DE SYNCHRONISATION
       ============================================================ */

    public void libelleExisteAsync(String idEcole, String libelle, String idExclu,
                                   Consumer<Boolean> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> anneeDAO.libelleExiste(idEcole, libelle, idExclu), onSucces, onErreur);
    }

    public void compterClassesAsync(String idAnnee,
                                    Consumer<Long> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> anneeDAO.compterClasses(idAnnee), onSucces, onErreur);
    }

    /**
     * Invalide le cache en mémoire et force le rafraîchissement global du contexte UI.
     */
    private void appliquerApresChangement(AnneeScolaire annee) {
        // 1. Invalidation du cache local de l'année modifiée
        AnneeScolaireCache.invalidate(annee.getId());

        // 2. Notification / Synchronisation via le rafraîchisseur
        rafraichisseur.verifier();

        // 3. Mise à jour de la session active si l'année modifiée est celle en cours de consultation
        AnneeScolaire active = SessionManager.getInstance().getAnneeActive();
        if (active != null && active.getId().equals(annee.getId())) {
            SessionManager.getInstance().setAnneeActive(annee);
        }
    }
}



//package org.erpklassup.erpklassup.service.module2;
//
//import org.erpklassup.erpklassup.dao.module2.AnneeScolaireDAO;
//import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
//import org.erpklassup.erpklassup.service.ServiceAsyncBase;
//
//import java.util.List;
//import java.util.Optional;

//import java.util.function.Consumer;
//
//
//public class AnneeScolaireService extends ServiceAsyncBase {
//
//    private final AnneeScolaireDAO anneeDAO;
//
//    public AnneeScolaireService() {
//        this(new AnneeScolaireDAO());
//    }
//
//    public AnneeScolaireService(AnneeScolaireDAO anneeDAO) {
//        this.anneeDAO = anneeDAO;
//    }
//
//    public List<AnneeScolaire> listerAnnees(String idEcole) {
//        return anneeDAO.findByEcole(idEcole);
//    }
//
//    public List<AnneeScolaire> rechercher(String idEcole, String terme) {
//        return anneeDAO.rechercher(idEcole, terme);
//    }
//
//    public Optional<AnneeScolaire> getCourante(String idEcole) {
//        return anneeDAO.findCourante(idEcole);
//    }
//
//    public void listerAnneesAsync(String idEcole,
//                                  Consumer<List<AnneeScolaire>> onSucces,
//                                  Consumer<Throwable> onErreur) {
//        executer(() -> anneeDAO.findByEcole(idEcole), onSucces, onErreur);
//    }
//
//    /**
//     * Recherche asynchrone (avec debounce côté controller).
//     */
//    public void rechercherAsync(String idEcole, String terme,
//                                Consumer<List<AnneeScolaire>> onSucces,
//                                Consumer<Throwable> onErreur) {
//        executer(() -> anneeDAO.rechercher(idEcole, terme), onSucces, onErreur);
//    }
//
//    /**
//     * Charge l'année courante d'une école de manière asynchrone.
//     */
//    public void chargerCouranteAsync(String idEcole,
//                                     Consumer<Optional<AnneeScolaire>> onSucces,
//                                     Consumer<Throwable> onErreur) {
//        executer(() -> anneeDAO.findCourante(idEcole), onSucces, onErreur);
//    }
//
//    /**
//     * Crée une nouvelle année scolaire.
//     * Règle métier :
//     *   - Le libellé doit être unique par école
//     *   - La date de fin doit être postérieure à la date de début
//     *   - Si {@code estCourante} = true, on retire le flag des autres années
//     */
//    public void creerAsync(AnneeScolaire annee,
//                           Runnable onSucces,
//                           Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//
//            // 1. Validation
//            if (annee.getIdEcole() == null || annee.getIdEcole().isBlank()) {
//                throw new IllegalArgumentException("L'identifiant de l'école est obligatoire.");
//            }
//            if (annee.getLibelle() == null || annee.getLibelle().isBlank()) {
//                throw new IllegalArgumentException("Le libellé de l'année est obligatoire.");
//            }
//            if (annee.getDateDebut() == null || annee.getDateFin() == null) {
//                throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
//            }
//            if (!annee.getDateFin().isAfter(annee.getDateDebut())) {
//                throw new IllegalArgumentException(
//                        "La date de fin doit être postérieure à la date de début.");
//            }
//
//            // 2. Vérifier l'unicité du libellé
//            if (anneeDAO.libelleExiste(annee.getIdEcole(), annee.getLibelle(), null)) {
//                throw new IllegalStateException(
//                        "Une année avec le libellé « " + annee.getLibelle() + " » existe déjà.");
//            }
//
//            // 3. Si année courante → retirer le flag des autres
//            if (annee.isEstCourante()) {
//                anneeDAO.definirCourante(annee.getId(), annee.getIdEcole());
//            }
//
//            // 4. Créer l'année
//            if (!anneeDAO.create(annee)) {
//                throw new IllegalStateException("Impossible de créer l'année scolaire.");
//            }
//
//        }, onSucces, onErreur);
//    }
//
//    /**
//     * Modifie une année existante.
//     */
//    public void modifierAsync(AnneeScolaire annee,
//                              Runnable onSucces,
//                              Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//
//            // 1. Validation
//            if (annee.getId() == null || annee.getId().isBlank()) {
//                throw new IllegalArgumentException("L'identifiant de l'année est obligatoire.");
//            }
//            if (annee.getLibelle() == null || annee.getLibelle().isBlank()) {
//                throw new IllegalArgumentException("Le libellé de l'année est obligatoire.");
//            }
//            if (!annee.getDateFin().isAfter(annee.getDateDebut())) {
//                throw new IllegalArgumentException(
//                        "La date de fin doit être postérieure à la date de début.");
//            }
//
//            // 2. Vérifier l'unicité du libellé (en excluant l'année en cours)
//            if (anneeDAO.libelleExiste(annee.getIdEcole(), annee.getLibelle(), annee.getId())) {
//                throw new IllegalStateException(
//                        "Une autre année avec le libellé « " + annee.getLibelle() + " » existe déjà.");
//            }
//
//            // 3. Mettre à jour
//            if (!anneeDAO.update(annee)) {
//                throw new IllegalStateException("Deux années ne peuvent pas être COURANTE au même moment.");
//            }
//
//        }, onSucces, onErreur);
//    }
//
//    /**
//     * Définit une année comme courante.
//     * Transaction atomique : retire le flag des autres années de l'école.
//     */
//    public void definirCouranteAsync(String idAnnee, String idEcole,
//                                     Runnable onSucces,
//                                     Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            if (idAnnee == null || idAnnee.isBlank()) {
//                throw new IllegalArgumentException("L'identifiant de l'année est obligatoire.");
//            }
//            if (!anneeDAO.definirCourante(idAnnee, idEcole)) {
//                throw new IllegalStateException("Impossible de définir l'année courante.");
//            }
//        }, onSucces, onErreur);
//    }
//
//    /**
//     * Supprime (soft delete) une année.
//     * Règle métier : blocage si des classes y sont rattachées.
//     */
//    public void supprimerAsync(String idAnnee,
//                               Runnable onSucces,
//                               Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//
//            // 1. Vérifier qu'aucune classe n'est rattachée
//            long nbClasses = anneeDAO.compterClasses(idAnnee);
//            if (nbClasses > 0) {
//                throw new IllegalStateException(
//                        "Impossible de supprimer cette année : " + nbClasses
//                                + " classe(s) y sont rattachées. Supprimez-les d'abord.");
//            }
//
//            // 2. Supprimer
//            if (!anneeDAO.softDelete(idAnnee)) {
//                throw new IllegalStateException("Impossible de supprimer l'année scolaire.");
//            }
//
//        }, onSucces, onErreur);
//    }
//
//    /* ============================================================
//       HELPERS
//       ============================================================ */
//
//    /**
//     * Vérifie si un libellé d'année existe déjà (async).
//     */
//    public void libelleExisteAsync(String idEcole, String libelle, String idExclu,
//
//                                   Consumer<Boolean> onSucces,
//                                   Consumer<Throwable> onErreur) {
//        executer(() -> anneeDAO.libelleExiste(idEcole, libelle, idExclu),
//                onSucces, onErreur);
//    }
//
//    /**
//     * Compte les classes rattachées (async).
//     */
//    public void compterClassesAsync(String idAnnee,
//                                    Consumer<Long> onSucces,
//                                    Consumer<Throwable> onErreur) {
//        executer(() -> anneeDAO.compterClasses(idAnnee), onSucces, onErreur);
//    }
//}