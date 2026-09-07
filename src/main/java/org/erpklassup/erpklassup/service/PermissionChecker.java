package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.ActionPermissionDAO;

import java.util.Map;

/**
 * Vérificateur central des permissions (Système de portes génériques)
 *
 * Cette classe fait le lien entre :
 * - Les actions du code Java (ex: "eleve.supprimer")
 * - Les permissions en base de données (ex: "ELEVE_DELETE")
 * - L'utilisateur connecté (via SessionManager)
 */
public class PermissionChecker {

//    private static PermissionChecker instance;
//    private final ActionPermissionDAO actionPermissionDAO;
//    private final SessionManager sessionManager;
//    private Map<String, String> cacheActions;
//
//    private PermissionChecker() {
//        this.actionPermissionDAO = new ActionPermissionDAO();
//        this.sessionManager = SessionManager.getInstance();
//        this.cacheActions = actionPermissionDAO.chargerToutesActions();
//    }
//
//    /**
//     * Retourne l'instance unique (Singleton)
//     */
//    public static PermissionChecker getInstance() {
//        if (instance == null) {
//            instance = new PermissionChecker();
//        }
//        return instance;
//    }
//
//    /**
//     * Vérifie si l'utilisateur courant peut effectuer une action
//     *
//     * @param codeAction Ex: "eleve.supprimer", "note.creer", "paiement.valider"
//     * @return true si l'utilisateur a la permission requise, false sinon
//     */
//    public boolean peutFaire(String codeAction) {
//        // 1. Vérifier si l'utilisateur est connecté
//        if (!sessionManager.estConnecte()) {
//            System.err.println("❌ Aucun utilisateur connecté");
//            return false;
//        }
//
//        // 2. Chercher la permission requise pour cette action
//        String permissionRequise = cacheActions.get(codeAction);
//
//        // 3. Si l'action n'existe pas dans le registre → refuser
//        if (permissionRequise == null) {
//            System.err.println("❌ Action inconnue : " + codeAction);
//            return false;
//        }
//
//        // 4. Vérifier si l'utilisateur a cette permission
//        boolean autorise = sessionManager.aLaPermission(permissionRequise);
//
//        // 5. Log pour le débogage
//        if (autorise) {
//            System.out.println("✅ Action autorisée : " + codeAction + " (permission : " + permissionRequise + ")");
//        } else {
//            System.out.println("⛔ Action refusée : " + codeAction + " (permission requise : " + permissionRequise + ")");
//        }
//
//        return autorise;
//    }
//
//    /**
//     * Retourne la permission requise pour une action donnée
//     * Utile pour afficher un message d'erreur précis
//     *
//     * @param codeAction Ex: "eleve.supprimer"
//     * @return Le code de la permission requise, ou null si l'action est inconnue
//     */
//    public String getPermissionRequise(String codeAction) {
//        return cacheActions.get(codeAction);
//    }
//
//    /**
//     * Rafraîchit le cache des actions depuis la base de données
//     * À appeler si l'admin ajoute/modifie des actions
//     */
//    public void rafraichirCache() {
//        this.cacheActions = actionPermissionDAO.chargerToutesActions();
//        System.out.println("✅ Cache des actions rafraîchi : " + cacheActions.size() + " actions chargées");
//    }
//
//    /**
//     * Vérifie si une action existe dans le registre
//     */
//    public boolean actionExiste(String codeAction) {
//        return cacheActions.containsKey(codeAction);
//    }
//
//    /**
//     * Retourne le nombre d'actions enregistrées
//     */
//    public int getNombreActions() {
//        return cacheActions.size();
//    }
}