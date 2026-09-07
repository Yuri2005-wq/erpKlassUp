package org.erpklassup.erpklassup.util;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.erpklassup.erpklassup.HelloApplication;
import org.erpklassup.erpklassup.WindowsTitleBar;

import java.io.IOException;

public class ModalUtil {

    private static final String APP_BAR_COLOR = "#16213A";

    /**
     * Ouvre n'importe quel fichier FXML dans un Stage personnalisé
     */
    public static <T> T ouvrirModal(String fxmlPath, String titre, Window fenetreParente, double largeur, double hauteur) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent contenu = loader.load();

            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.initStyle(StageStyle.DECORATED);

            // Bloquer la fenêtre parente (comportement modal)
            if (fenetreParente != null) {
                stage.initOwner(fenetreParente);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            Scene scene = new Scene(contenu, largeur, hauteur);

            // Appliquer le thème AtlantaFX
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            stage.setScene(scene);
            stage.setResizable(false);

            // Icône de l'application
            try {
                stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
            } catch (Exception e) {
                System.err.println("Impossible de charger l'icône du modal : " + e.getMessage());
            }

            stage.show();

            // Couleur de la barre de titre Windows (exécuté après le show())
            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);

            return loader.getController();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du modal FXML : " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
}