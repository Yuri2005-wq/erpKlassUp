package org.erpklassup.erpklassup.controllers.renduView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.erpklassup.erpklassup.util.ViewRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur principal pour la page de détail utilisateur.
 * Utilise le pattern TopBar + ViewRegistry comme SettingsTabControllers.
 */
public class UserDetailTabController {

    @FXML private StackPane tabContentContainer;
    @FXML private Button btnTabInformations;
    @FXML private Button btnTabRoles;
    @FXML private Button btnTabPermissions;
    @FXML private Button btnTabSecurite;
    @FXML private Button btnTabAudit;

    private final Map<Button, String> tabViewsMap = new HashMap<>();
    private Button currentActiveButton = null;
    private ViewRegistry viewRegistry;

    @FXML
    public void initialize() {
        this.viewRegistry = new ViewRegistry(tabContentContainer, 4);

        // Enregistrer les routes FXML
        tabViewsMap.put(btnTabInformations, "/org/erpklassup/erpklassup/view/UserInfoTab.fxml");
        tabViewsMap.put(btnTabRoles, "/org/erpklassup/erpklassup/view/UserRolesTab.fxml");
        tabViewsMap.put(btnTabPermissions, "/org/erpklassup/erpklassup/view/UserPermissionsTab.fxml");
        tabViewsMap.put(btnTabSecurite, "/org/erpklassup/erpklassup/view/UserSecurityTab.fxml");
        tabViewsMap.put(btnTabAudit, "/org/erpklassup/erpklassup/view/UserAuditTab.fxml");

        // Afficher le premier onglet par défaut
        if (btnTabInformations != null) {
            mettreEnValeurBouton(btnTabInformations);
            viewRegistry.afficherVue(tabViewsMap.get(btnTabInformations));
        }
    }

    @FXML
    private void switchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();

        if (clickedButton == currentActiveButton) return;

        String fxmlPath = tabViewsMap.get(clickedButton);

        if (fxmlPath != null) {
            mettreEnValeurBouton(clickedButton);
            viewRegistry.afficherVue(fxmlPath);
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
    }
}