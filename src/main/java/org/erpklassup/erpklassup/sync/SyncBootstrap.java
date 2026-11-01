package org.erpklassup.erpklassup.sync;

import org.erpklassup.erpklassup.Database;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * SyncBootstrap — point d'entrée unique pour démarrer/arrêter le sous-système
 * de synchronisation. À appeler une fois au démarrage de l'application
 * (ex: dans Application.start()) et à l'arrêt (ex: Application.stop()).
 *
 * Usage minimal :
 *   SyncBootstrap.start();
 *   ...
 *   SyncBootstrap.shutdown();   // à l'arrêt propre de l'app
 *
 * Déclenchement manuel (bouton "Synchroniser maintenant" dans l'UI) :
 *   SyncBootstrap.triggerNow();
 */
public class SyncBootstrap {

    // ==========================================
    // CONFIGURATION — À REMPLACER PAR UN VRAI FLUX D'ENRÔLEMENT PLUS TARD
    // ==========================================
    // TODO : quand l'endpoint d'enrôlement existera côté Django, ces trois
    // valeurs (surtout ECOLE_KEY et ID_CLIENT) devront être obtenues
    // automatiquement au premier lancement du poste et stockées localement
    // (ex: via java.util.prefs.Preferences, comme Database.getClientId()),
    // plutôt que codées en dur ici. Pour l'instant, on réutilise le
    // SyncClient de test créé manuellement côté Django.
    private static final String BASE_URL = "http://127.0.0.1:8000"; // TODO: URL de prod une fois déployé
    private static final String ECOLE_KEY = "gFkY4bSDvGVlQkYc85NW7-xx2RIfb8zu-T-uI2UmXLw"; // TODO: à sortir du code en dur
    private static final String ID_CLIENT = "CLI-eed0e2cd"; // TODO: remplacer par Database.getClientId() une fois l'enrôlement en place

    private static final int INTERVALLE_SECONDES = 30;
    private static final JobKey JOB_KEY = new JobKey("syncJob", "erpklassup-sync");

    private static Scheduler scheduler;

    private SyncBootstrap() {
        // classe utilitaire statique, pas d'instanciation
    }

    /**
     * Démarre le scheduler Quartz et programme le cycle de synchronisation
     * pour s'exécuter toutes les INTERVALLE_SECONDES secondes. Idempotent :
     * un appel alors que le scheduler tourne déjà ne fait rien.
     */
    public static synchronized void start() {
        if (scheduler != null) {
            return; // déjà démarré
        }

        try {
            SyncApiClient api = new SyncApiClient(BASE_URL, ECOLE_KEY, ID_CLIENT);
            SyncOrchestrator.ConnectionProvider connections = new DatabaseConnectionProvider();
            SyncOrchestrator orchestrator = new SyncOrchestrator(connections, api, ID_CLIENT);

            scheduler = StdSchedulerFactory.getDefaultScheduler();
            // JobFactory personnalisée : renvoie toujours la même instance
            // d'orchestrator déjà construite avec ses dépendances, au lieu
            // que Quartz tente un new SyncOrchestrator() sans argument.
            scheduler.setJobFactory(new SyncJobFactory(orchestrator));

            JobDetail job = JobBuilder.newJob(SyncOrchestrator.class)
                    .withIdentity(JOB_KEY)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("syncTrigger", "erpklassup-sync")
                    .startNow()
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(INTERVALLE_SECONDES)
                                    .repeatForever()
                    )
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.start();

            System.out.println("🔄 Synchronisation démarrée (cycle toutes les "
                    + INTERVALLE_SECONDES + "s)");
        } catch (SchedulerException e) {
            // Une synchronisation qui ne démarre pas ne doit jamais empêcher
            // l'application de fonctionner en local — on logue et on continue.
            System.err.println("⚠️ Impossible de démarrer la synchronisation : " + e.getMessage());
            scheduler = null;
        }
    }

    /**
     * Déclenche un cycle de synchronisation immédiatement, sans attendre le
     * prochain tick planifié — pour un bouton "Synchroniser maintenant" côté
     * UI. Sans effet si le scheduler n'est pas démarré.
     */
    public static synchronized void triggerNow() {
        if (scheduler == null) {
            System.err.println("⚠️ Synchronisation non démarrée, impossible de déclencher un cycle manuel.");
            return;
        }
        try {
            scheduler.triggerJob(JOB_KEY);
        } catch (SchedulerException e) {
            System.err.println("⚠️ Échec du déclenchement manuel de synchronisation : " + e.getMessage());
        }
    }

    /**
     * Arrête proprement le scheduler. À appeler à la fermeture de
     * l'application (ex: Application.stop()), avant Database.fermerPool().
     * Le `true` attend la fin du cycle en cours plutôt que de le couper net,
     * pour éviter de laisser une transaction locale à moitié appliquée.
     */
    public static synchronized void shutdown() {
        if (scheduler == null) {
            return;
        }
        try {
            scheduler.shutdown(true);
            System.out.println("🔄 Synchronisation arrêtée proprement");
        } catch (SchedulerException e) {
            System.err.println("⚠️ Erreur à l'arrêt du scheduler de synchronisation : " + e.getMessage());
        } finally {
            scheduler = null;
        }
    }
}
