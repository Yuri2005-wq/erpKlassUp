package org.erpklassup.erpklassup.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Système de Toast Notifications (popups en haut à droite)
 */
public class ToastNotification {

    public enum Type {
        SUCCESS("#16A34A", "fth-check", "Succès"),
        ERROR("#E5484D", "fth-x", "Erreur"),
        WARNING("#F5A524", "fth-alert-triangle", "Attention"),
        INFO("#3B82F6", "fth-info", "Information");

        private final String couleur;
        private final String icone;
        private final String titreDefaut;

        Type(String couleur, String icone, String titreDefaut) {
            this.couleur = couleur;
            this.icone = icone;
            this.titreDefaut = titreDefaut;
        }

        public String getCouleur() { return couleur; }
        public String getIcone() { return icone; }
        public String getTitreDefaut() { return titreDefaut; }
    }

    // ========== POSITION ==========
    public enum Position {
        HAUT_DROITE,
        HAUT_GAUCHE,
        HAUT_CENTRE,
        BAS_DROITE,
        BAS_GAUCHE,
        BAS_CENTRE
    }

    public static void afficher(Stage stage, Type type, String titre, String message) {
        afficher(stage, type, titre, message, 3000, Position.HAUT_DROITE);
    }

    public static void afficher(Stage stage, Type type, String titre, String message, int dureeMs) {
        afficher(stage, type, titre, message, dureeMs, Position.HAUT_DROITE);
    }

    public static void afficher(Stage stage, Type type, String titre, String message,
                                int dureeMs, Position position) {
        VBox toast = creerToast(type, titre, message);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(toast);

        // ✅ CALCULER LA POSITION
        double x = calculerX(stage, position, 380);
        double y = calculerY(stage, position, 100);

        popup.show(stage, x, y);

        // ✅ Animation d'entrée (glissement depuis le haut)
        toast.setTranslateY(-50);
        toast.setOpacity(0);

        TranslateTransition entree = new TranslateTransition(Duration.millis(300), toast);
        entree.setFromY(-50);
        entree.setToY(0);

        FadeTransition fonduEntree = new FadeTransition(Duration.millis(300), toast);
        fonduEntree.setFromValue(0);
        fonduEntree.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.millis(dureeMs));

        FadeTransition sortie = new FadeTransition(Duration.millis(300), toast);
        sortie.setFromValue(1.0);
        sortie.setToValue(0.0);
        sortie.setOnFinished(event -> popup.hide());

        SequentialTransition sequence = new SequentialTransition(fonduEntree, pause, sortie);
        entree.play();
        sequence.play();
    }

    // ========== RACCOURCIS ==========
    public static void succes(Stage stage, String message) {
        afficher(stage, Type.SUCCESS, "Succès", message);
    }

    public static void erreur(Stage stage, String message) {
        afficher(stage, Type.ERROR, "Erreur", message, 5000);
    }

    public static void avertissement(Stage stage, String message) {
        afficher(stage, Type.WARNING, "Attention", message, 4000);
    }

    public static void info(Stage stage, String message) {
        afficher(stage, Type.INFO, "Information", message);
    }

    // ========== CALCUL DE POSITION ==========
    private static double calculerX(Stage stage, Position position, double largeurToast) {
        double stageX = stage.getX();
        double stageWidth = stage.getWidth();

        return switch (position) {
            case HAUT_DROITE, BAS_DROITE -> stageX + stageWidth - largeurToast - 20;
            case HAUT_GAUCHE, BAS_GAUCHE -> stageX + 20;
            case HAUT_CENTRE, BAS_CENTRE -> stageX + (stageWidth / 2) - (largeurToast / 2);
        };
    }

    private static double calculerY(Stage stage, Position position, double hauteurToast) {
        double stageY = stage.getY();
        double stageHeight = stage.getHeight();

        return switch (position) {
            case HAUT_DROITE, HAUT_GAUCHE, HAUT_CENTRE -> stageY + 60;  // ✅ EN HAUT
            case BAS_DROITE, BAS_GAUCHE, BAS_CENTRE -> stageY + stageHeight - hauteurToast - 20;
        };
    }

    // ========== CRÉATION DU TOAST ==========
    private static VBox creerToast(Type type, String titre, String message) {
        VBox container = new VBox(8);
        container.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-color: " + type.getCouleur() + ";" +
                        "-fx-border-width: 0 0 0 4px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-padding: 12px 15px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);" +
                        "-fx-max-width: 350px;"
        );

        // ✅ Icône + Titre (avec bonne icône Feather)
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icone = new FontIcon(type.getIcone());
        icone.setIconSize(18);
        icone.setStyle("-fx-icon-color: " + type.getCouleur() + ";");

        Label titreLabel = new Label(titre);
        titreLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #1E293B;"
        );

        header.getChildren().addAll(icone, titreLabel);

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #64748B;"
        );

        container.getChildren().addAll(header, messageLabel);

        return container;
    }
}