package org.erpklassup.erpklassup.util;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.erpklassup.erpklassup.HelloApplication;
import org.erpklassup.erpklassup.WindowsTitleBar;

import java.net.URL;

public class ModalUtil {

    private static final String APP_BAR_COLOR = "#16213A";

    /** Padding laissé autour du contenu pour laisser respirer l'ombre portée. */
    private static final double PADDING_OMBRE = 30;

    // ==========================================
    // MODAL CLASSIQUE (centré, avec décoration)
    // ==========================================
    /**
     * Ouvre un modal centré, avec décoration Windows.
     * Utilise I18nManager pour résoudre les %clés du FXML.
     *
     * @param contextClass   Classe de contexte pour résoudre le FXML
     * @param fxmlPath       Chemin du FXML (relatif ou absolu)
     * @param titre          Titre de la fenêtre
     * @param fenetreParente Fenêtre parente (propriétaire)
     * @param largeur        Largeur de la fenêtre
     * @param hauteur        Hauteur de la fenêtre
     */
    public static <T> T ouvrirModal(Class<?> contextClass,
                                    String fxmlPath,
                                    String titre,
                                    Window fenetreParente,
                                    double largeur,
                                    double hauteur) {
        try {
            // ✅ Utilise I18nManager pour créer le loader avec le ResourceBundle
            FXMLLoader loader = I18nManager.getInstance().creerLoader(contextClass, fxmlPath);
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
            } catch (Exception ignored) {
                // Pas d'icône → pas grave
            }

            stage.show();
            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);

            return loader.getController();

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture du modal FXML : " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    // ==========================================
    // POPUP TRANSPARENTE (sous un nœud)
    // ==========================================
    /**
     * Ouvre une popup (Stage transparent, sans décoration) positionnée SOUS un nœud ancêtre.
     * Supporte les coins arrondis et l'ombre portée grâce à un fond de scène transparent.
     * Utilise I18nManager pour résoudre les %clés du FXML.
     *
     * @param contextClass   Classe de contexte pour résoudre le FXML
     * @param fxmlPath       Chemin du FXML (relatif ou absolu)
     * @param titre          Titre de la fenêtre (souvent vide pour une popup)
     * @param fenetreParente Fenêtre parente (propriétaire)
     * @param noeudAncetre   Nœud sous lequel positionner la popup
     * @param largeur        Largeur VISIBLE (sans le padding d'ombre)
     * @param hauteur        Hauteur VISIBLE (sans le padding d'ombre)
     */
    public static <T> T ouvrirPopupSous(Class<?> contextClass,
                                        String fxmlPath,
                                        String titre,
                                        Window fenetreParente,
                                        Node noeudAncetre,
                                        double largeur,
                                        double hauteur) {
        try {
            // ==========================================
            // 1. Charger le FXML avec le bundle i18n
            // ==========================================
            FXMLLoader loader = I18nManager.getInstance().creerLoader(contextClass, fxmlPath);
            Parent contenu = loader.load();

            // ==========================================
            // 2. Créer le Stage transparent
            // ==========================================
            Stage stage = new Stage();
            stage.setTitle(titre != null ? titre : "");
            stage.initStyle(StageStyle.TRANSPARENT);   // ✅ Pas de décoration Windows
            stage.setAlwaysOnTop(true);

            if (fenetreParente != null) {
                stage.initOwner(fenetreParente);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            // ==========================================
            // 3. Scene transparente + taille élargie pour l'ombre
            // ==========================================
            double sceneWidth = largeur + 2 * PADDING_OMBRE;
            double sceneHeight = hauteur + 2 * PADDING_OMBRE;

            Scene scene = new Scene(contenu, sceneWidth, sceneHeight);

            // ⚠️ ORDRE CRUCIAL :
            //   1) setFill AVANT setUserAgentStylesheet
            //   2) setUserAgentStylesheet peut écraser le fill
            scene.setFill(Color.TRANSPARENT);

            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            stage.setScene(scene);
            stage.setResizable(false);

            // ==========================================
            // 4. Position sous le nœud ancêtre
            // ==========================================
            if (noeudAncetre != null && noeudAncetre.getScene() != null) {
                Bounds bounds = noeudAncetre.localToScreen(noeudAncetre.getBoundsInLocal());
                if (bounds != null) {
                    // Aligner à droite du nœud (compensation du padding gauche)
                    double x = bounds.getMaxX() - largeur - PADDING_OMBRE;
                    double y = bounds.getMaxY() - PADDING_OMBRE + 8;

                    // Empêcher la sortie d'écran
                    Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                    if (x < screen.getMinX()) {
                        x = screen.getMinX();
                    }
                    if (x + sceneWidth > screen.getMaxX()) {
                        x = screen.getMaxX() - sceneWidth;
                    }
                    if (y + sceneHeight > screen.getMaxY()) {
                        // Basculer au-dessus du nœud si pas assez de place en dessous
                        y = bounds.getMinY() - sceneHeight + PADDING_OMBRE - 8;
                    }

                    stage.setX(x);
                    stage.setY(y);
                }
            }

            // ==========================================
            // 5. Fermeture automatique au clic ailleurs
            // ==========================================
            stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (wasFocused && !isNowFocused) {
                    stage.close();
                }
            });

            // ==========================================
            // 6. Afficher + re-forcer la transparence
            // ==========================================
            stage.show();

            // ✅ Double sécurité : Windows peut remettre un fond après show()
            stage.getScene().setFill(Color.TRANSPARENT);

            return loader.getController();

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture popup : " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
}