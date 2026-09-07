package org.erpklassup.erpklassup;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

import static org.erpklassup.erpklassup.Database.initializeDatabase;

public class HelloApplication extends Application {

    // Votre couleur personnalisée
    private static final String APP_BAR_COLOR = "#16213A";

    @Override
    public void start(Stage stage) throws IOException {
        Database.initializeDatabase();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            exception.printStackTrace();
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Erreur inattendue : " + exception.getMessage()).showAndWait());
        });
        // Garder la décoration native Windows (barre de titre avec boutons)
        stage.initStyle(StageStyle.DECORATED);

        // Charger le FXML
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("view/login-view.fxml"));
        Pane fxmlContent = fxmlLoader.load();

        Scene scene = new Scene(fxmlContent, 1000, 600);
        stage.setTitle("KlassUp");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        // Appliquer la couleur à la barre de titre
        WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);

        //Configuration du nom de la fenetre et de l'icône d'application
        stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));

    }

    public static void main(String[] args) {
        launch();
    }
}