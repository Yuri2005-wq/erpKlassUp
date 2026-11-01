package org.erpklassup.erpklassup.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.erpklassup.erpklassup.HelloApplication;
import org.erpklassup.erpklassup.WindowsTitleBar;
import atlantafx.base.theme.Styles;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilitaires pour afficher des boîtes de dialogue / alertes personnalisées.
 *
 * <p>Les fenêtres s'adaptent automatiquement à la longueur du message :
 * <ul>
 *   <li>Messages courts → fenêtre compacte</li>
 *   <li>Messages longs → fenêtre qui grandit jusqu'à {@value #HAUTEUR_MAX}px,
 *       puis un scroll apparaît</li>
 * </ul>
 */
public class AlertUtil {

    /* ═══════════════════════════════════════════════
       CONSTANTES
       ═══════════════════════════════════════════════ */
    private static final String APP_BAR_COLOR   = "#16213A";
    private static final double LARGEUR_MESSAGE = 420;
    private static final double HAUTEUR_MAX     = 600;
    private static final double LARGEUR_MIN     = 420;

    /* ═══════════════════════════════════════════════
       ENUM
       ═══════════════════════════════════════════════ */
    public enum AlertType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }

    /* ═══════════════════════════════════════════════
       RACCOURCIS PUBLICS
       ═══════════════════════════════════════════════ */

    public static void afficherInformation(String titre, String message) {
        afficherBoiteDialogue(titre, message, AlertType.INFO, null, false);
    }

    public static void afficherInformation(String titre, String message, Window parent) {
        afficherBoiteDialogue(titre, message, AlertType.INFO, parent, false);
    }

    public static void afficherSucces(String titre, String message) {
        afficherBoiteDialogue(titre, message, AlertType.SUCCESS, null, false);
    }

    public static void afficherSucces(String titre, String message, Window parent) {
        afficherBoiteDialogue(titre, message, AlertType.SUCCESS, parent, false);
    }

    public static void afficherAvertissement(String titre, String message) {
        afficherBoiteDialogue(titre, message, AlertType.WARNING, null, false);
    }

    public static void afficherAvertissement(String titre, String message, Window parent) {
        afficherBoiteDialogue(titre, message, AlertType.WARNING, parent, false);
    }

    public static void afficherErreur(String titre, String message) {
        afficherBoiteDialogue(titre, message, AlertType.ERROR, null, false);
    }

    public static void afficherErreur(String titre, String message, Window parent) {
        afficherBoiteDialogue(titre, message, AlertType.ERROR, parent, false);
    }

    public static boolean afficherConfirmation(String titre, String message) {
        return afficherBoiteDialogue(titre, message, AlertType.WARNING, null, true);
    }

    public static boolean afficherConfirmation(String titre, String message, Window parent) {
        return afficherBoiteDialogue(titre, message, AlertType.WARNING, parent, true);
    }

    /* ═══════════════════════════════════════════════
       CONSTRUCTION DE LA BOÎTE
       ═══════════════════════════════════════════════ */
    private static boolean afficherBoiteDialogue(String titre, String message,
                                                 AlertType type, Window parent,
                                                 boolean estConfirmation) {
        AtomicBoolean resultatConfirmation = new AtomicBoolean(false);

        /* ─────────── STAGE ─────────── */
        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);

        if (parent != null) {
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }

        /* ─────────── LAYOUT RACINE ─────────── */
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color: #ffffff;");

        /* ─────────── MESSAGE ─────────── */
        Label labelMessage = new Label(message != null ? message : "");
        labelMessage.setWrapText(true);
        labelMessage.setMaxWidth(LARGEUR_MESSAGE);
        labelMessage.setPrefWidth(LARGEUR_MESSAGE);
        labelMessage.setMinHeight(Region.USE_PREF_SIZE);  // le VBox ne comprime pas le Label
        labelMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        /* ─────────── SCROLL (pour messages longs) ─────────── */
        ScrollPane scrollMessage = new ScrollPane(labelMessage);
        scrollMessage.setFitToWidth(true);
        scrollMessage.setFitToHeight(false);
        scrollMessage.setPrefViewportWidth(LARGEUR_MESSAGE);
        scrollMessage.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollMessage.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollMessage.setMaxHeight(HAUTEUR_MAX - 120);       // marge pour titre + boutons
        scrollMessage.setStyle("-fx-background-color: transparent; "
                + "-fx-background: transparent; "
                + "-fx-border-color: transparent; "
                + "-fx-padding: 0;");

        // Le ScrollPane ne doit pas imposer une hauteur fixe au Label :
        // on laisse le Label déterminer sa hauteur, et le ScrollPane scrolle si ça dépasse.
        VBox.setVgrow(scrollMessage, Priority.ALWAYS);

        /* ─────────── BOUTONS ─────────── */
        HBox barreBoutons = new HBox(10);
        barreBoutons.setAlignment(Pos.CENTER_RIGHT);

        if (estConfirmation) {
            Button btnAnnuler = new Button("Annuler");
            btnAnnuler.getStyleClass().add(Styles.BUTTON_OUTLINED);
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

            if (type == AlertType.ERROR)         btnOk.getStyleClass().add(Styles.DANGER);
            else if (type == AlertType.WARNING)  btnOk.getStyleClass().add(Styles.WARNING);
            else if (type == AlertType.SUCCESS)  btnOk.getStyleClass().add(Styles.SUCCESS);

            btnOk.setOnAction(e -> stage.close());
            barreBoutons.getChildren().add(btnOk);
        }

        root.getChildren().addAll(scrollMessage, barreBoutons);

        /* ─────────── SCÈNE ─────────── */
        Scene scene = new Scene(root);
        stage.setScene(scene);

        /* ─────────── AJUSTEMENT AUTOMATIQUE DE LA TAILLE ─────────── */
        // On laisse la fenêtre prendre la taille du contenu, puis on borne
        stage.sizeToScene();
        stage.setMinWidth(LARGEUR_MIN);
        stage.setMaxHeight(HAUTEUR_MAX);

        // Ajuster la hauteur max du ScrollPane selon la taille disponible
        // après que la scène ait sa taille préférée
        stage.setOnShown(e -> {
            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
        });

        /* ─────────── ICÔNE ─────────── */
        try {
            stage.getIcons().add(new Image(
                    HelloApplication.class.getResourceAsStream("logo.png")));
        } catch (Exception ignored) {}

        /* ─────────── AFFICHAGE ─────────── */
        if (estConfirmation) {
            stage.showAndWait();
        } else {
            stage.show();
        }

        return resultatConfirmation.get();
    }
}
//package org.erpklassup.erpklassup.util;
//
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.image.Image;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.stage.Modality;
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//import javafx.stage.Window;
//import org.erpklassup.erpklassup.HelloApplication;
//import org.erpklassup.erpklassup.WindowsTitleBar;
//import atlantafx.base.theme.Styles;
//
//import java.util.concurrent.atomic.AtomicBoolean;
//
///**
// * Utilitaires pour afficher des boîtes de dialogue / alertes personnalisées.
// */
//public class AlertUtil {
//
//    private static final String APP_BAR_COLOR = "#16213A"; // Couleur de la barre de titre
//
//    public enum AlertType {
//        SUCCESS,
//        ERROR,
//        WARNING,
//        INFO
//    }
//
//    // ========== MÉTHODES PUBLIQUES RACCOURCIS ==========
//
//    public static void afficherInformation(String titre, String message) {
//        afficherBoiteDialogue(titre, message, AlertType.INFO, null, false);
//    }
//
//    public static void afficherInformation(String titre, String message, Window parent) {
//        afficherBoiteDialogue(titre, message, AlertType.INFO, parent, false);
//    }
//
//    public static void afficherSucces(String titre, String message) {
//        afficherBoiteDialogue(titre, message, AlertType.SUCCESS, null, false);
//    }
//
//    public static void afficherSucces(String titre, String message, Window parent) {
//        afficherBoiteDialogue(titre, message, AlertType.SUCCESS, parent, false);
//    }
//
//    public static void afficherAvertissement(String titre, String message) {
//        afficherBoiteDialogue(titre, message, AlertType.WARNING, null, false);
//    }
//
//    public static void afficherAvertissement(String titre, String message, Window parent) {
//        afficherBoiteDialogue(titre, message, AlertType.WARNING, parent, false);
//    }
//
//    public static void afficherErreur(String titre, String message) {
//        afficherBoiteDialogue(titre, message, AlertType.ERROR, null, false);
//    }
//
//    public static void afficherErreur(String titre, String message, Window parent) {
//        afficherBoiteDialogue(titre, message, AlertType.ERROR, parent, false);
//    }
//
//    public static boolean afficherConfirmation(String titre, String message) {
//        return afficherBoiteDialogue(titre, message, AlertType.WARNING, null, true);
//    }
//
//    public static boolean afficherConfirmation(String titre, String message, Window parent) {
//        return afficherBoiteDialogue(titre, message, AlertType.WARNING, parent, true);
//    }
//
//    // ========== CRÉATION ET AFFICHAGE DE LA BOÎTE DE DIALOGUE ==========
//
//    private static boolean afficherBoiteDialogue(String titre, String message, AlertType type, Window parent, boolean estConfirmation) {
//        AtomicBoolean resultatConfirmation = new AtomicBoolean(false);
//
//        Stage stage = new Stage();
//        stage.setTitle(titre);
//        stage.initStyle(StageStyle.DECORATED);
//        stage.setResizable(false);
//
//        if (parent != null) {
//            stage.initOwner(parent);
//            stage.initModality(Modality.WINDOW_MODAL);
//        } else {
//            stage.initModality(Modality.APPLICATION_MODAL);
//        }
//
//        // --- Layout Principal ---
//        VBox root = new VBox(15);
//        root.setPadding(new Insets(20));
//        root.setAlignment(Pos.CENTER_LEFT);
//        root.setStyle("-fx-background-color: #ffffff;");
//
//        // --- Message ---
//        Label labelMessage = new Label(message);
//        labelMessage.setWrapText(true);
//        labelMessage.setMaxWidth(380);
//        labelMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
//
//        // --- Barre de Boutons ---
//        HBox barreBoutons = new HBox(10);
//        barreBoutons.setAlignment(Pos.CENTER_RIGHT);
//
//        if (estConfirmation) {
//            Button btnAnnuler = new Button("Annuler");
//            btnAnnuler.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
//            btnAnnuler.setOnAction(e -> {
//                resultatConfirmation.set(false);
//                stage.close();
//            });
//
//            Button btnConfirmer = new Button("Confirmer");
//            btnConfirmer.getStyleClass().addAll(Styles.ACCENT, Styles.DANGER);
//            btnConfirmer.setOnAction(e -> {
//                resultatConfirmation.set(true);
//                stage.close();
//            });
//
//            barreBoutons.getChildren().addAll(btnAnnuler, btnConfirmer);
//        } else {
//            Button btnOk = new Button("D'accord");
//            btnOk.getStyleClass().add(Styles.ACCENT);
//
//            if (type == AlertType.ERROR) {
//                btnOk.getStyleClass().add(Styles.DANGER);
//            } else if (type == AlertType.WARNING) {
//                btnOk.getStyleClass().add(Styles.WARNING);
//            } else if (type == AlertType.SUCCESS) {
//                btnOk.getStyleClass().add(Styles.SUCCESS);
//            }
//
//            btnOk.setOnAction(e -> stage.close());
//            barreBoutons.getChildren().add(btnOk);
//        }
//
//        root.getChildren().addAll(labelMessage, barreBoutons);
//
//        Scene scene = new Scene(root, 420, 160);
//        stage.setScene(scene);
//
//        // --- Icône de la fenêtre ---
//        try {
//            stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
//        } catch (Exception ignored) {}
//
//        // ✅ APPLIQUER LA COULEUR UNE FOIS LA FENÊTRE AFFICHÉE (Active au premier plan)
//        stage.setOnShown(e -> {
//            WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
//        });
//
//        // --- Affichage ---
//        if (estConfirmation) {
//            stage.showAndWait(); // Bloque l'exécution jusqu'à la fermeture
//        } else {
//            stage.show();
//        }
//
//        return resultatConfirmation.get();
//    }
//}