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

    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();

    // ✅ Plus de "final" ni de "new" ici : instanciation paresseuse pour casser la boucle
    private RafraichisseurPermissions rafraichisseur;

    private ScheduledExecutorService schedulerBackground;

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

        remplacerPermissions(permissions);
        demarrerPlanificateurArrierePlan();
    }

    public void demarrerSession(String idSession, Utilisateur user, Set<String> codesPermissions) {
        this.idSessionCourante = idSession;
        this.utilisateurCourant = user;

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

        // ✅ Initialisation paresseuse du rafraichisseur AVANT de l'utiliser
        if (rafraichisseur == null) {
            rafraichisseur = new RafraichisseurPermissions();
        }

        // Heartbeat BDD toutes les 5 minutes
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte() && idSessionCourante != null) {
                sessionUtilisateurDAO.toucherSession(idSessionCourante);
            }
        }, 1, 5, TimeUnit.MINUTES);

        // Synchronisation des permissions et vérification du statut du compte toutes les 15s
        schedulerBackground.scheduleAtFixedRate(() -> {
            if (estConnecte()) {
                try {
                    rafraichisseur.rafraichir();
                } catch (Exception e) {
                    System.err.println("Erreur lors de la synchronisation des permissions : " + e.getMessage());
                }
            }
        }, 0, 15, TimeUnit.SECONDS);
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
}

//package org.erpklassup.erpklassup.service;
//
//import javafx.application.Platform;
//import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
//import org.erpklassup.erpklassup.models.Ecole;
//import org.erpklassup.erpklassup.models.Permission;
//import org.erpklassup.erpklassup.models.Utilisateur;
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
//
//    private final Set<String> codesPermissions = ConcurrentHashMap.newKeySet();
//    private final Map<String, String> mappingActions = new ConcurrentHashMap<>();
//    private final List<Runnable> ecouteursPermissions = new CopyOnWriteArrayList<>();
//
//    private final SessionUtilisateurDAO sessionUtilisateurDAO = new SessionUtilisateurDAO();
//    private final RafraichisseurPermissions rafraichisseur = new RafraichisseurPermissions();
//
//    private ScheduledExecutorService schedulerBackground;
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
//    public void demarrerSession(String idSession, Utilisateur user, List<Permission> permissions) {
//        this.idSessionCourante = idSession;
//        this.utilisateurCourant = user;
//
//        remplacerPermissions(permissions);
//        demarrerPlanificateurArrierePlan();
//    }
//
//    public void demarrerSession(String idSession, Utilisateur user, Set<String> codesPermissions) {
//        this.idSessionCourante = idSession;
//        this.utilisateurCourant = user;
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
//        // Heartbeat en base de données toutes les 5 minutes
//        schedulerBackground.scheduleAtFixedRate(() -> {
//            if (estConnecte() && idSessionCourante != null) {
//                sessionUtilisateurDAO.toucherSession(idSessionCourante);
//            }
//        }, 1, 5, TimeUnit.MINUTES);
//
//        // Synchronisation des permissions/mapping toutes les 15 secondes.
//        // Délai initial ramené à 0 : le premier chargement du mapping ne doit
//        // plus dépendre uniquement de AuthService (qui le fait déjà de façon
//        // synchrone au login) — c'est une sécurité pour ne pas retomber dans
//        // le bug "boutons cachés pendant ~15s après connexion" si ce code est
//        // un jour appelé sans passer par AuthService.seConnecter.
//        schedulerBackground.scheduleAtFixedRate(() -> {
//            if (estConnecte()) {
//                try {
//                    rafraichisseur.rafraichir();
//                } catch (Exception e) {
//                    System.err.println("Erreur lors de la synchronisation des permissions : " + e.getMessage());
//                }
//            }
//        }, 0, 15, TimeUnit.SECONDS);
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
//        codesPermissions.clear();
//        mappingActions.clear();
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
//}
