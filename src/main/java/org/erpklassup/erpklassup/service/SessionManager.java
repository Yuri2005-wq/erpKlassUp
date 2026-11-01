package org.erpklassup.erpklassup.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.models.Ecole;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionManager {

    private static volatile SessionManager instance;

    private String idSessionCourante;
    private Utilisateur utilisateurCourant;
    private Ecole ecoleCourante;
    private String tokenJwt;

    /* ============================================================
       Contexte année scolaire active
       ============================================================ */
    private AnneeScolaire anneeActive;
    private final List<Runnable> ecouteursChangementAnnee = new CopyOnWriteArrayList<>();

    /* ============================================================
       Contexte graphique
       ============================================================ */
    private Stage stagePrincipal;
    private ViewRegistry viewRegistry;

    /* ============================================================
       Permissions
       ============================================================ */
    private final Set<String> codesPermissions = ConcurrentHashMap.newKeySet();
    private final Map<String, String> mappingActions = new ConcurrentHashMap<>();
    private final List<Runnable> ecouteursPermissions = new CopyOnWriteArrayList<>();
    private final List<Runnable> ecouteursChangementUtilisateur = new CopyOnWriteArrayList<>();

    /** Callback exécuté quand la session courante est invalidée (déconnexion distante). */
    private volatile Runnable callbackSessionInvalidee;

    public void setOnSessionInvalidee(Runnable callback) {
        this.callbackSessionInvalidee = callback;
    }

    /* ============================================================
       DAO
       ============================================================ */
    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();

    /* ============================================================
       Rafraîchisseurs (instanciation paresseuse)
       ============================================================ */
    private RafraichisseurPermissions rafraichisseur;
    private RafraichisseurAnneeScolaire rafraichisseurAnnee;

    /* ============================================================
       Scheduler & inactivité
       ============================================================ */
    private ScheduledExecutorService schedulerBackground;

    private static final int DUREE_INACTIVITE_DEFAUT_MINUTES = 30;
    private static final int INTERVALLE_VERIF_INACTIVITE_SECONDES = 30;
    private static final int INTERVALLE_VERIF_ANNEE_SECONDES = 20;

    private volatile int dureeInactiviteMinutes = DUREE_INACTIVITE_DEFAUT_MINUTES;
    private volatile long derniereActiviteMillis = System.currentTimeMillis();

    /** Callback exécuté lors d'une déconnexion pour inactivité. */
    private volatile Runnable onInactiviteDetectee;

    /**
     * ✅ Anti-réentrance : évite que le callback d'inactivité soit déclenché
     * plusieurs fois de suite tant que la déconnexion n'est pas finalisée.
     */
    private final AtomicBoolean inactiviteEnCours = new AtomicBoolean(false);

    /* ============================================================
       Singleton
       ============================================================ */
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

    /* ============================================================
       Contexte graphique
       ============================================================ */
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

    /* ============================================================
       Démarrage de session
       ============================================================ */
    public void demarrerSession(String idSession, Utilisateur user, List<Permission> permissions) {
        this.idSessionCourante = idSession;
        this.utilisateurCourant = user;
        this.derniereActiviteMillis = System.currentTimeMillis();

        // ✅ Réinitialiser l'état d'inactivité
        inactiviteEnCours.set(false);

        remplacerPermissions(permissions);
        demarrerPlanificateurArrierePlan();
    }

    public void demarrerSession(String idSession, Utilisateur user, Set<String> codesPermissions) {
        this.idSessionCourante = idSession;
        this.utilisateurCourant = user;
        this.derniereActiviteMillis = System.currentTimeMillis();

        // ✅ Réinitialiser l'état d'inactivité
        inactiviteEnCours.set(false);

        remplacerCodesPermissions(codesPermissions);
        chargerAnneeCouranteInitiale();
        demarrerPlanificateurArrierePlan();
    }

    private void chargerAnneeCouranteInitiale() {
        String idEcole = getIdEcoleCourante();
        if (idEcole != null) {
            new org.erpklassup.erpklassup.service.module2.AnneeScolaireService()
                    .chargerCouranteAsync(idEcole,
                            opt -> opt.ifPresent(this::setAnneeActive),
                            err -> System.err.println("❌ Impossible de charger l'année courante au démarrage : " + err.getMessage())
                    );
        }
    }

    /* ============================================================
       Scheduler d'arrière-plan
       ============================================================ */
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
        if (rafraichisseurAnnee == null) {
            rafraichisseurAnnee = new RafraichisseurAnneeScolaire();
        }

        /* ---------- Heartbeat BDD toutes les 5 minutes ---------- */
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte() && idSessionCourante != null) {
                sessionUtilisateurDAO.toucherSession(idSessionCourante);
            }
        }, 1, 5, TimeUnit.MINUTES);

        /* ---------- Synchronisation des permissions toutes les 15s ---------- */
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte()) {
                try {
                    rafraichisseur.rafraichir();
                } catch (Exception e) {
                    System.err.println("Erreur synchro permissions : " + e.getMessage());
                }
            }
        }, 0, 15, TimeUnit.SECONDS);

        /* ---------- Vérification de l'année scolaire active ---------- */
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte()) {
                try {
                    rafraichisseurAnnee.verifier();
                } catch (Exception e) {
                    System.err.println("Erreur vérification année : " + e.getMessage());
                }
            }
        }, 5, INTERVALLE_VERIF_ANNEE_SECONDES, TimeUnit.SECONDS);

        /* ---------- ✅ Surveillance de l'inactivité (CORRIGÉE) ---------- */
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (!estConnecte()) return;

            long minutesInactif = getMinutesInactivite();
            if (minutesInactif < dureeInactiviteMinutes) return;

            // ✅ Anti-réentrance : on ne déclenche qu'une seule fois
            if (!inactiviteEnCours.compareAndSet(false, true)) {
                return;
            }

            System.out.println("⏱️ Inactivité détectée : " + minutesInactif
                    + " min (seuil = " + dureeInactiviteMinutes + " min)");

            Runnable callback = onInactiviteDetectee;
            if (callback == null) {
                inactiviteEnCours.set(false);
                return;
            }

            // ✅ Bascule sur le FX thread AVANT d'exécuter le callback
            Platform.runLater(() -> {
                try {
                    callback.run();
                } catch (Exception e) {
                    System.err.println("Erreur callback inactivité : " + e.getMessage());
                    // En cas d'erreur, on permet un nouvel essai au prochain cycle
                    inactiviteEnCours.set(false);
                }
            });
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

    /* ============================================================
       Permissions
       ============================================================ */
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
            System.err.println("Aucune permission associée à l'action : " + codeAction
                    + " (accès refusé)");
            return false;
        }
        return aLaPermission(codePermission);
    }

    /* ============================================================
       Fin de session
       ============================================================ */
    public void terminerSession() {
        if (idSessionCourante != null) {
            sessionUtilisateurDAO.InvaliderSession(idSessionCourante, "LOGOUT_USER");
        }

        arreterPlanificateurArrierePlan();

        idSessionCourante = null;
        utilisateurCourant = null;
        ecoleCourante = null;
        tokenJwt = null;
        anneeActive = null;
        codesPermissions.clear();
        mappingActions.clear();

        onInactiviteDetectee = null;

        // ✅ Réinitialiser l'état d'inactivité
        inactiviteEnCours.set(false);

        notifierChangementPermissions();
    }

    /* ============================================================
       École / Utilisateur
       ============================================================ */
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

    /* ============================================================
       Contexte année scolaire active / mode archive
       ============================================================ */
    public void setAnneeActive(AnneeScolaire annee) {
        this.anneeActive = annee;
        notifierChangementAnnee();
    }

    public AnneeScolaire getAnneeActive() {
        return anneeActive;
    }

    public String getIdAnneeActive() {
        return anneeActive != null ? anneeActive.getId() : null;
    }

    public boolean isModeLectureSeule() {
        if (anneeActive == null) return true;
        AnneeScolaire.Statut statut = anneeActive.getStatut();
        return statut != AnneeScolaire.Statut.EN_COURS
                && statut != AnneeScolaire.Statut.PREPARATION;
    }

    public void ecouterChangementAnnee(Runnable ecouteur) {
        ecouteursChangementAnnee.add(ecouteur);
    }

    public void arreterEcouteAnnee(Runnable ecouteur) {
        ecouteursChangementAnnee.remove(ecouteur);
    }

    private void notifierChangementAnnee() {
        for (Runnable ecouteur : ecouteursChangementAnnee) {
            Platform.runLater(() -> {
                try {
                    ecouteur.run();
                } catch (Exception e) {
                    System.err.println("Erreur dans l'écouteur de changement d'année : "
                            + e.getMessage());
                }
            });
        }
    }

    /* ============================================================
       Écouteurs de permissions
       ============================================================ */
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
                    System.err.println("Erreur dans l'écouteur de permissions : "
                            + e.getMessage());
                }
            });
        }
    }

    /* ============================================================
       Gestion de l'inactivité
       ============================================================ */
    public void enregistrerActivite() {
        this.derniereActiviteMillis = System.currentTimeMillis();
    }

    public void setDureeInactiviteMinutes(int minutes) {
        if (minutes < 1) {
            System.err.println("⚠️ Durée d'inactivité invalide : "
                    + minutes + " min (minimum 1)");
            return;
        }
        this.dureeInactiviteMinutes = minutes;
        System.out.println("⏱️ Durée d'inactivité configurée : " + minutes + " minutes");
    }

    public int getDureeInactiviteMinutes() {
        return dureeInactiviteMinutes;
    }

    public long getMinutesInactivite() {
        return (System.currentTimeMillis() - derniereActiviteMillis) / 60_000;
    }

    public void setOnInactiviteDetectee(Runnable callback) {
        this.onInactiviteDetectee = callback;
    }

    /* ============================================================
       Notification changement utilisateur
       ============================================================ */
    public void notifierChangementUtilisateur() {
        for (Runnable ecouteur : ecouteursChangementUtilisateur) {
            Platform.runLater(() -> {
                try {
                    ecouteur.run();
                } catch (Exception e) {
                    System.err.println("Erreur écouteur changement utilisateur : "
                            + e.getMessage());
                }
            });
        }
    }

    /* ============================================================
       Token JWT
       ============================================================ */
    public String getTokenJwt() {
        return tokenJwt;
    }

    public void setTokenJwt(String tokenJwt) {
        this.tokenJwt = tokenJwt;
    }
}
//package org.erpklassup.erpklassup.service;
//
//import javafx.application.Platform;
//import javafx.stage.Stage;
//import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
//import org.erpklassup.erpklassup.models.Ecole;
//import org.erpklassup.erpklassup.models.Permission;
//import org.erpklassup.erpklassup.models.Utilisateur;
//import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
//import org.erpklassup.erpklassup.util.ViewRegistry;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class SessionManager {
//
//    private static volatile SessionManager instance;
//
//    private String idSessionCourante;
//    private Utilisateur utilisateurCourant;
//    private Ecole ecoleCourante;
//    private String tokenJwt;
//
//    // ✅ NOUVEAU : contexte d'année scolaire active (indépendant de l'école active)
//    private AnneeScolaire anneeActive;
//    private final List<Runnable> ecouteursChangementAnnee = new CopyOnWriteArrayList<>();
//
//
//    private Stage stagePrincipal;
//    private ViewRegistry viewRegistry;
//
//    private final Set<String> codesPermissions = ConcurrentHashMap.newKeySet();
//    private final Map<String, String> mappingActions = new ConcurrentHashMap<>();
//    private final List<Runnable> ecouteursPermissions = new CopyOnWriteArrayList<>();
//    private final List<Runnable> ecouteursChangementUtilisateur = new CopyOnWriteArrayList<>();
//
//
//    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();
//
//    // ✅ Plus de "final" ni de "new" ici : instanciation paresseuse pour casser la boucle
//    private RafraichisseurPermissions rafraichisseur;
//
//    // ✅ NOUVEAU : instanciation paresseuse, même raison que rafraichisseur ci-dessus
//    private RafraichisseurAnneeScolaire rafraichisseurAnnee;
//
//    private ScheduledExecutorService schedulerBackground;
//    private static final int DUREE_INACTIVITE_DEFAUT_MINUTES = 3;
//
//    /** Intervalle de vérification de l'inactivité (en secondes). */
//    private static final int INTERVALLE_VERIF_INACTIVITE_SECONDES = 30;
//
//    /** Intervalle de vérification du statut de l'année scolaire active (en secondes). */
//    private static final int INTERVALLE_VERIF_ANNEE_SECONDES = 20;
//
//    /** Durée d'inactivité configurable (peut être modifiée dynamiquement). */
//    private volatile int dureeInactiviteMinutes = DUREE_INACTIVITE_DEFAUT_MINUTES;
//
//    /** Timestamp de la dernière activité utilisateur (thread-safe). */
//    private volatile long derniereActiviteMillis = System.currentTimeMillis();
//
//    /** Callback exécuté lors d'une déconnexion pour inactivité (pour audit + UI). */
//    private volatile Runnable onInactiviteDetectee;
//
//    private SessionManager() {}
//
//    public static SessionManager getInstance() {
//        if (instance == null) {
//            synchronized (SessionManager.class) {
//                if (instance == null) {
//                    instance = new SessionManager();
//                }
//            }
//        }
//        return instance;
//    }
//
//    public void initialiserContexteGraphique(Stage stage, ViewRegistry registry) {
//        this.stagePrincipal = stage;
//        this.viewRegistry = registry;
//    }
//
//    public Stage getStagePrincipal() {
//        return stagePrincipal;
//    }
//
//    public ViewRegistry getViewRegistry() {
//        return viewRegistry;
//    }
//
//    public void demarrerSession(String idSession, Utilisateur user, List<Permission> permissions) {
//        this.idSessionCourante = idSession;
//        this.utilisateurCourant = user;
//        this.derniereActiviteMillis = System.currentTimeMillis();   // ✅ Reset
//
//
//        remplacerPermissions(permissions);
//        demarrerPlanificateurArrierePlan();
//    }
//
//    public void demarrerSession(String idSession, Utilisateur user, Set<String> codesPermissions) {
//        this.idSessionCourante = idSession;
//        this.utilisateurCourant = user;
//        this.derniereActiviteMillis = System.currentTimeMillis();   // ✅ Reset
//
//
//        remplacerCodesPermissions(codesPermissions);
//        demarrerPlanificateurArrierePlan();
//    }
//
//    private void demarrerPlanificateurArrierePlan() {
//        arreterPlanificateurArrierePlan();
//
//        schedulerBackground = Executors.newSingleThreadScheduledExecutor(r -> {
//            Thread t = new Thread(r, "ERP-Session-Background-Thread");
//            t.setDaemon(true);
//            return t;
//        });
//
//        if (rafraichisseur == null) {
//            rafraichisseur = new RafraichisseurPermissions();
//        }
//        if (rafraichisseurAnnee == null) {
//            rafraichisseurAnnee = new RafraichisseurAnneeScolaire();
//        }
//
//        // Heartbeat BDD toutes les 5 minutes (existant)
//        schedulerBackground.scheduleAtFixedRate(() -> {
//            if (estConnecte() && idSessionCourante != null) {
//                sessionUtilisateurDAO.toucherSession(idSessionCourante);
//            }
//        }, 1, 5, TimeUnit.MINUTES);
//
//        // Synchronisation des permissions toutes les 15s (existant)
//        schedulerBackground.scheduleAtFixedRate(() -> {
//            if (estConnecte()) {
//                try {
//                    rafraichisseur.rafraichir();
//                } catch (Exception e) {
//                    System.err.println("Erreur lors de la synchronisation des permissions : " + e.getMessage());
//                }
//            }
//        }, 0, 15, TimeUnit.SECONDS);
//
//        // ✅ NOUVEAU : Vérification périodique du statut/courante de l'année scolaire active
//        schedulerBackground.scheduleAtFixedRate(() -> {
//            if (estConnecte()) {
//                try {
//                    rafraichisseurAnnee.verifier();
//                } catch (Exception e) {
//                    System.err.println("Erreur lors de la vérification de l'année scolaire : " + e.getMessage());
//                }
//            }
//        }, 5, INTERVALLE_VERIF_ANNEE_SECONDES, TimeUnit.SECONDS);
//
//        // ✅ NOUVEAU : Surveillance de l'inactivité
//        // ✅ Surveillance de l'inactivité
//        schedulerBackground.scheduleAtFixedRate(() -> {
//            if (!estConnecte()) return;
//
//            long minutesInactif = getMinutesInactivite();
//
//            if (minutesInactif >= dureeInactiviteMinutes) {
//                System.out.println("⏱️ Inactivité détectée : " + minutesInactif
//                        + " min (seuil = " + dureeInactiviteMinutes + " min)");
//
//                Runnable callback = onInactiviteDetectee;
//                if (callback != null) {
//                    // ✅ On bascule sur le FX thread AVANT d'exécuter le callback
//                    Platform.runLater(() -> {
//                        try {
//                            callback.run();
//                        } catch (Exception e) {
//                            System.err.println("Erreur callback inactivité : " + e.getMessage());
//                        }
//                    });
//                }
//            }
//        }, INTERVALLE_VERIF_INACTIVITE_SECONDES, INTERVALLE_VERIF_INACTIVITE_SECONDES, TimeUnit.SECONDS);
////        schedulerBackground.scheduleAtFixedRate(() -> {
////            if (!estConnecte()) return;
////
////            long minutesInactif = getMinutesInactivite();
////
////            if (minutesInactif >= dureeInactiviteMinutes) {
////                System.out.println("⏱️ Inactivité détectée : " + minutesInactif
////                        + " min (seuil = " + dureeInactiviteMinutes + " min)");
////
////                // Exécuter le callback s'il est défini (audit + redirection UI)
////                Runnable callback = onInactiviteDetectee;
////                if (callback != null) {
////                    try {
////                        callback.run();
////                    } catch (Exception e) {
////                        System.err.println("Erreur callback inactivité : " + e.getMessage());
////                    }
////                }
////            }
////        }, INTERVALLE_VERIF_INACTIVITE_SECONDES, INTERVALLE_VERIF_INACTIVITE_SECONDES, TimeUnit.SECONDS);
//    }
//
//    private void arreterPlanificateurArrierePlan() {
//        if (schedulerBackground != null && !schedulerBackground.isShutdown()) {
//            schedulerBackground.shutdown();
//            try {
//                if (!schedulerBackground.awaitTermination(2, TimeUnit.SECONDS)) {
//                    schedulerBackground.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                schedulerBackground.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//
//    public void remplacerPermissions(List<Permission> permissions) {
//        codesPermissions.clear();
//        if (permissions != null) {
//            for (Permission p : permissions) {
//                if (p != null && p.getCodePermission() != null) {
//                    codesPermissions.add(p.getCodePermission());
//                }
//            }
//        }
//        notifierChangementPermissions();
//    }
//
//    public void remplacerCodesPermissions(Collection<String> nouveauxCodes) {
//        codesPermissions.clear();
//        if (nouveauxCodes != null) {
//            codesPermissions.addAll(nouveauxCodes);
//        }
//        notifierChangementPermissions();
//    }
//
//    public void remplacerMappingActions(Map<String, String> mapping) {
//        mappingActions.clear();
//        if (mapping != null) {
//            mappingActions.putAll(mapping);
//        }
//        notifierChangementPermissions();
//    }
//
//    public boolean estConnecte() {
//        return utilisateurCourant != null;
//    }
//
//    public boolean aLaPermission(String codePermission) {
//        return codePermission != null && codesPermissions.contains(codePermission);
//    }
//
//    public boolean peutExecuterAction(String codeAction) {
//        String codePermission = mappingActions.get(codeAction);
//        if (codePermission == null) {
//            System.err.println("Aucune permission associée à l'action : " + codeAction + " (accès refusé)");
//            return false;
//        }
//        return aLaPermission(codePermission);
//    }
//
//    public void terminerSession() {
//        if (idSessionCourante != null) {
//            sessionUtilisateurDAO.InvaliderSession(idSessionCourante, "LOGOUT_USER");
//        }
//
//        arreterPlanificateurArrierePlan();
//
//        idSessionCourante = null;
//        utilisateurCourant = null;
//        ecoleCourante = null;
//        tokenJwt = null;                // ✅ AJOUT
//        anneeActive = null;             // ✅ AJOUT
//        codesPermissions.clear();
//        mappingActions.clear();
//
//        onInactiviteDetectee = null;   // ✅ Nettoyage
//
//        notifierChangementPermissions();
//    }
//
//    public void setEcoleCourante(Ecole ecole) {
//        this.ecoleCourante = ecole;
//    }
//
//    public Ecole getEcoleCourante() {
//        return ecoleCourante;
//    }
//
//    public String getIdEcoleCourante() {
//        if (ecoleCourante != null) {
//            return ecoleCourante.getIdEcole();
//        }
//        return utilisateurCourant != null ? utilisateurCourant.getIdEcole() : null;
//    }
//
//    public String getIdSessionCourante() {
//        return idSessionCourante;
//    }
//
//    public Utilisateur getUtilisateurCourant() {
//        return utilisateurCourant;
//    }
//
//    public String getIdUtilisateurCourant() {
//        return utilisateurCourant != null ? utilisateurCourant.getIdUtilisateur() : null;
//    }
//
//    // ==========================================
//    // ✅ NOUVELLES MÉTHODES : Contexte année scolaire active / mode archive
//    // ==========================================
//
//    /**
//     * Définit l'année scolaire actuellement consultée dans l'interface.
//     * Indépendant de l'école active : une même école a plusieurs années.
//     * Appelée depuis le sélecteur d'année du sidebar, ET automatiquement
//     * par RafraichisseurAnneeScolaire quand un changement est détecté.
//     */
//    public void setAnneeActive(AnneeScolaire annee) {
//        this.anneeActive = annee;
//        notifierChangementAnnee();
//    }
//
//    public AnneeScolaire getAnneeActive() {
//        return anneeActive;
//    }
//
//    public String getIdAnneeActive() {
//        return anneeActive != null ? anneeActive.getId() : null;
//    }
//
//    /**
//     * True si l'année actuellement affichée est CLOTUREE ou ARCHIVEE (ou si
//     * aucune année n'est sélectionnée) : l'UI doit désactiver les boutons
//     * d'édition/suppression/création. Ne remplace jamais AnneeAccessGuard
//     * côté service — c'est de la défense en profondeur, pas la vraie garde.
//     */
//    public boolean isModeLectureSeule() {
//        if (anneeActive == null) return true;
//        AnneeScolaire.Statut statut = anneeActive.getStatut();
//        return statut != AnneeScolaire.Statut.EN_COURS && statut != AnneeScolaire.Statut.PREPARATION;
//    }
//
//    public void ecouterChangementAnnee(Runnable ecouteur) {
//        ecouteursChangementAnnee.add(ecouteur);
//    }
//
//    public void arreterEcouteAnnee(Runnable ecouteur) {
//        ecouteursChangementAnnee.remove(ecouteur);
//    }
//
//    private void notifierChangementAnnee() {
//        for (Runnable ecouteur : ecouteursChangementAnnee) {
//            Platform.runLater(() -> {
//                try {
//                    ecouteur.run();
//                } catch (Exception e) {
//                    System.err.println("Erreur dans l'écouteur de changement d'année : " + e.getMessage());
//                }
//            });
//        }
//    }
//
//    public void ecouterChangementsPermissions(Runnable ecouteur) {
//        ecouteursPermissions.add(ecouteur);
//    }
//
//    public void arreterEcoute(Runnable ecouteur) {
//        ecouteursPermissions.remove(ecouteur);
//    }
//
//    private void notifierChangementPermissions() {
//        for (Runnable ecouteur : ecouteursPermissions) {
//            Platform.runLater(() -> {
//                try {
//                    ecouteur.run();
//                } catch (Exception e) {
//                    System.err.println("Erreur dans l'écouteur de permissions : " + e.getMessage());
//                }
//            });
//        }
//    }
//
//    // ==========================================
//    // ✅ Gestion de l'inactivité (existant, inchangé)
//    // ==========================================
//
//    /**
//     * Enregistre une activité utilisateur (clic, souris, clavier, navigation...).
//     * À appeler depuis le contrôleur UI à chaque interaction.
//     */
//    public void enregistrerActivite() {
//        this.derniereActiviteMillis = System.currentTimeMillis();
//    }
//
//    /**
//     * Configure la durée d'inactivité avant déconnexion automatique.
//     * @param minutes Durée en minutes (minimum 1)
//     */
//    public void setDureeInactiviteMinutes(int minutes) {
//        if (minutes < 1) {
//            System.err.println("⚠️ Durée d'inactivité invalide : " + minutes + " min (minimum 1)");
//            return;
//        }
//        this.dureeInactiviteMinutes = minutes;
//        System.out.println("⏱️ Durée d'inactivité configurée : " + minutes + " minutes");
//    }
//
//    public int getDureeInactiviteMinutes() {
//        return dureeInactiviteMinutes;
//    }
//
//    /**
//     * Retourne le nombre de minutes d'inactivité écoulées.
//     */
//    public long getMinutesInactivite() {
//        return (System.currentTimeMillis() - derniereActiviteMillis) / 60_000;
//    }
//
//    /**
//     * Enregistre un callback exécuté lors d'une déconnexion pour inactivité.
//     * Utile pour tracer dans l'audit + rediriger vers le login.
//     */
//    public void setOnInactiviteDetectee(Runnable callback) {
//        this.onInactiviteDetectee = callback;
//    }
//
//    public void notifierChangementUtilisateur() {
//        for (Runnable ecouteur : ecouteursChangementUtilisateur) {
//            Platform.runLater(() -> {
//                try {
//                    ecouteur.run();
//                } catch (Exception e) {
//                    System.err.println("Erreur écouteur changement utilisateur : " + e.getMessage());
//                }
//            });
//        }
//    }
//
//    public String getTokenJwt() {
//        return tokenJwt;
//    }
//
//    public void setTokenJwt(String tokenJwt) {
//        this.tokenJwt = tokenJwt;
//    }
//}