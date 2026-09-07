package org.erpklassup.erpklassup;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.ViewRegistry;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class HelloController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private StackPane sidebarContainer;
    @FXML private Region resizeHandle;

    // Boutons Sidebar principaux
    @FXML private Button btnDashboard;
    @FXML private Button btnStudents;
    @FXML private Button btnPresences;
    @FXML private Button btnClasses;
    @FXML private Button btnNotes;
    @FXML private Button btnPaiement;
    @FXML private Button btnComptabilite;
    @FXML private Button btnPersonnel;
    @FXML private Button btnPaie;
    @FXML private Button btnMessages;
    @FXML private Button btnDocuments;
    @FXML private Button btnSettings;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnLogout;

    // Sous-menus
    @FXML private VBox sousMenuStudents;
    @FXML private VBox sousMenuPresences;
    @FXML private VBox sousMenuNotes;
    @FXML private VBox sousMenuPaiement;

    // Chevrons
    @FXML private FontIcon chevronStudents;
    @FXML private FontIcon chevronPresences;
    @FXML private FontIcon chevronNotes;
    @FXML private FontIcon chevronPaiement;

    // État des sous-menus
    private final Map<String, Boolean> sousMenusState = new HashMap<>();
    private final Preferences prefs = Preferences.userNodeForPackage(HelloController.class);

    private double lastMouseX;

    // Gestionnaire de navigation et de cache des vues
    private ViewRegistry viewRegistry;
    private Button currentActiveButton = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le registre de vues pour contentArea (limite à 5 vues actives en mémoire RAM)
        this.viewRegistry = new ViewRegistry(contentArea, 5);

        setupSidebarResize();
        loadSousMenusState();

        // Chargement initial de la page Utilisateurs
        loadPage("/org/erpklassup/erpklassup/view/user.fxml", btnUtilisateurs);
    }

    private void setupSidebarResize() {
        resizeHandle.setOnMouseEntered(e -> sidebarContainer.setCursor(Cursor.H_RESIZE));
        resizeHandle.setOnMouseExited(e -> sidebarContainer.setCursor(Cursor.DEFAULT));
        resizeHandle.setOnMousePressed(e -> lastMouseX = e.getScreenX());
        resizeHandle.setOnMouseDragged(e -> {
            double deltaX = e.getScreenX() - lastMouseX;
            double newWidth = sidebarContainer.getPrefWidth() + deltaX;
            if (newWidth >= sidebarContainer.getMinWidth() && newWidth <= sidebarContainer.getMaxWidth()) {
                sidebarContainer.setPrefWidth(newWidth);
            }
            lastMouseX = e.getScreenX();
        });
    }

    /**
     * Gestion du toggle des sous-menus avec animation douce
     */
    @FXML
    void toggleSousMenu(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String buttonId = sourceButton.getId();

        VBox sousMenu = null;
        FontIcon chevron = null;

        switch (buttonId) {
            case "btnStudents":
                sousMenu = sousMenuStudents;
                chevron = chevronStudents;
                break;
            case "btnPresences":
                sousMenu = sousMenuPresences;
                chevron = chevronPresences;
                break;
            case "btnNotes":
                sousMenu = sousMenuNotes;
                chevron = chevronPaiement;
                break;
            case "btnPaiement":
                sousMenu = sousMenuPaiement;
                chevron = chevronPaiement;
                break;
        }

        if (sousMenu != null && chevron != null) {
            boolean isVisible = sousMenu.isVisible();

            if (isVisible) {
                // Fermer avec animation
                animateSousMenu(sousMenu, chevron, false);
                sousMenusState.put(buttonId, false);
            } else {
                // Ouvrir avec animation
                animateSousMenu(sousMenu, chevron, true);
                sousMenusState.put(buttonId, true);
            }

            // Sauvegarder l'état
            saveSousMenusState();
        }
    }

    /**
     * Animation d'ouverture/fermeture des sous-menus
     */
    private void animateSousMenu(VBox sousMenu, FontIcon chevron, boolean show) {
        if (show) {
            sousMenu.setVisible(true);
            sousMenu.setManaged(true);

            rotateChevron(chevron, 0, 180);

            sousMenu.setOpacity(0);
            sousMenu.setTranslateY(-10);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sousMenu.opacityProperty(), 0, Interpolator.EASE_OUT),
                            new KeyValue(sousMenu.translateYProperty(), -10, Interpolator.EASE_OUT)
                    ),
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(sousMenu.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(sousMenu.translateYProperty(), 0, Interpolator.EASE_OUT)
                    )
            );
            timeline.play();
        } else {
            rotateChevron(chevron, 180, 0);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sousMenu.opacityProperty(), 1, Interpolator.EASE_IN),
                            new KeyValue(sousMenu.translateYProperty(), 0, Interpolator.EASE_IN)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(sousMenu.opacityProperty(), 0, Interpolator.EASE_IN),
                            new KeyValue(sousMenu.translateYProperty(), -10, Interpolator.EASE_IN)
                    )
            );

            timeline.setOnFinished(e -> {
                sousMenu.setVisible(false);
                sousMenu.setManaged(false);
                sousMenu.setTranslateY(0);
            });

            timeline.play();
        }
    }

    private void rotateChevron(FontIcon chevron, double fromAngle, double toAngle) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(chevron.rotateProperty(), fromAngle, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(chevron.rotateProperty(), toAngle, Interpolator.EASE_BOTH)
                )
        );
        timeline.play();
    }

    private void saveSousMenusState() {
        prefs.putBoolean("sousMenuStudents", sousMenusState.getOrDefault("btnStudents", false));
        prefs.putBoolean("sousMenuPresences", sousMenusState.getOrDefault("btnPresences", false));
        prefs.putBoolean("sousMenuNotes", sousMenusState.getOrDefault("btnNotes", false));
        prefs.putBoolean("sousMenuPaiement", sousMenusState.getOrDefault("btnPaiement", false));
    }

    private void loadSousMenusState() {
        boolean studentsOpen = prefs.getBoolean("sousMenuStudents", false);
        boolean presencesOpen = prefs.getBoolean("sousMenuPresences", false);
        boolean notesOpen = prefs.getBoolean("sousMenuNotes", false);
        boolean paiementOpen = prefs.getBoolean("sousMenuPaiement", false);

        sousMenusState.put("btnStudents", studentsOpen);
        sousMenusState.put("btnPresences", presencesOpen);
        sousMenusState.put("btnNotes", notesOpen);
        sousMenusState.put("btnPaiement", paiementOpen);

        if (studentsOpen) {
            sousMenuStudents.setVisible(true);
            sousMenuStudents.setManaged(true);
            chevronStudents.setRotate(180);
        }
        if (presencesOpen) {
            sousMenuPresences.setVisible(true);
            sousMenuPresences.setManaged(true);
            chevronPresences.setRotate(180);
        }
        if (notesOpen) {
            sousMenuNotes.setVisible(true);
            sousMenuNotes.setManaged(true);
            chevronNotes.setRotate(180);
        }
        if (paiementOpen) {
            sousMenuPaiement.setVisible(true);
            sousMenuPaiement.setManaged(true);
            chevronPaiement.setRotate(180);
        }
    }

    // ===== GESTIONNAIRES D'ÉVÉNEMENT (NAVIGATION) =====

    @FXML
    void handleDashboardHome(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/dashboard.fxml", btnDashboard);
    }

    @FXML
    void handleNouvelleInscription(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/nouvelle-inscription.fxml", btnStudents);
    }

    @FXML
    void handleListeEleves(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/liste-eleves.fxml", btnStudents);
    }

    @FXML
    void handleReinscriptions(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/reinscriptions.fxml", btnStudents);
    }

    @FXML
    void handleDossiersEleves(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/dossiers-eleves.fxml", btnStudents);
    }

    @FXML
    void handleFeuilleAppel(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/feuille-appel.fxml", btnPresences);
    }

    @FXML
    void handleRetards(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/retards-absences.fxml", btnPresences);
    }

    @FXML
    void handleStatsAssiduite(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/stats-assiduite.fxml", btnPresences);
    }

    @FXML
    void handleClassesMenu(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/classes.fxml", btnClasses);
    }

    @FXML
    void handleSaisieNotes(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/saisie-notes.fxml", btnNotes);
    }

    @FXML
    void handleBulletins(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/bulletins.fxml", btnNotes);
    }

    @FXML
    void handleExamens(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/examens.fxml", btnNotes);
    }

    @FXML
    void handleGuichet(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/guichet-paiements.fxml", btnPaiement);
    }

    @FXML
    void handleFraisInscription(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/frais-inscription.fxml", btnPaiement);
    }

    @FXML
    void handleRecouvrement(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/recouvrements.fxml", btnPaiement);
    }

    @FXML
    void handleFactures(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/factures.fxml", btnPaiement);
    }

    @FXML
    void handleComptabilite(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/comptabilite.fxml", btnComptabilite);
    }

    @FXML
    void handlePersonnel(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/personnel.fxml", btnPersonnel);
    }

    @FXML
    void handlePaie(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/paie.fxml", btnPaie);
    }

    @FXML
    void handleMessages(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/messages-parents.fxml", btnMessages);
    }

    @FXML
    void handleDocuments(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/documents-officiels.fxml", btnDocuments);
    }

    @FXML
    void handleSettings(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/parametres.fxml", btnSettings);
    }

    @FXML
    void handleUtilisateurs(ActionEvent event) {
        loadPage("/org/erpklassup/erpklassup/view/user.fxml", btnUtilisateurs);
    }

    /**
     * Gestionnaire de déconnexion utilisant AlertUtil et nettoyant le cache de navigation
     */
    @FXML
    void handleLogout(ActionEvent event) {
        Window currentWindow = btnLogout.getScene().getWindow();

        boolean confirme = AlertUtil.afficherConfirmation(
                "Déconnexion",
                "Voulez-vous vraiment vous déconnecter ?",
                currentWindow
        );

        if (confirme) {
            // Nettoyer tous les états et réinitialiser la mémoire
            if (viewRegistry != null) {
                viewRegistry.toutRéinitialiser();
            }

            try {
                returnVerLogin((Stage) currentWindow);
            } catch (IOException e) {
                System.err.println("Erreur lors de la redirection vers l'écran de login.");
                e.printStackTrace();
            }
        }
    }

    // ===== MÉTHODE UTILITAIRE POUR CHARGER LES PAGES =====

    /**
     * Charge une vue FXML dans le conteneur principal à l'aide de ViewRegistry.
     * Gère la sauvegarde de l'état de la vue sortante et la mise en évidence des boutons.
     */
    private void loadPage(String fxmlFile, Button activeButton) {
        // Demande au ViewRegistry de charger/restaurer la vue dans contentArea
        viewRegistry.afficherVue(fxmlFile);

        // Mise à jour de l'apparence des boutons du menu
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
        }

        if (activeButton != null) {
            if (!activeButton.getStyleClass().contains("active")) {
                activeButton.getStyleClass().add("active");
            }
            currentActiveButton = activeButton;
        }
    }

    private void returnVerLogin(Stage currentStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("view/login-view.fxml"));
        Scene mainScene = new Scene(loader.load(), 1000, 640);

        Stage loginStage = new Stage();
        try {
            loginStage.getIcons().add(new javafx.scene.image.Image(HelloApplication.class.getResourceAsStream("logo.png")));
        } catch (Exception ignored) {}

        loginStage.setTitle("KlassUp");
        loginStage.setScene(mainScene);
        loginStage.initStyle(StageStyle.UNDECORATED);
        loginStage.setWidth(1000);
        loginStage.setHeight(640);

        currentStage.close();
        loginStage.show();
    }
}