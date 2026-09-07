package org.erpklassup.erpklassup.service;

public final class Autorisation {
    private Autorisation() {}

    /** Contrôle réel côté service — jamais contournable depuis l'UI, même si un bouton a été masqué. */
    public static void exigerAction(String codeAction) {
        if (!SessionManager.getInstance().peutExecuterAction(codeAction)) {
            throw new AccesRefuseException("Action non autorisée : " + codeAction);
        }
    }
}