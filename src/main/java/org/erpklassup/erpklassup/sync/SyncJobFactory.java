package org.erpklassup.erpklassup.sync;

import org.quartz.Job;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.Scheduler;

/**
 * SyncJobFactory — Quartz instancie normalement ses Job via un constructeur
 * sans argument (par réflexion). SyncOrchestrator a besoin de dépendances
 * (ConnectionProvider, SyncApiClient, idClient) construites une seule fois
 * au démarrage de l'app, pas recréées à chaque tick — cette factory
 * renvoie donc toujours la même instance déjà construite plutôt que d'en
 * fabriquer une nouvelle à chaque déclenchement.
 */
public class SyncJobFactory implements JobFactory {

    private final SyncOrchestrator orchestrator;

    public SyncJobFactory(SyncOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        return orchestrator;
    }
}
