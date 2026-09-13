package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.LogsAuditDAO;
import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.dto.FiltreAudit;
import org.erpklassup.erpklassup.models.Utilisateur;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

public class AuditService extends ServiceAsyncBase {

    private final LogsAuditDAO dao;
    private SessionManager sessionManager;   // ← plus de "final", plus d'init ici

    public AuditService() {
        this(new LogsAuditDAO());
    }

    public AuditService(LogsAuditDAO dao) {
        this.dao = dao;
        // ✅ On ne touche PLUS à SessionManager ici (c'était la cause du StackOverflowError)
    }

    /**
     * Récupère le SessionManager à la demande, après construction complète.
     */
    private SessionManager session() {
        if (sessionManager == null) {
            sessionManager = SessionManager.getInstance();
        }
        return sessionManager;
    }

    /**
     * Enregistre une action d'audit de manière asynchrone (non bloquante pour l'UI).
     */
    public void tracerActionAsync(String categorie, String action, String details) {
        SessionManager sm = session();
        if (!sm.estConnecte()) {
            return;
        }

        Utilisateur user = sm.getUtilisateurCourant();
        String idEcole = sm.getIdEcoleCourante();
        String idUser = user.getIdUtilisateur();
        String nomAuteur = user.getNomComplet();

        String adresseIp = obtenirAdresseIpLocale();
        String appareil = obtenirInfoAppareil();

        executer(
                () -> {
                    dao.inserer(idEcole, idUser, nomAuteur, categorie, action, details, adresseIp, appareil);
                    return null;
                },
                inutilise -> {},
                erreur -> System.err.println("Échec du traçage d'audit : " + erreur.getMessage())
        );
    }


    /**
     * Parcourt les interfaces réseau pour extraire l'adresse IPv4 réseau réelle de la machine.
     */
    private String obtenirAdresseIpLocale() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }

            InetAddress localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                return localHost.getHostAddress();
            }
        } catch (Exception e) {
            System.err.println("Impossible de déterminer l'adresse IP locale : " + e.getMessage());
        }

        return "127.0.0.1";
    }

    /**
     * Génère une chaîne descriptive complète de la machine (Nom d'hôte + OS + Architecture).
     */
    private String obtenirInfoAppareil() {
        String hostName = "Machine";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}

        String osName = System.getProperty("os.name", "Inconnu");
        String osArch = System.getProperty("os.arch", "");

        return String.format("%s (%s %s)", hostName, osName, osArch).trim();
    }

    /**
     * Recherche asynchrone des lignes d'audit selon les filtres spécifiés.
     */
    public void rechercherAsync(FiltreAudit filtre, Consumer<List<AuditLigne>> onSucces, Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(
                () -> dao.rechercher(filtre),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); }
        );
    }

    /**
     * Récupère la liste distincte des catégories d'audit disponibles pour une école.
     */
    public void listerCategoriesAsync(String idEcole, Consumer<List<String>> onSucces, Consumer<Throwable> onErreur) {
        executer(
                () -> dao.listerCategoriesDistinctes(idEcole),
                onSucces,
                onErreur
        );
    }

    public List<AuditLigne> rechercher(FiltreAudit filtre) {
        return dao.rechercher(filtre);
    }
}

//package org.erpklassup.erpklassup.service;
//
//import org.erpklassup.erpklassup.dao.LogsAuditDAO;
//import org.erpklassup.erpklassup.dto.AuditLigne;
//import org.erpklassup.erpklassup.dto.FiltreAudit;
//import org.erpklassup.erpklassup.models.Utilisateur;
//
//import java.util.List;
//import java.util.function.Consumer;
//
//public class AuditService extends ServiceAsyncBase {
//
//    private final LogsAuditDAO dao;
//    private final SessionManager sessionManager;
//
//    public AuditService() {
//        this(new LogsAuditDAO());
//    }
//
//    public AuditService(LogsAuditDAO dao) {
//        this.dao = dao;
//        this.sessionManager = SessionManager.getInstance();
//    }
//
//    /**
//     * Enregistre une action d'audit de manière asynchrone (non bloquante pour l'UI).
//     *
//     * @param categorie La catégorie fonctionnelle (ex: "ELEVE", "PAYEMENT")
//     * @param action    Le type d'action effectuée (ex: "CREATION", "SUPPRESSION")
//     * @param details   La description détaillée de l'opération
//     */
//    public void tracerActionAsync(String categorie, String action, String details) {
//        if (!sessionManager.estConnecte()) {
//            return;
//        }
//
//        Utilisateur user = sessionManager.getUtilisateurCourant();
//        String idEcole = sessionManager.getIdEcoleCourante();
//        String idUser = user.getIdUtilisateur();
//        String nomAuteur = user.getNomComplet();
//
//        String adresseIp = "127.0.0.1";
//        String appareil = "Appareil (" + System.getProperty("os.name") + ")";
//
//        // ✅ Correction : On retourne null dans le Callable et on remplace la lambda inutilisée par un Runnable vide
//        executer(
//                () -> {
//                    dao.inserer(idEcole, idUser, nomAuteur, categorie, action, details, adresseIp, appareil);
//                    return null;
//                },
//                inutilise -> {},
//                erreur -> System.err.println("Échec du traçage d'audit : " + erreur.getMessage())
//        );
//    }
//
//    /**
//     * Recherche asynchrone des lignes d'audit selon les filtres spécifiés.
//     */
//    public void rechercherAsync(FiltreAudit filtre, Consumer<List<AuditLigne>> onSucces, Consumer<Throwable> onErreur) {
//        long jeton = nouvelleRequete();
//        executer(
//                () -> dao.rechercher(filtre),
//                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
//                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); }
//        );
//    }
//
//    /**
//     * Récupère la liste distincte des catégories d'audit disponibles pour une école.
//     */
//    public void listerCategoriesAsync(String idEcole, Consumer<List<String>> onSucces, Consumer<Throwable> onErreur) {
//        executer(
//                () -> dao.listerCategoriesDistinctes(idEcole),
//                onSucces,
//                onErreur
//        );
//    }
//}