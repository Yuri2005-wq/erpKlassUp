package org.erpklassup.erpklassup.controllers.renduView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ViewRegistry;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Contrôleur principal pour la gestion de l'onglet de configuration (Settings).
 * Il délègue la gestion de la mémoire, de l'état et de l'affichage des sous-vues au {@link ViewRegistry}.
 */
public class SettingsTabControllers implements Initializable {

    // Conteneur JavaFX où les sous-vues FXML seront injectées
    @FXML private StackPane tabContentContainer;

    // Boutons de navigation (TopBar)
    @FXML private Button btnTabUtilisateur;
    @FXML private Button btnTabRolePermissions;
    @FXML private Button btnTabAudits;

    // Mappage associant chaque bouton FXML au chemin de sa vue FXML correspondante
    private final Map<Button, String> tabViewsMap = new HashMap<>();

    // Référence vers le bouton actuellement sélectionné pour la mise en valeur CSS
    private Button currentActiveButton = null;

    // Gestionnaire centralisé des vues (gère le cache LRU, le nettoyage et la sauvegarde d'état)
    private ViewRegistry viewRegistry;

    // ✅ ÉCOUTEUR i18n
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    // ✅ Raccourci i18n
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    /**
     * Initialisation du contrôleur après le chargement du fichier FXML.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Instanciation du ViewRegistry restreint à 3 vues max en mémoire cache RAM
        this.viewRegistry = new ViewRegistry(tabContentContainer, 3);

        // 2. Enregistrement des routes FXML associées aux boutons
        tabViewsMap.put(btnTabUtilisateur, "/org/erpklassup/erpklassup/view/UtilisateursTab.fxml");
        tabViewsMap.put(btnTabRolePermissions, "/org/erpklassup/erpklassup/view/RolesPermissionsTab.fxml");
        tabViewsMap.put(btnTabAudits, "/org/erpklassup/erpklassup/view/JournalAuditTab.fxml");

        // 3. Activation et affichage de la première vue par défaut (Utilisateurs)
        if (btnTabUtilisateur != null) {
            mettreEnValeurBouton(btnTabUtilisateur);
            viewRegistry.afficherVue(tabViewsMap.get(btnTabUtilisateur));
        }

        // ✅ S'abonner aux changements de langue
        i18n().ecouterChangement(ecouteurI18n);
    }

    /**
     * Gestionnaire d'événement déclenché lors du clic sur un onglet de la barre supérieure.
     */
    @FXML
    private void switchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();

        // Évite tout rechargement inutile si l'utilisateur clique sur l'onglet déjà actif
        if (clickedButton == currentActiveButton) {
            return;
        }

        // Récupération du chemin FXML associé au bouton cliqué
        String fxmlPath = tabViewsMap.get(clickedButton);

        if (fxmlPath != null) {
            mettreEnValeurBouton(clickedButton);
            viewRegistry.afficherVue(fxmlPath);
        }
    }

    /**
     * ✅ Rafraîchit les textes des boutons après changement de langue.
     */
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
    }

    /**
     * Applique la classe CSS active au bouton sélectionné et retire le style de l'ancien bouton.
     */
    private void mettreEnValeurBouton(Button boutonActif) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active-top-tab");
        }

        boutonActif.getStyleClass().add("active-top-tab");
        currentActiveButton = boutonActif;
    }

    /**
     * Nettoie les ressources en mémoire lors de la fermeture complète du composant parent.
     */
    public void fermer() {
        if (viewRegistry != null) {
            viewRegistry.toutReinitialiser();
        }
        i18n().arreterEcoute(ecouteurI18n);
    }
}