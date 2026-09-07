package org.erpklassup.erpklassup.service;

import javafx.concurrent.Task;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class ServiceAsyncBase {
    private final AtomicLong compteurRequetes = new AtomicLong();

    protected <T> void executer(Callable<T> action, Consumer<T> onSucces, Consumer<Throwable> onErreur) {
        Task<T> tache = new Task<>() {
            @Override protected T call() throws Exception { return action.call(); }
        };
        tache.setOnSucceeded(e -> onSucces.accept(tache.getValue()));
        tache.setOnFailed(e -> onErreur.accept(tache.getException()));
        AppExecutor.get().submit(tache);
    }

    protected void executerSansRetour(ActionSansRetour action, Runnable onSucces, Consumer<Throwable> onErreur) {
        executer(() -> { action.executer(); return null; }, ignore -> onSucces.run(), onErreur);
    }

    protected long nouvelleRequete() { return compteurRequetes.incrementAndGet(); }
    protected boolean estRequeteActuelle(long jeton) { return jeton == compteurRequetes.get(); }

    @FunctionalInterface
    protected interface ActionSansRetour { void executer() throws Exception; }
}