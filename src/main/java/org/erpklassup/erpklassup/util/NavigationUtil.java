package org.erpklassup.erpklassup.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.erpklassup.erpklassup.WindowsTitleBar;

import java.io.IOException;

/**
 * Classe utilitaire centralisée pour la navigation.
 * Reproduit exactement le comportement de HelloController.retournerAuLogin(Stage).
 *
 * Utilisable depuis :
 *   - HelloController (déconnexion manuelle)
 *   - RafraichisseurPermissions (déconnexion forcée)
 *   - Tout autre composant ayant besoin de rediriger vers le login
 */
public final class NavigationUtil {

    private NavigationUtil() {
        throw new UnsupportedOperationException("Classe utilitaire — ne pas instancier");
    }

    /**
     * Change la scène du Stage courant vers l'écran de login.
     * (SANS créer un nouveau Stage)
     *
     * @param currentStage Le Stage à rediriger
     * @throws IOException Si le FXML de login ne peut pas être chargé
     */
    public static void retournerAuLogin(Stage currentStage) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                NavigationUtil.class.getResource("/org/erpklassup/erpklassup/view/login-view.fxml"));
        Parent loginRoot = loader.load();

        Scene loginScene = new Scene(loginRoot, 1000, 640);

        Scene sceneActuelle = currentStage.getScene();
        Parent rootActuel = sceneActuelle != null ? sceneActuelle.getRoot() : null;

        Runnable appliquerNouvelleScene = () -> {
            // ✅ sortir du mode maximisé AVANT de fixer la taille,
            // sinon setWidth/setHeight sont silencieusement ignorés par Windows
            if (currentStage.isMaximized()) {
                currentStage.setMaximized(false);
            }

            currentStage.setScene(loginScene);
            currentStage.setWidth(1000);
            currentStage.setHeight(640);
            currentStage.setResizable(false);
            currentStage.setTitle("KlassUp - Connexion");
            currentStage.centerOnScreen();

            WindowsTitleBar.setTitleBarColor(currentStage, "#16213A");

            loginRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), loginRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        };

        if (rootActuel != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), rootActuel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> appliquerNouvelleScene.run());
            fadeOut.play();
        } else {
            appliquerNouvelleScene.run();
        }
    }
}