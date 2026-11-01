package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.AnimationsUtil;
import org.erpklassup.erpklassup.util.ViewRegistry;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur de la page d'accueil des Paramètres : grille de catégories
 * (Pédagogique, Financière, Système, ...).
 * Un double-clic sur une carte joue le rebond de feedback puis navigue
 * en réutilisant le ViewRegistry déjà enregistré par HelloController
 * dans SessionManager — pas de système de navigation séparé.
 */
public class SettingsAccueilController implements Initializable {

    @FXML private VBox cardPedagogique;
    @FXML private VBox cardFinance;
    @FXML private VBox cardSysteme;

    // ⚠️ Adapte ce chemin si ta page Système ne s'appelle pas "user.fxml"
    private static final String FXML_PEDAGOGIQUE = "/org/erpklassup/erpklassup/view/SettingsPedagogiqueTab.fxml";
    private static final String FXML_FINANCE      = "/org/erpklassup/erpklassup/view/SettingsFinanceTab.fxml";
    private static final String FXML_SYSTEME      = "/org/erpklassup/erpklassup/view/user.fxml";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        brancherCarte(cardPedagogique, FXML_PEDAGOGIQUE);
        brancherCarte(cardFinance, FXML_FINANCE);
        brancherCarte(cardSysteme, FXML_SYSTEME);
    }

    private void brancherCarte(VBox carte, String fxmlCible) {
        if (carte == null) return;

        carte.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                AnimationsUtil.rebondClic(carte, () -> naviguerVers(fxmlCible));
            }
        });
    }

    private void naviguerVers(String fxmlCible) {
        // ⚠️ Suppose que SessionManager expose un getter du ViewRegistry
        // enregistré via initialiserContexteGraphique(stage, viewRegistry)
        // dans HelloController. Si le getter porte un autre nom chez toi,
        // remplace l'appel ci-dessous.
        ViewRegistry viewRegistry = SessionManager.getInstance().getViewRegistry();

        if (viewRegistry != null) {
            viewRegistry.afficherVue(fxmlCible);
        } else {
            System.err.println("⚠️ [SettingsAccueilController] ViewRegistry introuvable dans SessionManager");
        }
    }
}
