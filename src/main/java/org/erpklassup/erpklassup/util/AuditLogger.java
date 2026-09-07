package org.erpklassup.erpklassup.util;

import org.erpklassup.erpklassup.dao.LogsAuditDAO;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.SessionManager;

/** Écriture asynchrone, jamais bloquante, jamais fatale : une panne d'audit ne doit jamais casser l'action métier. */
public final class AuditLogger {

    private static final LogsAuditDAO DAO = new LogsAuditDAO();

    private AuditLogger() {}

    public static void enregistrer(String categorie, String action, String details) {
        var session = SessionManager.getInstance();
        String idEcole = session.getIdEcoleCourante();
        if (idEcole == null) return; // pas de session active, rien à journaliser

        String idUtilisateur = session.getIdUtilisateurCourant();
        String nomAuteur = session.getUtilisateurCourant() != null
                ? session.getUtilisateurCourant().getNomComplet() : "Système";

        AppExecutor.get().submit(() -> {
            try {
                DAO.inserer(idEcole, idUtilisateur, nomAuteur, categorie, action, details, null, null);
            } catch (Exception e) {
                System.err.println("Erreur écriture audit : " + e.getMessage());
            }
        });
    }
}