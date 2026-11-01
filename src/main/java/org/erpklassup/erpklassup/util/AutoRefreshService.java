package org.erpklassup.erpklassup.util;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Service générique de rafraîchissement automatique basé sur
 * {@link ScheduledService}.
 *
 * <p>Reproduit exactement le comportement utilisé dans
 * {@code JournalAuditTabController} :</p>
 * <ul>
 *   <li>Un loader ({@link Supplier}) exécuté périodiquement en arrière-plan</li>
 *   <li>Un callback {@code onSuccess} appelé sur le thread JavaFX</li>
 *   <li>Un callback {@code onFailure} pour les erreurs</li>
 *   <li>Un callback {@code onBeforeLoad} optionnel (ex : afficher "Chargement…")</li>
 *   <li>Un cache du dernier résultat ({@link #getDernierResultat()})</li>
 *   <li>Une méthode {@link #restartNow()} pour relancer immédiatement</li>
 * </ul>
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>{@code
 * AutoRefreshService<List<AuditLigne>> service =
 *     new AutoRefreshService<>(Duration.seconds(3))
 *         .loader(() -> auditService.rechercher(filtre))
 *         .onBeforeLoad(() -> table.setPlaceholder(new Label("Chargement…")))
 *         .onSuccess(this::mettreAJourTable)
 *         .onFailure(Throwable::printStackTrace)
 *         .start();
 *
 * // Plus tard :
 * service.restartNow();
 * service.dispose();
 * }</pre>
 *
 * @param <T> Type du résultat retourné par le loader
 */
public class AutoRefreshService<T> {

    /* ============================================================
       ÉTAT
       ============================================================ */
    private final ScheduledService<T> scheduledService;
    private final Duration periode;

    private Supplier<T>    loader;
    private Consumer<T>    onSuccess;
    private Consumer<Throwable> onFailure;
    private Runnable       onBeforeLoad;

    private T dernierResultat;
    private boolean demarre = false;

    /* ============================================================
       CONSTRUCTEUR
       ============================================================ */

    /**
     * @param periode Intervalle entre deux rafraîchissements (ex : {@code Duration.seconds(3)})
     */
    public AutoRefreshService(Duration periode) {
        if (periode == null) {
            throw new IllegalArgumentException("La période ne peut pas être nulle.");
        }
        this.periode = periode;

        this.scheduledService = new ScheduledService<>() {
            @Override
            protected Task<T> createTask() {
                return new Task<>() {
                    @Override
                    protected T call() throws Exception {
                        if (loader == null) return null;
                        return loader.get();
                    }
                };
            }
        };

        this.scheduledService.setPeriod(periode);

        /* ---------- onSucceeded ---------- */
        this.scheduledService.setOnSucceeded(event -> {
            T resultat = scheduledService.getValue();
            if (resultat != null) {
                dernierResultat = resultat;
                if (onSuccess != null) onSuccess.accept(resultat);
            }
        });

        /* ---------- onFailed ---------- */
        this.scheduledService.setOnFailed(event -> {
            Throwable err = scheduledService.getException();
            if (onFailure != null) {
                onFailure.accept(err);
            } else if (err != null) {
                err.printStackTrace();
            }
        });
    }

    /* ============================================================
       CONFIGURATION FLUENT
       ============================================================ */

    /** Définit le loader exécuté périodiquement (thread background). */
    public AutoRefreshService<T> loader(Supplier<T> loader) {
        this.loader = loader;
        return this;
    }

    /** Callback exécuté sur le thread JavaFX à chaque succès. */
    public AutoRefreshService<T> onSuccess(Consumer<T> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    /** Callback exécuté sur le thread JavaFX à chaque échec. */
    public AutoRefreshService<T> onFailure(Consumer<Throwable> onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    /** Callback exécuté avant chaque cycle de chargement (thread JavaFX). */
    public AutoRefreshService<T> onBeforeLoad(Runnable onBeforeLoad) {
        this.onBeforeLoad = onBeforeLoad;

        // On branche le onBeforeLoad sur le scheduledService :
        // ScheduledService n'a pas de hook direct → on enveloppe le loader.
        Supplier<T> ancien = this.loader;
        this.loader = () -> {
            if (onBeforeLoad != null) {
                javafx.application.Platform.runLater(onBeforeLoad);
            }
            return ancien != null ? ancien.get() : null;
        };
        return this;
    }

    /* ============================================================
       DÉMARRAGE / ARRÊT
       ============================================================ */

    /** Démarre le rafraîchissement périodique. */
    public AutoRefreshService<T> start() {
        if (!demarre) {
            scheduledService.start();
            demarre = true;
        }
        return this;
    }

    /** Relance immédiatement un cycle (sans attendre la période). */
    public void restartNow() {
        if (scheduledService.isRunning()) {
            scheduledService.restart();
        } else {
            start();
        }
    }

    /**
     * Arrête le service. À appeler impérativement dans
     * {@code VueDisposable.disposer()} pour éviter les fuites mémoire.
     */
    public void dispose() {
        if (scheduledService.isRunning()) {
            scheduledService.cancel();
        }
        demarre = false;
    }

    /* ============================================================
       ACCESSEURS
       ============================================================ */

    /** Retourne le dernier résultat reçu (peut être null). */
    public T getDernierResultat() {
        return dernierResultat;
    }

    /** true si le service tourne actuellement. */
    public boolean isRunning() {
        return scheduledService.isRunning();
    }

    /** Période configurée. */
    public Duration getPeriode() {
        return periode;
    }

    /** Accès direct au ScheduledService sous-jacent (cas avancés). */
    public ScheduledService<T> getScheduledService() {
        return scheduledService;
    }
}