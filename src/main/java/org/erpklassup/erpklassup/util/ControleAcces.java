package org.erpklassup.erpklassup.util;

import javafx.scene.Node;
import org.erpklassup.erpklassup.service.SessionManager;

public final class ControleAcces {
    private ControleAcces() {}

    /** Vérifie une action métier (via ActionPermission) — à utiliser partout dans l'UI. */
    public static void appliquerAction(Node controle, String codeAction) {
        boolean autorise = SessionManager.getInstance().peutExecuterAction(codeAction);
        controle.setDisable(!autorise);
        controle.setVisible(autorise);
        controle.setManaged(autorise);
    }

    public static boolean autoriseAction(String codeAction) {
        return SessionManager.getInstance().peutExecuterAction(codeAction);
    }
}