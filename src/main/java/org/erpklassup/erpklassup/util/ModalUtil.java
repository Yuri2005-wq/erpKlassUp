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
    /**
     * Ouvre un modal positionné SOUS un nœud ancêtre (comme une popup).
     * Utilise le pattern modal mais avec positionnement personnalisé.
     *
     * @param contextClass    Classe de contexte pour résoudre le FXML
     * @param fxmlPath        Chemin du FXML
     * @param titre           Titre de la fenêtre (souvent vide pour une popup)
     * @param fenetreParente  Fenêtre parente
     * @param noeudAncetre    Nœud sous lequel positionner la popup
     * @param largeur         Largeur
     * @param hauteur         Hauteur
     */
    public static <T> T ouvrirPopupSous(Class<?> contextClass,
                                        String fxmlPath,
                                        String titre,
                                        Window fenetreParente,
                                        javafx.scene.Node noeudAncetre,
                                        double largeur,
                                        double hauteur) {
        try {
            // 1. Charger le FXML
            URL fxmlUrl = contextClass.getResource(fxmlPath);
            if (fxmlUrl == null) {
                String cleanPath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                fxmlUrl = contextClass.getClassLoader().getResource(cleanPath);
            }
            if (fxmlUrl == null) {
                throw new IllegalArgumentException("FXML introuvable : " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent contenu = loader.load();

            // 2. Créer le Stage
            Stage stage = new Stage();
            stage.setTitle(titre != null ? titre : "");
            stage.initStyle(StageStyle.UNDECORATED);   // ✅ Pas de barre de titre
            stage.setAlwaysOnTop(true);                // ✅ Au-dessus des autres fenêtres

            if (fenetreParente != null) {
                stage.initOwner(fenetreParente);
                stage.initModality(Modality.WINDOW_MODAL);  // ✅ Bloque les clics sur la fenêtre parente
            }

            Scene scene = new Scene(contenu, largeur, hauteur);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);  // ✅ Fond transparent
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            stage.setScene(scene);
            stage.setResizable(false);

            // 3. Positionner SOUS le nœud ancêtre
            if (noeudAncetre != null && noeudAncetre.getScene() != null) {
                javafx.geometry.Bounds bounds = noeudAncetre.localToScreen(
                        noeudAncetre.getBoundsInLocal());
                if (bounds != null) {
                    // Aligner à droite du nœud
                    double x = bounds.getMaxX() - largeur;
                    double y = bounds.getMaxY() + 8;   // 8px sous le nœud

                    // Empêcher la popup de sortir de l'écran
                    javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                    javafx.geometry.Rectangle2D screenBounds = screen.getVisualBounds();

                    if (x < screenBounds.getMinX()) x = screenBounds.getMinX();
                    if (x + largeur > screenBounds.getMaxX()) x = screenBounds.getMaxX() - largeur;
                    if (y + hauteur > screenBounds.getMaxY()) y = bounds.getMinY() - hauteur - 8;

                    stage.setX(x);
                    stage.setY(y);
                }
            }

            // 4. Fermeture automatique quand on clique ailleurs
            stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (wasFocused && !isNowFocused) {
                    stage.close();
                }
            });

            stage.show();
            return loader.getController();

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture popup : " + fxmlPath);
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