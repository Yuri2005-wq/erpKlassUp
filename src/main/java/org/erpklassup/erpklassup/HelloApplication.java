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

        // ✅ La fenêtre est créée mais totalement invisible : le HWND existe
        // (on peut donc appeler DWM dessus) mais l'utilisateur ne voit rien
        // tant qu'on n'a pas remis l'opacité à 1.
        stage.setOpacity(0);
        stage.show(); // crée le HWND natif — obligatoire avant tout appel DWM

        WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
        WindowsTitleBar.enregistrerStage(stage); // met en cache le HWND pour toute la suite de l'appli

        // ✅ On ne révèle la fenêtre qu'une fois la couleur déjà appliquée :
        // plus aucun flash de barre de titre blanche.
        stage.setOpacity(1);
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
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//import org.erpklassup.erpklassup.util.ToastNotification;
//import org.kordamp.ikonli.IkonHandler;
//import org.kordamp.ikonli.feather.Feather;
//import org.kordamp.ikonli.feather.FeatherIkonHandler;
//import org.kordamp.ikonli.javafx.FontIcon;
//import org.kordamp.ikonli.javafx.IkonResolver;
//
//import java.io.IOException;
//
//import static org.erpklassup.erpklassup.Database.initializeDatabase;
//
//public class HelloApplication extends Application {
//
//    // Votre couleur personnalisée
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
//        // Garder la décoration native Windows (barre de titre avec boutons)
//        stage.initStyle(StageStyle.DECORATED);
//
//        // Charger le FXML
//        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
//
//        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("view/login-view.fxml"));
//        Pane fxmlContent = fxmlLoader.load();
//
//        Scene scene = new Scene(fxmlContent, 1000, 600);
//        stage.setTitle("KlassUp");
//        stage.setResizable(false);
//        stage.setScene(scene);
//        stage.show();
//
//        WindowsTitleBar.setTitleBarColor(stage, APP_BAR_COLOR);
//
//        //Configuration du nom de la fenetre et de l'icône d'application
//        stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("logo.png")));
//
//    }
//
//    public static void main(String[] args) {
//        launch();
//    }
//}