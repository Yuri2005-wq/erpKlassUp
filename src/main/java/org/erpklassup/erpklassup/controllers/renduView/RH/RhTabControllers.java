package org.erpklassup.erpklassup.controllers.renduView.RH;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.erpklassup.erpklassup.util.BreadcrumbManager;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ViewRegistry;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Contrôleur principal pour la gestion de l'onglet de configuration Système
 * (Utilisateurs, Rôles & Permissions, Journal d'audit). Il délègue la gestion
 * de la mémoire, de l'état et de l'affichage des sous-vues au {@link ViewRegistry}.
 */
public class RhTabControllers implements Initializable {

    @FXML private StackPane tabContentContainer;
    @FXML private Button btnTableau;
    @FXML private Button btnListePersonnel;
    @FXML private Button btnDepartements;
    @FXML private Button btnPostes;

    private final Map<Button, String> tabViewsMap = new HashMap<>();
    private Button currentActiveButton = null;
    private ViewRegistry viewRegistry;
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    // Fourni par le shell des Paramètres après chargement de cette vue.
    private Runnable onRetourAccueil;

    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.viewRegistry = new ViewRegistry(tabContentContainer, 3);

        tabViewsMap.put(btnTableau, "/org/erpklassup/erpklassup/view/GRH/tableauBordGrh.fxml");
        tabViewsMap.put(btnListePersonnel, "/org/erpklassup/erpklassup/view/GRH/listePersonnel.fxml");
        tabViewsMap.put(btnDepartements, "/org/erpklassup/erpklassup/view/GRH/departement.fxml");
        tabViewsMap.put(btnPostes, "/org/erpklassup/erpklassup/view/GRH/poste.fxml");


        if (btnTableau != null) {
            mettreEnValeurBouton(btnTableau);
            viewRegistry.afficherVue(tabViewsMap.get(btnTableau));
            mettreAJourTitrePage(btnTableau);
            BreadcrumbManager.getInstance().definirSousSection(btnTableau.getText());
        }

//        if (lblCategorieCourante != null) {
//            lblCategorieCourante.setText(i18n().t("settings.categorie.systeme"));
//        }

        i18n().ecouterChangement(ecouteurI18n);
    }

    @FXML
    private void switchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        if (clickedButton == currentActiveButton) {
            return;
        }

        String fxmlPath = tabViewsMap.get(clickedButton);
        if (fxmlPath != null) {
            mettreEnValeurBouton(clickedButton);
            viewRegistry.afficherVue(fxmlPath);
            mettreAJourTitrePage(clickedButton);
            BreadcrumbManager.getInstance().definirSousSection(clickedButton.getText());
        }
    }

    /**
     * Déclenché par le bouton "Retour à l'accueil" du bandeau de titre.
     */
    @FXML
    private void retournerAccueil(ActionEvent event) {
        if (onRetourAccueil != null) {
            onRetourAccueil.run();
        }
    }

    /**
     * Appelé par le shell juste après le chargement de cette vue, pour lui
     * fournir la logique de retour vers l'accueil des Paramètres.
     */
    public void setOnRetourAccueil(Runnable callback) {
        this.onRetourAccueil = callback;
    }

    private void rafraichirTextes() {
//        if (btnTableau != null) {
//            btnTableau.setText(i18n().t("settings.tab.users"));
//        }
//        if (btnTabRolePermissions != null) {
//            btnTabRolePermissions.setText(i18n().t("settings.tab.roles"));
//        }
//        if (btnTabAudits != null) {
//            btnTabAudits.setText(i18n().t("settings.tab.audit"));
//        }
//        if (lblCategorieCourante != null) {
//            lblCategorieCourante.setText(i18n().t("settings.categorie.systeme"));
//        }
    }

    private void mettreEnValeurBouton(Button boutonActif) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active-top-tab");
        }
        boutonActif.getStyleClass().add("active-top-tab");
        currentActiveButton = boutonActif;
    }

    public void fermer() {
        if (viewRegistry != null) {
            viewRegistry.toutReinitialiser();
        }
        i18n().arreterEcoute(ecouteurI18n);
    }

    private void mettreAJourTitrePage(Button boutonActif) {
//        if (lblPageCourante == null) return;

        String cle;
        if (boutonActif == btnTableau) {
            cle = "settings.page.title.users";
        } else if (boutonActif == btnListePersonnel) {
            cle = "settings.page.title.roles";}


        BreadcrumbManager.getInstance().definirSousSection(btnTableau.getText());
    }
}