package org.erpklassup.erpklassup.controllers.renduView.module2;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.erpklassup.erpklassup.util.BreadcrumbAware;
import org.erpklassup.erpklassup.util.BreadcrumbManager;
import org.erpklassup.erpklassup.util.ViewRegistry;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class StructureAcademiqueController implements Initializable, BreadcrumbAware {

    @FXML private StackPane tabContentContainer;
    @FXML private Label     lblPageCourante;

    @FXML private Button   btnTabNiveaux;
    @FXML private Button   btnTabClasses;
    @FXML private Button   btnTabSalles;
    @FXML private Button   btnTabMatieres;
    @FXML private Button btnTabPeriode;
    @FXML private Button btnClasses;

    private final Map<Button, String> tabViewsMap = new HashMap<>();

    private Button currentActiveButton = null;
    private ViewRegistry viewRegistry;

    /* ============================================================
       Constantes du fil d'Ariane
       ============================================================ */
    private static final String BC_PARENT  = "Paramètres";
    private static final String BC_SECTION = "Configuration Pédagogique";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.viewRegistry = new ViewRegistry(tabContentContainer, 4);

        tabViewsMap.put(btnTabNiveaux,  "/org/erpklassup/erpklassup/view/module2/parametre-academique.fxml");
        tabViewsMap.put(btnTabClasses,  "/org/erpklassup/erpklassup/view/module2/annee-view.fxml");
        tabViewsMap.put(btnTabSalles,   "/org/erpklassup/erpklassup/view/module2/salle-view.fxml");
        tabViewsMap.put(btnTabMatieres, "/org/erpklassup/erpklassup/view/module2/matieres_programmes.fxml");
        tabViewsMap.put(btnTabPeriode, "/org/erpklassup/erpklassup/view/module2/periodes_evaluation.fxml");
        tabViewsMap.put(btnClasses, "/org/erpklassup/erpklassup/view/module2/classes.fxml");



        if (btnTabNiveaux != null) {
            activerOnglet(btnTabNiveaux);
        }
    }
    @Override
    public void definirBreadcrumb() {
        String titre = (currentActiveButton != null) ? currentActiveButton.getText() : "Niveaux";
        BreadcrumbManager bc = BreadcrumbManager.getInstance();
        bc.definirSection("Paramètres", "Configuration Pédagogique");
        bc.definirSousSection(titre);
    }



    @FXML
    private void switchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        if (clickedButton == currentActiveButton) return;
        activerOnglet(clickedButton);
    }

    private void activerOnglet(Button bouton) {
        String fxmlPath = tabViewsMap.get(bouton);
        if (fxmlPath == null) return;

        mettreEnValeurBouton(bouton);
        viewRegistry.afficherVue(fxmlPath);

        // ✅ Titre = texte réel du bouton
        String titre = bouton.getText();
        if (lblPageCourante != null) {
            lblPageCourante.setText(titre);
        }

        // ✅ Fil d'Ariane complet (parent + section + sous-section)
        BreadcrumbManager bc = BreadcrumbManager.getInstance();
        bc.definirSection("Param\u00E8tres", "Configuration Pédagogique");
        bc.definirSousSection(titre);
    }

    private void mettreEnValeurBouton(Button boutonActif) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active-top-tab");
        }
        if (!boutonActif.getStyleClass().contains("active-top-tab")) {
            boutonActif.getStyleClass().add("active-top-tab");
        }
        currentActiveButton = boutonActif;
    }

    public void fermer() {
        if (viewRegistry != null) {
            viewRegistry.toutReinitialiser();
        }
    }
}