package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.EcoleDAO;
import org.erpklassup.erpklassup.models.Ecole;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service pour la gestion de l'école unique de l'application.
 *
 * <p>Règle métier : il ne peut y avoir qu'UNE seule école active en base.
 * Le service refuse donc toute création si une école existe déjà.</p>
 */
public class EcoleService extends ServiceAsyncBase {

    private final EcoleDAO ecoleDAO;

    public EcoleService() {
        this(new EcoleDAO());
    }

    public EcoleService(EcoleDAO ecoleDAO) {
        this.ecoleDAO = ecoleDAO;
    }

    /* ============================================================
       LECTURE
       ============================================================ */

    /**
     * Version synchrone : charge l'école unique.
     */
    public Optional<Ecole> chargerEcole() {
        return ecoleDAO.findPremiereEcoleActive();
    }

    /**
     * Version asynchrone : charge l'école unique.
     */
    public void chargerEcoleAsync(Consumer<Optional<Ecole>> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(ecoleDAO::findPremiereEcoleActive, onSucces, onErreur);
    }

    /* ============================================================
       SAUVEGARDE (création OU mise à jour)
       ============================================================ */

    /**
     * Sauvegarde l'école :
     * <ul>
     *   <li>Si {@code ecole.getId()} est null → création (échoue si une existe déjà)</li>
     *   <li>Sinon → mise à jour</li>
     * </ul>
     */
    public void sauvegarderAsync(Ecole ecole,
                                 Runnable onSucces,
                                 Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {

            // 1. Validation
            valider(ecole);

            boolean estCreation = (ecole.getIdEcole() == null || ecole.getIdEcole().isBlank());

            if (estCreation) {
                // 2. Vérifier qu'aucune école n'existe déjà
                Optional<Ecole> existante = ecoleDAO.findPremiereEcoleActive();
                if (existante.isPresent()) {
                    throw new IllegalStateException(
                            "Une école existe déjà en base. Modifiez-la au lieu d'en créer une nouvelle.");
                }

                // 3. Générer un ID
                ecole.setIdEcole(java.util.UUID.randomUUID().toString());

                // 4. Créer
                if (!ecoleDAO.create(ecole)) {
                    throw new IllegalStateException("Impossible de créer l'école en base.");
                }
            } else {
                // 5. Mettre à jour
                if (!ecoleDAO.update(ecole)) {
                    throw new IllegalStateException(
                            "Impossible de mettre à jour l'école (peut-être supprimée entre-temps).");
                }
            }

        }, onSucces, onErreur);
    }

    /* ============================================================
       VALIDATION
       ============================================================ */
    private void valider(Ecole ecole) {
        if (ecole == null) {
            throw new IllegalArgumentException("L'école est obligatoire.");
        }
        if (estVide(ecole.getNomEcole())) {
            throw new IllegalArgumentException("Le nom de l'école est obligatoire.");
        }
        if (estVide(ecole.getNumeroAgrement())) {
            throw new IllegalArgumentException("Le numéro d'agrément est obligatoire.");
        }
        if (estVide(ecole.getAdresse())) {
            throw new IllegalArgumentException("L'adresse est obligatoire.");
        }
        if (estVide(ecole.getTelephone())) {
            throw new IllegalArgumentException("Le téléphone est obligatoire.");
        }
        if (estVide(ecole.getRegion())) {
            throw new IllegalArgumentException("La région est obligatoire.");
        }
        if (estVide(ecole.getSousSysteme())) {
            throw new IllegalArgumentException("Le sous-système est obligatoire.");
        }
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}