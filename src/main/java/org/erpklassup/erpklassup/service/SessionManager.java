package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.models.Ecole;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.util.ViewRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private static volatile SessionManager instance;

    private String idSessionCourante;
    private Utilisateur utilisateurCourant;
    private Ecole ecoleCourante;

    private Stage stagePrincipal;
    private ViewRegistry viewRegistry;

    private final Set<String> codesPermissions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> mappingActions = new ConcurrentHashMap<>();
    private final List<Runnable> ecouteursPermissions = new CopyOnWriteArrayList<>();
    private final List<Runnable> ecouteursChangementUtilisateur = new CopyOnWriteArrayList<>();


    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();

    // ✅ Plus de "final" ni de "new" ici : instanciation paresseuse pour casser la boucle
    private RafraichisseurPermissions rafraichisseur;

    private ScheduledExecutorService schedulerBackground;
    private static final int DUREE_INACTIVITE_DEFAUT_MINUTES = 30;

    /** Intervalle de vérification de l'inactivité (en secondes). */
    private static final int INTERVALLE_VERIF_INACTIVITE_SECONDES = 30;

    /** Durée d'inactivité configurable (peut être modifiée dynamiquement). */
    private volatile int dureeInactiviteMinutes = DUREE_INACTIVITE_DEFAUT_MINUTES;

    /** Timestamp de la dernière activité utilisateur (thread-safe). */
    private volatile long derniereActiviteMillis = System.currentTimeMillis();

    /** Callback exécuté lors d'une déconnexion pour inactivité (pour audit + UI). */
    private volatile Runnable onInactiviteDetectee;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void initialiserContexteGraphique(Stage stage, ViewRegistry registry) {
        this.stagePrincipal = stage;
        this.viewRegistry = registry;
    }

    public Stage getStagePrincipal() {
        return stagePrincipal;
    }

    public ViewRegistry getViewRegistry() {
        return viewRegistry;
    }

    public void demarrerSession(String idSession, Utilisateur user, List<Permission> permissions) {
        this.idSessionCourante = idSession;
        this.utilisateurCourant = user;
        this.derniereActiviteMillis = System.currentTimeMillis();   // ✅ Reset


        remplacerPermissions(permissions);
        demarrerPlanificateurArrierePlan();
    }

    public void demarrerSession(String idSession, Utilisateur user, Set<String> codesPermissions) {
        this.idSessionCourante = idSession;
        this.utilisateurCourant = user;
        this.derniereActiviteMillis = System.currentTimeMillis();   // ✅ Reset


        remplacerCodesPermissions(codesPermissions);
        demarrerPlanificateurArrierePlan();
    }

    private void demarrerPlanificateurArrierePlan() {
        arreterPlanificateurArrierePlan();

        schedulerBackground = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ERP-Session-Background-Thread");
            t.setDaemon(true);
            return t;
        });

        if (rafraichisseur == null) {
            rafraichisseur = new RafraichisseurPermissions();
        }

        // Heartbeat BDD toutes les 5 minutes (existant)
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte() && idSessionCourante != null) {
                sessionUtilisateurDAO.toucherSession(idSessionCourante);
            }
        }, 1, 5, TimeUnit.MINUTES);

        // Synchronisation des permissions toutes les 15s (existant)
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte()) {
                try {
                    rafraichisseur.rafraichir();
                } catch (Exception e) {
                    System.err.println("Erreur lors de la synchronisation des permissions : " + e.getMessage());
                }
            }
        }, 0, 15, TimeUnit.SECONDS);

        // ✅ NOUVEAU : Surveillance de l'inactivité
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (!estConnecte()) return;

            long minutesInactif = getMinutesInactivite();

            if (minutesInactif >= dureeInactiviteMinutes) {
                System.out.println("⏱️ Inactivité détectée : " + minutesInactif
                        + " min (seuil = " + dureeInactiviteMinutes + " min)");

                // Exécuter le callback s'il est défini (audit + redirection UI)
                Runnable callback = onInactiviteDetectee;
                if (callback != null) {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        System.err.println("Erreur callback inactivité : " + e.getMessage());
                    }
                }
            }
        }, INTERVALLE_VERIF_INACTIVITE_SECONDES, INTERVALLE_VERIF_INACTIVITE_SECONDES, TimeUnit.SECONDS);
    }

    private void arreterPlanificateurArrierePlan() {
        if (schedulerBackground != null && !schedulerBackground.isShutdown()) {
            schedulerBackground.shutdown();
            try {
                if (!schedulerBackground.awaitTermination(2, TimeUnit.SECONDS)) {
                    schedulerBackground.shutdownNow();
                }
            } catch (InterruptedException e) {
                schedulerBackground.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void remplacerPermissions(List<Permission> permissions) {
        codesPermissions.clear();
        if (permissions != null) {
            for (Permission p : permissions) {
                if (p != null && p.getCodePermission() != null) {
                    codesPermissions.add(p.getCodePermission());
                }
            }
        }
        notifierChangementPermissions();
    }

    public void remplacerCodesPermissions(Collection<String> nouveauxCodes) {
        codesPermissions.clear();
        if (nouveauxCodes != null) {
            codesPermissions.addAll(nouveauxCodes);
        }
        notifierChangementPermissions();
    }

    public void remplacerMappingActions(Map<String, String> mapping) {
        mappingActions.clear();
        if (mapping != null) {
            mappingActions.putAll(mapping);
        }
        notifierChangementPermissions();
    }

    public boolean estConnecte() {
        return utilisateurCourant != null;
    }

    public boolean aLaPermission(String codePermission) {
        return codePermission != null && codesPermissions.contains(codePermission);
    }

    public boolean peutExecuterAction(String codeAction) {
        String codePermission = mappingActions.get(codeAction);
        if (codePermission == null) {
            System.err.println("Aucune permission associée à l'action : " + codeAction + " (accès refusé)");
            return false;
        }
        return aLaPermission(codePermission);
    }

    public void terminerSession() {
        if (idSessionCourante != null) {
            sessionUtilisateurDAO.InvaliderSession(idSessionCourante, "LOGOUT_USER");
        }

        arreterPlanificateurArrierePlan();

        idSessionCourante = null;
        utilisateurCourant = null;
        ecoleCourante = null;
        codesPermissions.clear();
        mappingActions.clear();

        onInactiviteDetectee = null;   // ✅ Nettoyage

        notifierChangementPermissions();
    }

    public void setEcoleCourante(Ecole ecole) {
        this.ecoleCourante = ecole;
    }

    public Ecole getEcoleCourante() {
        return ecoleCourante;
    }

    public String getIdEcoleCourante() {
        if (ecoleCourante != null) {
            return ecoleCourante.getIdEcole();
        }
        return utilisateurCourant != null ? utilisateurCourant.getIdEcole() : null;
    }

    public String getIdSessionCourante() {
        return idSessionCourante;
    }

    public Utilisateur getUtilisateurCourant() {
        return utilisateurCourant;
    }

    public String getIdUtilisateurCourant() {
        return utilisateurCourant != null ? utilisateurCourant.getIdUtilisateur() : null;
    }

    public void ecouterChangementsPermissions(Runnable ecouteur) {
        ecouteursPermissions.add(ecouteur);
    }

    public void arreterEcoute(Runnable ecouteur) {
        ecouteursPermissions.remove(ecouteur);
    }

    private void notifierChangementPermissions() {
        for (Runnable ecouteur : ecouteursPermissions) {
            Platform.runLater(() -> {
                try {
                    ecouteur.run();
                } catch (Exception e) {
                    System.err.println("Erreur dans l'écouteur de permissions : " + e.getMessage());
                }
            });
        }
    }

    // ==========================================
    // ✅ NOUVELLES MÉTHODES : Gestion de l'inactivité
    // ==========================================

    /**
     * Enregistre une activité utilisateur (clic, souris, clavier, navigation...).
     * À appeler depuis le contrôleur UI à chaque interaction.
     */
    public void enregistrerActivite() {
        this.derniereActiviteMillis = System.currentTimeMillis();
    }

    /**
     * Configure la durée d'inactivité avant déconnexion automatique.
     * @param minutes Durée en minutes (minimum 1)
     */
    public void setDureeInactiviteMinutes(int minutes) {
        if (minutes < 1) {
            System.err.println("⚠️ Durée d'inactivité invalide : " + minutes + " min (minimum 1)");
            return;
        }
        this.dureeInactiviteMinutes = minutes;
        System.out.println("⏱️ Durée d'inactivité configurée : " + minutes + " minutes");
    }

    public int getDureeInactiviteMinutes() {
        return dureeInactiviteMinutes;
    }

    /**
     * Retourne le nombre de minutes d'inactivité écoulées.
     */
    public long getMinutesInactivite() {
        return (System.currentTimeMillis() - derniereActiviteMillis) / 60_000;
    }

    /**
     * Enregistre un callback exécuté lors d'une déconnexion pour inactivité.
     * Utile pour tracer dans l'audit + rediriger vers le login.
     */
    public void setOnInactiviteDetectee(Runnable callback) {
        this.onInactiviteDetectee = callback;
    }

    public void notifierChangementUtilisateur() {
        for (Runnable ecouteur : ecouteursChangementUtilisateur) {
            Platform.runLater(() -> {
                try {
                    ecouteur.run();
                } catch (Exception e) {
                    System.err.println("Erreur écouteur changement utilisateur : " + e.getMessage());
                }
            });
        }
    }
}
