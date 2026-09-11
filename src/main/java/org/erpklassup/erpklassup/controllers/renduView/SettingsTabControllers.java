package org.erpklassup.erpklassup.controllers.renduView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.erpklassup.erpklassup.util.ViewRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur principal pour la gestion de l'onglet de configuration (Settings).
 * Il délègue la gestion de la mémoire, de l'état et de l'affichage des sous-vues au {@link ViewRegistry}.
 */
public class SettingsTabControllers {

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

    /**
     * Initialisation du contrôleur après le chargement du fichier FXML.
     */
    @FXML
    public void initialize() {
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
    }

    /**
     * Gestionnaire d'événement déclenché lors du clic sur un onglet de la barre supérieure.
     *
     * @param event L'événement d'action généré par le clic sur le bouton
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
            // Mettre à jour l'apparence visuelle des boutons
            mettreEnValeurBouton(clickedButton);

            // Demander au ViewRegistry d'afficher la vue (gère la sauvegarde de l'ancienne et le chargement de la nouvelle)
            viewRegistry.afficherVue(fxmlPath);
        }
    }

    /**
     * Applique la classe CSS active au bouton sélectionné et retire le style de l'ancien bouton.
     *
     * @param boutonActif Le nouveau bouton à mettre en avant
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
    }
}