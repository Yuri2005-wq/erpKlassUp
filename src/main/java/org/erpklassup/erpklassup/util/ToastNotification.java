package org.erpklassup.erpklassup.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Système de Notifications Toast pour JavaFX.
 * Affiche des alertes animées temporaires en haut à droite.
 */
public class ToastNotification {

    public enum Type {
        SUCCESS("#16A34A", Feather.CHECK_CIRCLE, "Succès"),
        ERROR("#E5484D", Feather.X_CIRCLE, "Erreur"),
        WARNING("#F5A524", Feather.ALERT_TRIANGLE, "Attention"),
        INFO("#3B82F6", Feather.INFO, "Information");

        private final String couleurHex;
        private final Feather icone;
        private final String titreDefaut;

        Type(String couleurHex, Feather icone, String titreDefaut) {
            this.couleurHex = couleurHex;
            this.icone = icone;
            this.titreDefaut = titreDefaut;
        }

        public String getCouleurHex() { return couleurHex; }
        public Feather getIcone() { return icone; }
        public String getTitreDefaut() { return titreDefaut; }
    }

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
        if (stage == null || !stage.isShowing()) return;

        VBox toast = creerToast(type, titre, message);

        Popup popup = new Popup();

        // CORRECTION : Désactivation de la capture automatique des clics
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.setConsumeAutoHidingEvents(false);

        popup.getContent().add(toast);

        double x = calculerX(stage, position, 350);
        double y = calculerY(stage, position, 90);

        popup.show(stage, x, y);

        // Animation de glissement et fondu
        toast.setTranslateY(-20);
        toast.setOpacity(0);

        TranslateTransition entree = new TranslateTransition(Duration.millis(250), toast);
        entree.setFromY(-20);
        entree.setToY(0);

        FadeTransition fonduEntree = new FadeTransition(Duration.millis(250), toast);
        fonduEntree.setFromValue(0);
        fonduEntree.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.millis(dureeMs));

        FadeTransition sortie = new FadeTransition(Duration.millis(250), toast);
        sortie.setFromValue(1.0);
        sortie.setToValue(0.0);
        sortie.setOnFinished(event -> popup.hide());

        SequentialTransition sequence = new SequentialTransition(fonduEntree, pause, sortie);
        entree.play();
        sequence.play();
    }

    // ========== RACCOURCIS D'APPEL ==========
    public static void succes(Stage stage, String message) {
        afficher(stage, Type.SUCCESS, Type.SUCCESS.getTitreDefaut(), message);
    }

    public static void erreur(Stage stage, String message) {
        afficher(stage, Type.ERROR, Type.ERROR.getTitreDefaut(), message, 5000);
    }

    public static void avertissement(Stage stage, String message) {
        afficher(stage, Type.WARNING, Type.WARNING.getTitreDefaut(), message, 4000);
    }

    public static void info(Stage stage, String message) {
        afficher(stage, Type.INFO, Type.INFO.getTitreDefaut(), message);
    }

    // ========== CALCUL DE LA POSITION SUR L'ÉCRAN ==========
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
            case HAUT_DROITE, HAUT_GAUCHE, HAUT_CENTRE -> stageY + 50;
            case BAS_DROITE, BAS_GAUCHE, BAS_CENTRE -> stageY + stageHeight - hauteurToast - 20;
        };
    }

    // ========== CONSTRUCTION VISUELLE COMPOSANT TOAST ==========
    private static VBox creerToast(Type type, String titre, String message) {
        VBox container = new VBox(6);
        container.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-color: " + type.getCouleurHex() + ";" +
                        "-fx-border-width: 0 0 0 5px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-padding: 10px 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                        "-fx-min-width: 300px;" +
                        "-fx-max-width: 360px;"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        // Intégration de l'icône Feather Ikonli
        FontIcon icone = new FontIcon(type.getIcone());
        icone.setIconSize(20);
        icone.setIconColor(Color.web(type.getCouleurHex()));

        Label titreLabel = new Label(titre != null ? titre : type.getTitreDefaut());
        titreLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");

        header.getChildren().addAll(icone, titreLabel);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        container.getChildren().addAll(header, messageLabel);

        return container;
    }
}

//package org.erpklassup.erpklassup.util;
//
//import javafx.animation.FadeTransition;
//import javafx.animation.PauseTransition;
//import javafx.animation.SequentialTransition;
//import javafx.animation.TranslateTransition;
//import javafx.geometry.Pos;
//import javafx.scene.control.Label;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color;
//import javafx.stage.Popup;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//import org.kordamp.ikonli.feather.Feather;
//import org.kordamp.ikonli.javafx.FontIcon;
//
///**
// * Système de Notifications Toast pour JavaFX.
// * Affiche des alertes animées temporaires en haut à droite.
// */
//public class ToastNotification {
//
//    public enum Type {
//        SUCCESS("#16A34A", Feather.CHECK_CIRCLE, "Succès"),
//        ERROR("#E5484D", Feather.X_CIRCLE, "Erreur"),
//        WARNING("#F5A524", Feather.ALERT_TRIANGLE, "Attention"),
//        INFO("#3B82F6", Feather.INFO, "Information");
//
//        private final String couleurHex;
//        private final Feather icone;
//        private final String titreDefaut;
//
//        Type(String couleurHex, Feather icone, String titreDefaut) {
//            this.couleurHex = couleurHex;
//            this.icone = icone;
//            this.titreDefaut = titreDefaut;
//        }
//
//        public String getCouleurHex() { return couleurHex; }
//        public Feather getIcone() { return icone; }
//        public String getTitreDefaut() { return titreDefaut; }
//    }
//
//    public enum Position {
//        HAUT_DROITE,
//        HAUT_GAUCHE,
//        HAUT_CENTRE,
//        BAS_DROITE,
//        BAS_GAUCHE,
//        BAS_CENTRE
//    }
//
//    public static void afficher(Stage stage, Type type, String titre, String message) {
//        afficher(stage, type, titre, message, 3000, Position.HAUT_DROITE);
//    }
//
//    public static void afficher(Stage stage, Type type, String titre, String message, int dureeMs) {
//        afficher(stage, type, titre, message, dureeMs, Position.HAUT_DROITE);
//    }
//
//    public static void afficher(Stage stage, Type type, String titre, String message,
//                                int dureeMs, Position position) {
//        if (stage == null) return;
//
//        VBox toast = creerToast(type, titre, message);
//
//        Popup popup = new Popup();
//        popup.setAutoHide(true);
//        popup.setHideOnEscape(true);
//        popup.getContent().add(toast);
//
//        double x = calculerX(stage, position, 350);
//        double y = calculerY(stage, position, 90);
//
//        popup.show(stage, x, y);
//
//        // Animation de glissement et fondu
//        toast.setTranslateY(-20);
//        toast.setOpacity(0);
//
//        TranslateTransition entree = new TranslateTransition(Duration.millis(250), toast);
//        entree.setFromY(-20);
//        entree.setToY(0);
//
//        FadeTransition fonduEntree = new FadeTransition(Duration.millis(250), toast);
//        fonduEntree.setFromValue(0);
//        fonduEntree.setToValue(1);
//
//        PauseTransition pause = new PauseTransition(Duration.millis(dureeMs));
//
//        FadeTransition sortie = new FadeTransition(Duration.millis(250), toast);
//        sortie.setFromValue(1.0);
//        sortie.setToValue(0.0);
//        sortie.setOnFinished(event -> popup.hide());
//
//        SequentialTransition sequence = new SequentialTransition(fonduEntree, pause, sortie);
//        entree.play();
//        sequence.play();
//    }
//
//    // ========== RACCOURCIS D'APPEL ==========
//    public static void succes(Stage stage, String message) {
//        afficher(stage, Type.SUCCESS, Type.SUCCESS.getTitreDefaut(), message);
//    }
//
//    public static void erreur(Stage stage, String message) {
//        afficher(stage, Type.ERROR, Type.ERROR.getTitreDefaut(), message, 5000);
//    }
//
//    public static void avertissement(Stage stage, String message) {
//        afficher(stage, Type.WARNING, Type.WARNING.getTitreDefaut(), message, 4000);
//    }
//
//    public static void info(Stage stage, String message) {
//        afficher(stage, Type.INFO, Type.INFO.getTitreDefaut(), message);
//    }
//
//    // ========== CALCUL DE LA POSITION SUR L'ÉCRAN ==========
//    private static double calculerX(Stage stage, Position position, double largeurToast) {
//        double stageX = stage.getX();
//        double stageWidth = stage.getWidth();
//
//        return switch (position) {
//            case HAUT_DROITE, BAS_DROITE -> stageX + stageWidth - largeurToast - 20;
//            case HAUT_GAUCHE, BAS_GAUCHE -> stageX + 20;
//            case HAUT_CENTRE, BAS_CENTRE -> stageX + (stageWidth / 2) - (largeurToast / 2);
//        };
//    }
//
//    private static double calculerY(Stage stage, Position position, double hauteurToast) {
//        double stageY = stage.getY();
//        double stageHeight = stage.getHeight();
//
//        return switch (position) {
//            case HAUT_DROITE, HAUT_GAUCHE, HAUT_CENTRE -> stageY + 50;
//            case BAS_DROITE, BAS_GAUCHE, BAS_CENTRE -> stageY + stageHeight - hauteurToast - 20;
//        };
//    }
//
//    // ========== CONSTRUCTION VISUELLE COMPOSANT TOAST ==========
//    private static VBox creerToast(Type type, String titre, String message) {
//        VBox container = new VBox(6);
//        container.setStyle(
//                "-fx-background-color: #FFFFFF;" +
//                        "-fx-background-radius: 8px;" +
//                        "-fx-border-color: " + type.getCouleurHex() + ";" +
//                        "-fx-border-width: 0 0 0 5px;" +
//                        "-fx-border-radius: 8px;" +
//                        "-fx-padding: 10px 14px;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
//                        "-fx-min-width: 300px;" +
//                        "-fx-max-width: 360px;"
//        );
//
//        HBox header = new HBox(8);
//        header.setAlignment(Pos.CENTER_LEFT);
//
//        // Intégration de l'icône Feather Ikonli
//        FontIcon icone = new FontIcon(type.getIcone());
//        icone.setIconSize(20);
//        icone.setIconColor(Color.web(type.getCouleurHex()));
//
//        Label titreLabel = new Label(titre != null ? titre : type.getTitreDefaut());
//        titreLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");
//
//        header.getChildren().addAll(icone, titreLabel);
//
//        Label messageLabel = new Label(message);
//        messageLabel.setWrapText(true);
//        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
//
//        container.getChildren().addAll(header, messageLabel);
//
//        return container;
//    }
//}