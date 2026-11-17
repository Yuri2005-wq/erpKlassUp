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
    private boolean motDePasseTemporaire = false;

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
        this.motDePasseTemporaire = user.isDoitChangerMotDePasse();

        // ✅ Réinitialiser l'état d'inactivité
        inactiviteEnCours.set(false);

        remplacerPermissions(permissions);
        demarrerPlanificateurArrierePlan();
    }

    public void demarrerSession(String idSession, Utilisateur user, Set<String> codesPermissions) {
        this.idSessionCourante = idSession;
        this.utilisateurCourant = user;
        this.derniereActiviteMillis = System.currentTimeMillis();
        this.motDePasseTemporaire = user.isDoitChangerMotDePasse();
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
        motDePasseTemporaire = false;

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

    public boolean isMotDePasseTemporaire() {
        return motDePasseTemporaire;
    }

    public void setMotDePasseTemporaire(boolean valeur) {
        this.motDePasseTemporaire = valeur;
    }
}