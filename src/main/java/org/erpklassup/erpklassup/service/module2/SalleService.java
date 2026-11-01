package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.SalleDAO;
import org.erpklassup.erpklassup.models.module2.Classe;
import org.erpklassup.erpklassup.models.module2.Salle;
import org.erpklassup.erpklassup.models.module2.Salle.TypeSalle;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SalleService extends ServiceAsyncBase {

    /* ============================================================
       DÉPENDANCES
       ============================================================ */
    private final SalleDAO salleDAO;

    /* ============================================================
       CONSTRUCTEURS
       ============================================================ */
    public SalleService() {
        this(new SalleDAO());
    }

    public SalleService(SalleDAO salleDAO) {
        this.salleDAO = salleDAO;
    }

    /* ============================================================
       MÉTHODES SYNCHRONES (utilisées par AutoRefreshService)
       ============================================================ */
    public List<Salle> listerSalles(String idEcole) {
        return salleDAO.findByEcole(idEcole);
    }

    /**
     * Version synchrone : recherche avec filtres.
     */
    public List<Salle> rechercher(String idEcole, String terme,
                                  TypeSalle type, String batiment,
                                  Boolean occupee) {
        return salleDAO.rechercher(idEcole, terme, type, batiment, occupee);
    }

    /**
     * Version synchrone : recherche simple par terme.
     */
    public List<Salle> rechercher(String idEcole, String terme) {
        return salleDAO.rechercher(idEcole, terme);
    }

    /**
     * Version synchrone : récupère une salle par ID.
     */
    public Optional<Salle> findById(String idSalle) {
        return salleDAO.findById(idSalle);
    }

    /* ============================================================
       MÉTHODES ASYNCHRONES (utilisées par l'UI)
       ============================================================ */

    /**
     * Liste les salles d'une école de manière asynchrone.
     */
    public void listerSallesAsync(String idEcole,
                                  Consumer<List<Salle>> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.findByEcole(idEcole), onSucces, onErreur);
    }

    /**
     * Recherche asynchrone avec filtres.
     */
    public void rechercherAsync(String idEcole, String terme,
                                TypeSalle type, String batiment, Boolean occupee,
                                Consumer<List<Salle>> onSucces,
                                Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.rechercher(idEcole, terme, type, batiment, occupee),
                onSucces, onErreur);
    }

    /**
     * Recherche asynchrone simplifiée (terme uniquement).
     */
    public void rechercherAsync(String idEcole, String terme,
                                Consumer<List<Salle>> onSucces,
                                Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.rechercher(idEcole, terme), onSucces, onErreur);
    }

    /**
     * Charge une salle par ID.
     */
    public void chargerAsync(String idSalle,
                             Consumer<Optional<Salle>> onSucces,
                             Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.findById(idSalle), onSucces, onErreur);
    }

    /**
     * Crée une nouvelle salle.
     * Règles métier :
     *   - Le code salle doit être unique par école
     *   - La capacité doit être > 0
     *   - Le type doit être valide
     */
    public void creerAsync(Salle salle,
                           Runnable onSucces,
                           Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {

            // 1. Validation
            if (salle.getIdEcole() == null || salle.getIdEcole().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école est obligatoire.");
            }
            if (salle.getCodeSalle() == null || salle.getCodeSalle().isBlank()) {
                throw new IllegalArgumentException("Le code salle est obligatoire.");
            }
            if (salle.getNomSalle() == null || salle.getNomSalle().isBlank()) {
                throw new IllegalArgumentException("Le nom de la salle est obligatoire.");
            }
            if (salle.getCapaciteMax() <= 0) {
                throw new IllegalArgumentException("La capacité doit être supérieure à 0.");
            }
            if (salle.getTypeSalle() == null) {
                throw new IllegalArgumentException("Le type de salle est obligatoire.");
            }

            // 2. Vérifier l'unicité du code
            if (salleDAO.codeExiste(salle.getIdEcole(), salle.getCodeSalle(), null)) {
                throw new IllegalStateException(
                        "Une salle avec le code « " + salle.getCodeSalle() + " » existe déjà.");
            }

            // 3. Créer la salle
            if (!salleDAO.create(salle)) {
                throw new IllegalStateException("Impossible de créer la salle.");
            }

        }, onSucces, onErreur);
    }

    /**
     * Modifie une salle existante.
     */
    public void modifierAsync(Salle salle,
                              Runnable onSucces,
                              Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {

            // 1. Validation
            if (salle.getId() == null || salle.getId().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la salle est obligatoire.");
            }
            if (salle.getCodeSalle() == null || salle.getCodeSalle().isBlank()) {
                throw new IllegalArgumentException("Le code salle est obligatoire.");
            }
            if (salle.getCapaciteMax() <= 0) {
                throw new IllegalArgumentException("La capacité doit être supérieure à 0.");
            }

            // 2. Vérifier l'unicité du code (en excluant la salle en cours)
            if (salleDAO.codeExiste(salle.getIdEcole(), salle.getCodeSalle(), salle.getId())) {
                throw new IllegalStateException(
                        "Une autre salle avec le code « " + salle.getCodeSalle() + " » existe déjà.");
            }

            // 3. Mettre à jour
            if (!salleDAO.update(salle)) {
                throw new IllegalStateException("Impossible de modifier la salle.");
            }

        }, onSucces, onErreur);
    }

    /**
     * Supprime (soft delete) une salle.
     * Règle métier : blocage si des classes y sont rattachées.
     */
    public void supprimerAsync(String idSalle,
                               Runnable onSucces,
                               Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {

            // 1. Vérifier qu'aucune classe n'est rattachée
            long nbClasses = salleDAO.compterClasses(idSalle);
            if (nbClasses > 0) {
                throw new IllegalStateException(
                        "Impossible de supprimer cette salle : " + nbClasses
                                + " classe(s) y sont rattachées. Réaffectez-les d'abord.");
            }

            // 2. Supprimer
            if (!salleDAO.softDelete(idSalle)) {
                throw new IllegalStateException("Impossible de supprimer la salle.");
            }

        }, onSucces, onErreur);
    }

    /**
     * Bascule le statut d'occupation (occupée ↔ libre).
     */

    public void basculerOccupationAsync(String idSalle,
                                        Runnable onSucces,
                                        Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (idSalle == null || idSalle.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la salle est obligatoire.");
            }
            if (!salleDAO.basculerOccupation(idSalle)) {
                throw new IllegalStateException("Impossible de changer l'état de la salle.");
            }
        }, onSucces, onErreur);
    }

    /**
     * Définit explicitement le statut d'occupation.
     */
    public void setOccupationAsync(String idSalle, boolean occupee,
                                   Runnable onSucces,
                                   Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (idSalle == null || idSalle.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la salle est obligatoire.");
            }
            if (!salleDAO.setOccupation(idSalle, occupee)) {
                throw new IllegalStateException("Impossible de changer l'état de la salle.");
            }
        }, onSucces, onErreur);
    }

    /* ============================================================
       HELPERS
       ============================================================ */

    /**
     * Vérifie si un code salle existe déjà (async).
     */
    public void codeExisteAsync(String idEcole, String codeSalle, String idExclu,
                                Consumer<Boolean> onSucces,
                                Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.codeExiste(idEcole, codeSalle, idExclu),
                onSucces, onErreur);
    }

    /**
     * Compte les classes rattachées (async).
     */
    public void compterClassesAsync(String idSalle,
                                    Consumer<Long> onSucces,
                                    Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.compterClasses(idSalle), onSucces, onErreur);
    }

    /**
     * Compte le nombre total de salles (async).
     */
    public void compterParEcoleAsync(String idEcole,
                                     Consumer<Long> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.compterParEcole(idEcole), onSucces, onErreur);
    }

    /**
     * Compte les salles occupées (async).
     */
    public void compterSallesOccupeesAsync(String idEcole,
                                           Consumer<Long> onSucces,
                                           Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.compterSallesOccupees(idEcole), onSucces, onErreur);
    }

    /**
     * Liste distincte des bâtiments (async).
     */
    public void listerBatimentsAsync(String idEcole,
                                     Consumer<List<String>> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.listerBatiments(idEcole), onSucces, onErreur);
    }

    /**
     * Liste les salles libres (async).
     */
    public void listerSallesLibresAsync(String idEcole,
                                        Consumer<List<Salle>> onSucces,
                                        Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.listerSallesLibres(idEcole), onSucces, onErreur);
    }

    /**
     * Génère un code salle unique (async).
     */
    public void genererCodeSalleAsync(String idEcole,
                                      Consumer<String> onSucces,
                                      Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.genererCodeSalle(idEcole), onSucces, onErreur);
    }

    /* ============================================================
   DISPONIBILITÉ PAR VACATION (async)
   ============================================================ */

    /**
     * Vérifie si une salle est disponible pour une vacation donnée (async).
     */
    public void estDisponiblePourAsync(String idSalle,
                                       Classe.Vacation vacation,
                                       String idClasseExclue,
                                       Consumer<Boolean> onSucces,
                                       Consumer<Throwable> onErreur) {
        String vac = (vacation != null) ? vacation.name() : null;
        executer(() -> salleDAO.estDisponiblePour(idSalle, vac, idClasseExclue),
                onSucces, onErreur);
    }

    /**
     * Retourne la liste des vacations déjà utilisées pour une salle (async).
     */
    public void vacationsUtiliseesAsync(String idSalle,
                                        Consumer<List<String>> onSucces,
                                        Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.vacationsUtilisees(idSalle), onSucces, onErreur);
    }

    public void listerNomsClassesDansSalleAsync(String idSalle,
                                                Consumer<List<String>> onSucces,
                                                Consumer<Throwable> onErreur) {
        executer(() -> salleDAO.listerNomsClassesDansSalle(idSalle), onSucces, onErreur);
    }
}