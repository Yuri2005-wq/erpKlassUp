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
    /**
     * Ouvre un modal IDENTIQUE à ouvrirModal, MAIS positionné en HAUT À DROITE
     * de l'écran (ou du nœud ancêtre si fourni).
     *
     * @param contextClass   Classe de contexte pour résoudre le FXML
     * @param fxmlPath       Chemin du FXML
     * @param titre          Titre de la fenêtre
     * @param fenetreParente Fenêtre parente
     * @param noeudAncetre   Nœud de référence (peut être null → utilise l'écran)
     * @param largeur        Largeur du modal
     * @param hauteur        Hauteur du modal
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
            // 2. Créer le Stage DÉCORÉ (identique à ouvrirModal)
            // ==========================================
            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.initStyle(StageStyle.DECORATED);

            if (fenetreParente != null) {
                stage.initOwner(fenetreParente);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            // ==========================================
            // 3. Scene normale (identique à ouvrirModal)
            // ==========================================
            Scene scene = new Scene(contenu, largeur, hauteur);
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            stage.setScene(scene);
            stage.setResizable(false);

            try {
                stage.getIcons().add(new Image(
                        HelloApplication.class.getResourceAsStream("logo.png")));
            } catch (Exception ignored) { }

            // ==========================================
            // 4. Positionnement en HAUT À DROITE
            // ==========================================
            // Marge de 20px depuis le bord droit de l'écran
            final double MARGE_ECRAN = 20;

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();

            double x;
            double y;

            if (noeudAncetre != null && noeudAncetre.getScene() != null) {
                // Aligner sur le nœud ancêtre (ex: bloc profil dans l'app-bar)
                Bounds bounds = noeudAncetre.localToScreen(noeudAncetre.getBoundsInLocal());
                if (bounds != null) {
                    // En haut à droite : le modal s'aligne sous le nœud, à droite
                    x = bounds.getMaxX() - largeur;
                    y = bounds.getMaxY() + 8;
                } else {
                    x = screen.getMaxX() - largeur - MARGE_ECRAN;
                    y = screen.getMinY() + MARGE_ECRAN;
                }
            } else {
                // Pas de nœud ancêtre → coin haut droit de l'écran
                x = screen.getMaxX() - largeur - MARGE_ECRAN;
                y = screen.getMinY() + MARGE_ECRAN;
            }

            // Empêcher la sortie d'écran
            if (x < screen.getMinX()) x = screen.getMinX();
            if (x + largeur > screen.getMaxX()) x = screen.getMaxX() - largeur;
            if (y < screen.getMinY()) y = screen.getMinY();
            if (y + hauteur > screen.getMaxY()) y = screen.getMaxY() - hauteur;

            stage.setX(x);
            stage.setY(y);

            // ==========================================
            // 5. Afficher + couleur de la barre de titre
            // ==========================================
            stage.show();
            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);

            return loader.getController();

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture popup : " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
}