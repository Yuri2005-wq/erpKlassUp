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

import java.net.URL;

public class ModalUtil {

    private static final String APP_BAR_COLOR = "#16213A";

    /**
     * Ouvre n'importe quel fichier FXML en utilisant une classe de contexte pour résoudre le chemin.
     *
     * @param contextClass La classe appelante (ex: getClass() ou VotreControleur.class)
     * @param fxmlPath     Le chemin relatif ou absolu vers le FXML
     * @param titre        Le titre de la fenêtre
     * @param fenetreParente La fenêtre parente
     * @param largeur      Largeur de la fenêtre
     * @param hauteur      Hauteur de la fenêtre
     */
    public static <T> T ouvrirModal(Class<?> contextClass, String fxmlPath, String titre, Window fenetreParente, double largeur, double hauteur) {
        try {
            // 1. Recherche via le ClassLoader du contexte
            URL fxmlUrl = contextClass.getResource(fxmlPath);

            // 2. Recherche secondaire si le premier essai échoue
            if (fxmlUrl == null) {
                String cleanPath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                fxmlUrl = contextClass.getClassLoader().getResource(cleanPath);
            }

            if (fxmlUrl == null) {
                throw new IllegalArgumentException("Fichier FXML introuvable : " + fxmlPath + " (Contexte : " + contextClass.getName() + ")");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent contenu = loader.load();

            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.initStyle(StageStyle.DECORATED);

            if (fenetreParente != null) {
                stage.initOwner(fenetreParente);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            Scene scene = new Scene(contenu, largeur, hauteur);
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            stage.setScene(scene);
            stage.setResizable(false);

            try {
                stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
            } catch (Exception ignored) {}

            stage.show();
            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);

            return loader.getController();

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture du modal FXML : " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
}
//package org.erpklassup.erpklassup.util;
//
//import atlantafx.base.theme.PrimerLight;
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.image.Image;
//import javafx.stage.Modality;
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//import javafx.stage.Window;
//import org.erpklassup.erpklassup.HelloApplication;
//import org.erpklassup.erpklassup.WindowsTitleBar;
//
//import java.io.IOException;
//import java.net.URL;
//
//public class ModalUtil {
//
//    private static final String APP_BAR_COLOR = "#16213A";
//
//    /**
//     * Ouvre n'importe quel fichier FXML dans un Stage personnalisé
//     */
//    public static <T> T ouvrirModal(String fxmlPath, String titre, Window fenetreParente, double largeur, double hauteur) {
//        try {
//            // ✅ 1. Normalisation du chemin FXML
//            String pathComplet = fxmlPath.startsWith("/") ? fxmlPath : "/view/" + fxmlPath;
//
//            // ✅ 2. Chargement via le ClassLoader du thread courant ou de la classe
//            URL fxmlUrl = ModalUtil.class.getResource(pathComplet);
//
//            if (fxmlUrl == null) {
//                // Recherche secondaire si le chemin direct échoue
//                fxmlUrl = HelloApplication.class.getClassLoader().getResource(fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath);
//            }
//
//            if (fxmlUrl == null) {
//                throw new IllegalArgumentException("Fichier FXML introuvable au chemin : " + pathComplet);
//            }
//
//            FXMLLoader loader = new FXMLLoader(fxmlUrl);
//            Parent contenu = loader.load();
//
//            Stage stage = new Stage();
//            stage.setTitle(titre);
//            stage.initStyle(StageStyle.DECORATED);
//
//            if (fenetreParente != null) {
//                stage.initOwner(fenetreParente);
//                stage.initModality(Modality.WINDOW_MODAL);
//            }
//
//            Scene scene = new Scene(contenu, largeur, hauteur);
//            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
//
//            stage.setScene(scene);
//            stage.setResizable(false);
//
//            try {
//                stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
//            } catch (Exception ignored) {}
//
//            stage.show();
//            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
//
//            return loader.getController();
//
//        } catch (Exception e) {
//            System.err.println("Erreur lors de l'ouverture du modal FXML : " + fxmlPath);
//            e.printStackTrace();
//            return null;
//        }
//    }
//}