package org.erpklassup.erpklassup.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pool de threads unique pour toute l'application — pas un par service. */
public final class AppExecutor {

    private static final ExecutorService INSTANCE = Executors.newFixedThreadPool(4, runnable -> {
        Thread t = new Thread(runnable, "erpklassup-worker");
        t.setDaemon(true);
        return t;
    });

    private AppExecutor() {}

    public static ExecutorService get() { return INSTANCE; }

    /** À appeler une seule fois, à la fermeture réelle de l'application (Application.stop()). */
    public static void arreter() { INSTANCE.shutdownNow(); }
}
