package org.erpklassup.erpklassup.util;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.erpklassup.erpklassup.HelloApplication;
import org.erpklassup.erpklassup.WindowsTitleBar;

import java.util.concurrent.atomic.AtomicBoolean;

public class AlertUtil {

    private static final String APP_BAR_COLOR = "#16213A";

    public enum AlertType {
        INFORMATION,
        SUCCESS,
        WARNING,
        ERROR,
        CONFIRMATION
    }

    /**
     * Affiche une alerte personnalisée avec la barre de titre stylisée.
     */
    public static void afficherAlerte(String titre, String message, AlertType type, Window parent) {
        afficherBoiteDialogue(titre, message, type, parent, false);
    }

    /**
     * Affiche une alerte de confirmation (Oui / Non) stylisée.
     * @return true si l'utilisateur a cliqué sur le bouton de confirmation, false sinon.
     */
    public static boolean afficherConfirmation(String titre, String message, Window parent) {
        return afficherBoiteDialogue(titre, message, AlertType.CONFIRMATION, parent, true);
    }

    private static boolean afficherBoiteDialogue(String titre, String message, AlertType type, Window parent, boolean estConfirmation) {
        AtomicBoolean resultatConfirmation = new AtomicBoolean(false);

        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);

        if (parent != null) {
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
        }

        // Layout Principal
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color: #ffffff;");

        // Label du message
        Label labelMessage = new Label(message);
        labelMessage.setWrapText(true);
        labelMessage.setMaxWidth(380);
        labelMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        // Zone des Boutons
        HBox barreBoutons = new HBox(10);
        barreBoutons.setAlignment(Pos.CENTER_RIGHT);

        if (estConfirmation) {
            Button btnAnnuler = new Button("Annuler");
            btnAnnuler.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
            btnAnnuler.setOnAction(e -> {
                resultatConfirmation.set(false);
                stage.close();
            });

            Button btnConfirmer = new Button("Confirmer");
            btnConfirmer.getStyleClass().addAll(Styles.ACCENT, Styles.DANGER);
            btnConfirmer.setOnAction(e -> {
                resultatConfirmation.set(true);
                stage.close();
            });

            barreBoutons.getChildren().addAll(btnAnnuler, btnConfirmer);
        } else {
            Button btnOk = new Button("D'accord");
            btnOk.getStyleClass().add(Styles.ACCENT);

            // Adapter le style du bouton selon le type d'alerte
            if (type == AlertType.ERROR) {
                btnOk.getStyleClass().add(Styles.DANGER);
            } else if (type == AlertType.WARNING) {
                btnOk.getStyleClass().add(Styles.WARNING);
            } else if (type == AlertType.SUCCESS) {
                btnOk.getStyleClass().add(Styles.SUCCESS);
            }

            btnOk.setOnAction(e -> stage.close());
            barreBoutons.getChildren().add(btnOk);
        }

        root.getChildren().addAll(labelMessage, barreBoutons);

        // Scene et Stage
        Scene scene = new Scene(root, 420, 160);
        stage.setScene(scene);

        // Charger l'icône de l'application
        try {
            stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
        } catch (Exception ignored) {}

        stage.show();

        // Appliquer la couleur personnalisée à la barre de titre
        WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);

        if (estConfirmation) {
            stage.showAndWait(); // Attend l'action utilisateur en mode confirmation
        }

        return resultatConfirmation.get();
    }
}