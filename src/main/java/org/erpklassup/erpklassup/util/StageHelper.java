package org.erpklassup.erpklassup.util;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Utilitaire pour récupérer le Stage parent depuis n'importe quel contrôleur
 */
public class StageHelper {

    /**
     * Récupère le Stage à partir d'un Node (bouton, label, etc.)
     */
    public static Stage getStage(Node node) {
        if (node == null) return null;

        Window window = node.getScene().getWindow();
        if (window instanceof Stage) {
            return (Stage) window;
        }
        return null;
    }

    /**
     * Récupère le Stage principal (premier Stage)
     */
    public static Stage getStagePrincipal() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        return null;
    }
}