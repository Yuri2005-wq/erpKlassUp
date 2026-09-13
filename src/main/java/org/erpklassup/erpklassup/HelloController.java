package org.erpklassup.erpklassup;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.ToastNotification;
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
    private final AuditService auditService = new AuditService();


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

    /**
     * Gestionnaire de déconnexion professionnel
     * 1. Confirme la déconnexion
     * 2. Nettoie le ViewRegistry (cache, contrôleurs, états)
     * 3. Termine la session (SessionManager)
     * 4. Change de scène vers le login (même Stage)
     * 5. Affiche un toast de confirmation
     */
    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        Window currentWindow = btnLogout.getScene().getWindow();

        boolean confirme = AlertUtil.afficherConfirmation(
                "Déconnexion",
                "Voulez-vous vraiment vous déconnecter ?",
                currentWindow
        );

        if (confirme) {
            SessionManager sessionManager = SessionManager.getInstance();

            // 1. Récupération sécurisée du nom avant de détruire la session
            String nomUtilisateur = "Inconnu";
            if (sessionManager.getUtilisateurCourant() != null) {
                nomUtilisateur = sessionManager.getUtilisateurCourant().getNomComplet();
            }

            // 2. Traçage d'audit AVANT de terminer la session (pour avoir le contexte)
            auditService.tracerActionAsync(
                    "HABILITATION",
                    "DECONNEXION_UTILISATEUR",
                    "Utilisateur " + nomUtilisateur + " Déconnecté"
            );

            // 3. Réinitialisation des vues et fermeture de session
            if (viewRegistry != null) {
                viewRegistry.toutReinitialiser();
            }
            sessionManager.terminerSession();

            // 4. Redirection vers l'écran de login
            retournerAuLogin((Stage) currentWindow);

            // 5. Notification Toast sur la nouvelle scène du Stage
            Stage stage = (Stage) currentWindow;
            PauseTransition pause = new PauseTransition(Duration.millis(600));
            pause.setOnFinished(e -> {
                if (stage.isShowing()) {
                    ToastNotification.info(
                            stage,
                            "Vous avez été déconnecté avec succès"
                    );
                }
            });
            pause.play();
        }
    }

    /**
     * Change la scène du Stage courant vers l'écran de login
     * (SANS créer un nouveau Stage)
     */
    private void retournerAuLogin(Stage currentStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("view/login-view.fxml"));
        Parent loginRoot = loader.load();

        Scene loginScene = new Scene(loginRoot, 1000, 640);

        Scene sceneActuelle = currentStage.getScene();
        Parent rootActuel = sceneActuelle != null ? sceneActuelle.getRoot() : null;

        Runnable appliquerNouvelleScene = () -> {
            // ✅ sortir du mode maximisé AVANT de fixer la taille,
            // sinon setWidth/setHeight sont silencieusement ignorés par Windows
            if (currentStage.isMaximized()) {
                currentStage.setMaximized(false);
            }

            currentStage.setScene(loginScene);
            currentStage.setWidth(1000);
            currentStage.setHeight(640);
            currentStage.setResizable(false);
            currentStage.setTitle("KlassUp - Connexion");
            currentStage.centerOnScreen();

            WindowsTitleBar.setTitleBarColor(currentStage, "#16213A");

            loginRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), loginRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        };

        if (rootActuel != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), rootActuel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> appliquerNouvelleScene.run());
            fadeOut.play();
        } else {
            appliquerNouvelleScene.run();
        }
    }
    /**
     * Transition de déconnexion avec fondu
     * 1. Fade OUT de la scène courante
     * 2. Chargement du login
     * 3. Fade IN de la nouvelle scène
     */


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
}