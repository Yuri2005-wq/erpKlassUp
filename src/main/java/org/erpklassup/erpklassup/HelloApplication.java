package org.erpklassup.erpklassup;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.sync.SyncBootstrap;
import org.erpklassup.erpklassup.util.AlertUtil;

import java.io.IOException;
import java.util.Optional;

public class HelloApplication extends Application {

    private static final String APP_BAR_COLOR = "#16213A";

    @Override
    public void start(Stage stage) throws IOException {
        Database.initializeDatabase();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            exception.printStackTrace();
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Erreur inattendue : " + exception.getMessage()).showAndWait());
        });

        stage.initStyle(StageStyle.DECORATED);

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("view/login-view.fxml"));
        Pane fxmlContent = fxmlLoader.load();

        Scene scene = new Scene(fxmlContent, 1000, 600);
        stage.setTitle("KlassUp");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));

        stage.setOpacity(0);
        stage.show(); // crée le HWND natif — obligatoire avant tout appel DWM

        WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
        WindowsTitleBar.enregistrerStage(stage); // met en cache le HWND pour toute la suite de l'appli

        stage.setOpacity(1);

        // Gestion de la fermeture de la fenêtre avec alerte de confirmation
        stage.setOnCloseRequest(event -> {

            boolean confirmation = AlertUtil.afficherConfirmation(
                    "Fermeture de l'application", "êtes-vous sûr de vouloir fermer l'application?",
                    stage
            );

            if (confirmation) {
                SessionManager sessionManager = SessionManager.getInstance();
                if (sessionManager.estConnecte()) {
                    String idSession = sessionManager.getIdSessionCourante();
                    if (idSession != null) {
                        new SessionUtilisateurDAO().InvaliderSession(idSession, "CLOSE_APP");
                    }
                }
                Database.fermerPool();
                SyncBootstrap.shutdown();
            } else {
                event.consume();
            }
        });
    }


    public static void main(String[] args) {
        launch();
    }
}




//package org.erpklassup.erpklassup;
//
//import atlantafx.base.theme.PrimerLight;
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.scene.control.Alert;
//import javafx.scene.image.Image;
//import javafx.scene.layout.Pane;
//import javafx.scene.shape.SVGPath;
//import javafx.scene.paint.Color;
//
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.sync.SyncBootstrap;
//
//import java.io.IOException;
//
//public class HelloApplication extends Application {
//
//    private static final String APP_BAR_COLOR = "#16213A";
//
//    @Override
//    public void start(Stage stage) throws IOException {
//        Database.initializeDatabase();
//        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
//            exception.printStackTrace();
//            Platform.runLater(() ->
//                    new Alert(Alert.AlertType.ERROR, "Erreur inattendue : " + exception.getMessage()).showAndWait());
//        });
//
//        stage.initStyle(StageStyle.DECORATED);
//
//
//        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
//
//        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("view/login-view.fxml"));
//        Pane fxmlContent = fxmlLoader.load();
//
//        Scene scene = new Scene(fxmlContent, 1000, 600);
//        stage.setTitle("KlassUp");
//        stage.setResizable(false);
//        stage.setScene(scene);
//        stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
//
//        stage.setOpacity(0);
//        stage.show(); // crée le HWND natif — obligatoire avant tout appel DWM
//
//        WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
//        WindowsTitleBar.enregistrerStage(stage); // met en cache le HWND pour toute la suite de l'appli
//
//
//        stage.setOpacity(1);
//        stage.setOnCloseRequest(event -> {
//            SessionManager sessionManager = SessionManager.getInstance();
//            if (sessionManager.estConnecte()) {
//                String idSession = sessionManager.getIdSessionCourante();
//                if (idSession != null) {
//                    new SessionUtilisateurDAO().InvaliderSession(idSession, "CLOSE_APP");
//                }
//            }
//            Database.fermerPool();
//            SyncBootstrap.shutdown();
//        });
//    }
//
//    public static void main(String[] args) {
//        launch();
//    }
//}