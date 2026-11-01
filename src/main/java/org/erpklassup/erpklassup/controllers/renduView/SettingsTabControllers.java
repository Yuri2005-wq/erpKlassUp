package org.erpklassup.erpklassup.controllers.renduView;

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
public class SettingsTabControllers implements Initializable {

    @FXML private StackPane tabContentContainer;
    @FXML private Label lblPageCourante;
    @FXML private Label lblCategorieCourante;
    @FXML private Button btnTabUtilisateur;
    @FXML private Button btnTabRolePermissions;
    @FXML private Button btnTabAudits;
    @FXML private Button btnRetourAccueil;
    @FXML private FontIcon iconTitleShield;

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

        tabViewsMap.put(btnTabUtilisateur, "/org/erpklassup/erpklassup/view/UtilisateursTab.fxml");
        tabViewsMap.put(btnTabRolePermissions, "/org/erpklassup/erpklassup/view/RolesPermissionsTab.fxml");
        tabViewsMap.put(btnTabAudits, "/org/erpklassup/erpklassup/view/JournalAuditTab.fxml");

        if (btnTabUtilisateur != null) {
            mettreEnValeurBouton(btnTabUtilisateur);
            viewRegistry.afficherVue(tabViewsMap.get(btnTabUtilisateur));
            mettreAJourTitrePage(btnTabUtilisateur);
            BreadcrumbManager.getInstance().definirSousSection(btnTabUtilisateur.getText());

        }

        if (lblCategorieCourante != null) {
            lblCategorieCourante.setText(i18n().t("settings.categorie.systeme"));
        }

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
        if (btnTabUtilisateur != null) {
            btnTabUtilisateur.setText(i18n().t("settings.tab.users"));
        }
        if (btnTabRolePermissions != null) {
            btnTabRolePermissions.setText(i18n().t("settings.tab.roles"));
        }
        if (btnTabAudits != null) {
            btnTabAudits.setText(i18n().t("settings.tab.audit"));
        }
        if (lblCategorieCourante != null) {
            lblCategorieCourante.setText(i18n().t("settings.categorie.systeme"));
        }
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
        if (boutonActif == btnTabUtilisateur) {
            cle = "settings.page.title.users";
        } else if (boutonActif == btnTabRolePermissions) {
            cle = "settings.page.title.roles";
        } else if (boutonActif == btnTabAudits) {
            cle = "settings.page.title.audit";
        } else {
            cle = "settings.page.title";
        }

        BreadcrumbManager.getInstance().definirSousSection(btnTabUtilisateur.getText());
    }
}