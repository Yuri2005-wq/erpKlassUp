package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.MatiereDAO;
import org.erpklassup.erpklassup.models.module2.Matiere;
import org.erpklassup.erpklassup.service.Autorisation;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service pour l'entité {@link Matiere}.
 *
 * <p>Respecte le pattern async établi :</p>
 * <ul>
 *   <li>Méthodes synchrones brutes (délégation directe au DAO)</li>
 *   <li>Méthodes asynchrones avec {@code executer(...)} / {@code executerSansRetour(...)}</li>
 *   <li>{@code Autorisation.exigerAction(...)} dans les actions sensibles</li>
 *   <li>Callbacks {@code Consumer<List<Matiere>> onSucces} / {@code Consumer<Throwable> onErreur}</li>
 *   <li>Jetons de requête pour éviter les réponses obsolètes (recherche)</li>
 * </ul>
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class MatiereService extends ServiceAsyncBase {

    private final MatiereDAO matiereDAO;

    // ==========================================
    // CONSTRUCTEURS
    // ==========================================
    public MatiereService() {
        this(new MatiereDAO());
    }

    public MatiereService(MatiereDAO matiereDAO) {
        this.matiereDAO = matiereDAO;
    }

    // ==========================================
    // LECTURE — SYNCHRONE (pour auto-refresh)
    // ==========================================

    /** Liste complète des matières d'une école (synchrone). */
    public List<Matiere> listerParEcole(String idEcole) {
        return matiereDAO.findByEcole(idEcole);
    }

    /** Liste des matières racines d'une école (synchrone). */
    public List<Matiere> listerRacinesParEcole(String idEcole) {
        return matiereDAO.findRacinesByEcole(idEcole);
    }

    /** Recherche filtrée par terme (synchrone). */
    public List<Matiere> rechercher(String idEcole, String terme) {
        if (terme == null || terme.isBlank()) {
            return matiereDAO.findByEcole(idEcole);
        }
        return matiereDAO.search(idEcole, terme);
    }

    /** Récupère une matière par son id (synchrone). */
    public Optional<Matiere> trouverParId(String idMatiere) {
        return matiereDAO.findById(idMatiere);
    }

    // ==========================================
    // LECTURE — ASYNCHRONE
    // ==========================================

    public void listerParEcoleAsync(String idEcole,
                                    Consumer<List<Matiere>> onSucces,
                                    Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> matiereDAO.findByEcole(idEcole),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                onErreur);
    }

    public void listerRacinesParEcoleAsync(String idEcole,
                                           Consumer<List<Matiere>> onSucces,
                                           Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> matiereDAO.findRacinesByEcole(idEcole),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                onErreur);
    }

    /**
     * Recherche asynchrone (avec debounce côté controller).
     * Si le terme est vide, renvoie toutes les matières de l'école.
     */
    public void rechercherAsync(String idEcole, String terme,
                                Consumer<List<Matiere>> onSucces,
                                Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> {
                    if (terme == null || terme.isBlank()) {
                        return matiereDAO.findByEcole(idEcole);
                    }
                    return matiereDAO.search(idEcole, terme);
                },
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur   -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void trouverParIdAsync(String idMatiere,
                                  Consumer<Optional<Matiere>> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> matiereDAO.findById(idMatiere), onSucces, onErreur);
    }

    // ==========================================
    // ECRITURE — ASYNCHRONE
    // ==========================================

    /**
     * Crée une nouvelle matière.
     * Règle métier : le code doit être unique par école.
     */
    public void creerAsync(Matiere matiere,
                           Runnable onSucces,
                           Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // Autorisation.exigerAction("academique.matiere.creer");

            if (matiere.getIdEcole() == null || matiere.getIdEcole().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école est obligatoire.");
            }
            if (matiere.getCodeMatiere() == null || matiere.getCodeMatiere().isBlank()) {
                throw new IllegalArgumentException("Le code de la matière est obligatoire.");
            }
            if (matiere.getNomMatiere() == null || matiere.getNomMatiere().isBlank()) {
                throw new IllegalArgumentException("Le nom de la matière est obligatoire.");
            }

            if (matiereDAO.codeExiste(matiere.getIdEcole(), matiere.getCodeMatiere(), null)) {
                throw new IllegalStateException(
                        "Une matière avec le code « " + matiere.getCodeMatiere() + " » existe déjà.");
            }

            if (!matiereDAO.create(matiere)) {
                throw new IllegalStateException("Impossible de créer la matière.");
            }
        }, onSucces, onErreur);
    }

    /**
     * Modifie une matière existante.
     * Règle métier : le code doit rester unique par école (en excluant l'id courant).
     */
    public void modifierAsync(Matiere matiere,
                              Runnable onSucces,
                              Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // Autorisation.exigerAction("academique.matiere.modifier");

            if (matiere.getIdMatiere() == null || matiere.getIdMatiere().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la matière est obligatoire.");
            }
            if (matiere.getCodeMatiere() == null || matiere.getCodeMatiere().isBlank()) {
                throw new IllegalArgumentException("Le code de la matière est obligatoire.");
            }
            if (matiere.getNomMatiere() == null || matiere.getNomMatiere().isBlank()) {
                throw new IllegalArgumentException("Le nom de la matière est obligatoire.");
            }

            if (matiereDAO.codeExiste(matiere.getIdEcole(), matiere.getCodeMatiere(),
                    matiere.getIdMatiere())) {
                throw new IllegalStateException(
                        "Une autre matière avec le code « " + matiere.getCodeMatiere() + " » existe déjà.");
            }

            if (!matiereDAO.update(matiere)) {
                throw new IllegalStateException("Échec de la mise à jour de la matière.");
            }
        }, onSucces, onErreur);
    }

    /**
     * Suppression (soft delete) d'une matière.
     * Règle métier : blocage si des sous-matières actives en dépendent.
     */
    public void supprimerAsync(String idMatiere,
                               Runnable onSucces,
                               Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // Autorisation.exigerAction("academique.matiere.supprimer");

            if (idMatiere == null || idMatiere.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la matière est obligatoire.");
            }

            if (matiereDAO.aDesEnfantsActifs(idMatiere)) {
                throw new IllegalStateException(
                        "Impossible de supprimer cette matière : des sous-matières actives en dépendent.");
            }

            if (!matiereDAO.softDelete(idMatiere)) {
                throw new IllegalStateException("Échec de la suppression de la matière.");
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // VALIDATIONS MÉTIER — ASYNCHRONE
    // ==========================================

    /** Vérifie si un code matière existe déjà (async). */
    public void codeExisteAsync(String idEcole, String codeMatiere, String idMatiereAExclure,
                                Consumer<Boolean> onSucces,
                                Consumer<Throwable> onErreur) {
        executer(() -> matiereDAO.codeExiste(idEcole, codeMatiere, idMatiereAExclure),
                onSucces, onErreur);
    }

    /** Vérifie si une matière a des enfants actifs (async). */
    public void aDesEnfantsActifsAsync(String idMatiere,
                                       Consumer<Boolean> onSucces,
                                       Consumer<Throwable> onErreur) {
        executer(() -> matiereDAO.aDesEnfantsActifs(idMatiere), onSucces, onErreur);
    }
}