package org.erpklassup.erpklassup;

import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;

/** Bascule un bouton entre son état normal et un état "chargement" (spinner + texte), sans changer sa largeur. */
public final class BoutonChargement {

    private BoutonChargement() {}

    public static void demarrer(Button bouton, String texteChargement) {
        bouton.setUserData(bouton.getText());     // conserve le texte d'origine
        bouton.setPrefWidth(bouton.getWidth());   // fige la largeur actuelle → pas de saut visuel quand le texte change

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(16, 16);
        spinner.setMaxSize(16, 16);
        spinner.getStyleClass().add("bouton-spinner");

        bouton.setGraphic(spinner);
        bouton.setText(texteChargement);
        bouton.setDisable(true);
    }

    public static void arreter(Button bouton) {
        Object texteOriginal = bouton.getUserData();
        bouton.setGraphic(null);
        bouton.setText(texteOriginal != null ? texteOriginal.toString() : bouton.getText());
        bouton.setDisable(false);
        bouton.setPrefWidth(Region.USE_COMPUTED_SIZE); // libère la largeur figée
    }
}