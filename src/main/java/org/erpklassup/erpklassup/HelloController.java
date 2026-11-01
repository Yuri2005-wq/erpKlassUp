package org.erpklassup.erpklassup;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.erpklassup.erpklassup.dao.SessionUtilisateurDAO;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;
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
    @FXML private Region    resizeHandle;

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

    @FXML private VBox sousMenuStudents;
    @FXML private VBox sousMenuPresences;
    @FXML private VBox sousMenuNotes;
    @FXML private VBox sousMenuPaiement;
    @FXML private VBox sousMenuSettings;

    @FXML private FontIcon chevronStudents;
    @FXML private FontIcon chevronPresences;
    @FXML private FontIcon chevronNotes;
    @FXML private FontIcon chevronPaiement;
    @FXML private FontIcon chevronSettings;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final AuditService auditService = new AuditService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private final Map<String, Boolean> sousMenusState = new HashMap<>();
    private final Preferences prefs = Preferences.userNodeForPackage(HelloController.class);

    private double lastMouseX;

    private ViewRegistry viewRegistry;
    private Button currentActiveButton = null;

    /** Chemin de la page courante (pour rechargement i18n). */
    private String cheminPageCourante = "/org/erpklassup/erpklassup/view/module2/parametreAcademique.fxml";

    /** Écouteur i18n stocké (pour pouvoir se désabonner). */
    private final Runnable ecouteurI18n = this::onChangementLangue;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.viewRegistry = new ViewRegistry(contentArea, 5);

        AppExecutor.get().submit(() ->
                new SessionUtilisateurDAO().nettoyerSessionsObsoletes(30)
        );

        Platform.runLater(() -> {
            if (contentArea.getScene() != null && contentArea.getScene().getWindow() != null) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                SessionManager.getInstance().initialiserContexteGraphique(stage, this.viewRegistry);
                System.out.println("✅ [HelloController] ViewRegistry enregistré dans SessionManager");
            } else {
                System.err.println("⚠️ [HelloController] Impossible d'enregistrer le ViewRegistry : scène non prête");
            }

            installerMoniteurActivite(contentArea.getScene());
            SessionManager.getInstance().setOnInactiviteDetectee(this::gererDeconnexionInactivite);
        });

        setupSidebarResize();
        loadSousMenusState();

        I18nManager.getInstance().ecouterChangement(ecouteurI18n);

        loadPage(cheminPageCourante, btnSettings);
    }

    /* ============================================================
       CHANGEMENT DE LANGUE
       ============================================================ */
    private void onChangementLangue() {
        System.out.println("🌍 [HelloController] Changement de langue → rechargement...");

        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) contentArea.getScene().getWindow();

                String pageASauvegarder = (viewRegistry != null)
                        ? viewRegistry.getCheminVueCourante()
                        : cheminPageCourante;

                FXMLLoader loader = I18nManager.getInstance().creerLoader(
                        getClass(),
                        "/org/erpklassup/erpklassup/hello-view.fxml"
                );
                Parent nouvelleRacine = loader.load();

                stage.getScene().setRoot(nouvelleRacine);

                HelloController nouveauCtrl = loader.getController();
                if (nouveauCtrl != null && pageASauvegarder != null) {
                    Platform.runLater(() -> nouveauCtrl.rechargerPage(pageASauvegarder));
                }

                System.out.println("✅ [HelloController] Rechargement i18n terminé");

            } catch (Exception e) {
                System.err.println("❌ Erreur rechargement i18n : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void rechargerPage(String fxmlPath) {
        if (fxmlPath == null || viewRegistry == null) return;
        viewRegistry.invaliderCache(fxmlPath);
        viewRegistry.afficherVue(fxmlPath);
        cheminPageCourante = fxmlPath;
    }

    public void dispose() {
        I18nManager.getInstance().arreterEcoute(ecouteurI18n);
    }

    /* ============================================================
       MONITEUR D'ACTIVITÉ
       ============================================================ */
    private void installerMoniteurActivite(Scene scene) {
        if (scene == null) return;

        EventHandler<MouseEvent> souris  = e -> SessionManager.getInstance().enregistrerActivite();
        EventHandler<KeyEvent>   clavier = e -> SessionManager.getInstance().enregistrerActivite();

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED,  souris);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED,    souris);
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED,  souris);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED,  souris);
        scene.addEventFilter(KeyEvent.KEY_PRESSED,      clavier);
        scene.addEventFilter(KeyEvent.KEY_TYPED,        clavier);

        System.out.println("✅ [HelloController] Moniteur d'activité installé");
    }

    /* ============================================================
       DÉCONNEXION POUR INACTIVITÉ
       ============================================================ */
    private void gererDeconnexionInactivite() {
        SessionManager sessionManager = SessionManager.getInstance();

        String nomUtilisateur = "Inconnu";
        if (sessionManager.getUtilisateurCourant() != null) {
            nomUtilisateur = sessionManager.getUtilisateurCourant().getNomComplet();
        }

        long minutesInactif = sessionManager.getMinutesInactivite();

        auditService.tracerActionAsync(
                "AUTHENTIFICATION",
                "DECONNEXION_INACTIVITE",
                "Déconnexion automatique pour inactivité (" + minutesInactif
                        + " min) — utilisateur : " + nomUtilisateur
        );

        if (viewRegistry != null) {
            viewRegistry.toutReinitialiser();
        }

        sessionManager.terminerSession();

        Stage stage = sessionManager.getStagePrincipal();
        if (stage == null && contentArea.getScene() != null) {
            stage = (Stage) contentArea.getScene().getWindow();
        }

        final Stage stageFinal = stage;

        if (stageFinal != null) {
            try {
                NavigationUtil.retournerAuLogin(stageFinal);

                PauseTransition pause = new PauseTransition(Duration.millis(600));
                pause.setOnFinished(e -> {
                    if (stageFinal.isShowing()) {
                        ToastNotification.avertissement(
                                stageFinal,
                                I18nManager.getInstance().t("session.inactivity.logout", minutesInactif)
                        );
                    }
                });
                pause.play();

            } catch (IOException e) {
                System.err.println("❌ Erreur redirection login (inactivité) : " + e.getMessage());
            }
        }
    }

    /* ============================================================
       SIDEBAR RESIZE
       ============================================================ */
    private void setupSidebarResize() {
        resizeHandle.setOnMouseEntered(e -> sidebarContainer.setCursor(Cursor.H_RESIZE));
        resizeHandle.setOnMouseExited(e  -> sidebarContainer.setCursor(Cursor.DEFAULT));
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

    /* ============================================================
       SOUS-MENUS
       ============================================================ */
    @FXML
    void toggleSousMenu(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String buttonId = sourceButton.getId();

        VBox sousMenu = null;
        FontIcon chevron = null;

        switch (buttonId) {
            case "btnStudents"  -> { sousMenu = sousMenuStudents;  chevron = chevronStudents; }
            case "btnPresences" -> { sousMenu = sousMenuPresences; chevron = chevronPresences; }
            case "btnNotes"     -> { sousMenu = sousMenuNotes;     chevron = chevronNotes; }
            case "btnPaiement"  -> { sousMenu = sousMenuPaiement;  chevron = chevronPaiement; }
            case "btnSettings"  -> { sousMenu = sousMenuSettings;  chevron = chevronSettings; }
        }

        if (sousMenu != null && chevron != null) {
            boolean isVisible = sousMenu.isVisible();

            animateSousMenu(sousMenu, chevron, !isVisible);
            sousMenusState.put(buttonId, !isVisible);
            saveSousMenusState();
        }
    }

    private void animateSousMenu(VBox sousMenu, FontIcon chevron, boolean show) {
        if (show) {
            sousMenu.setVisible(true);
            sousMenu.setManaged(true);
            rotateChevron(chevron, 0, 180);
            sousMenu.setOpacity(0);
            sousMenu.setTranslateY(-10);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sousMenu.opacityProperty(),    0,   Interpolator.EASE_OUT),
                            new KeyValue(sousMenu.translateYProperty(), -10, Interpolator.EASE_OUT)
                    ),
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(sousMenu.opacityProperty(),    1,   Interpolator.EASE_OUT),
                            new KeyValue(sousMenu.translateYProperty(), 0,   Interpolator.EASE_OUT)
                    )
            );
            timeline.play();
        } else {
            rotateChevron(chevron, 180, 0);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(sousMenu.opacityProperty(),    1,   Interpolator.EASE_IN),
                            new KeyValue(sousMenu.translateYProperty(), 0,   Interpolator.EASE_IN)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(sousMenu.opacityProperty(),    0,   Interpolator.EASE_IN),
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
        if (chevron == null) return;
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
        prefs.putBoolean("sousMenuStudents",  sousMenusState.getOrDefault("btnStudents",  false));
        prefs.putBoolean("sousMenuPresences", sousMenusState.getOrDefault("btnPresences", false));
        prefs.putBoolean("sousMenuNotes",     sousMenusState.getOrDefault("btnNotes",     false));
        prefs.putBoolean("sousMenuPaiement",  sousMenusState.getOrDefault("btnPaiement",  false));
        prefs.putBoolean("sousMenuSettings",  sousMenusState.getOrDefault("btnSettings",  false));
    }

    private void loadSousMenusState() {
        boolean studentsOpen  = prefs.getBoolean("sousMenuStudents",  false);
        boolean presencesOpen = prefs.getBoolean("sousMenuPresences", false);
        boolean notesOpen     = prefs.getBoolean("sousMenuNotes",     false);
        boolean paiementOpen  = prefs.getBoolean("sousMenuPaiement",  false);
        boolean settingsOpen  = prefs.getBoolean("sousMenuSettings",  false);   // ✅ Corrigé

        sousMenusState.put("btnStudents",  studentsOpen);
        sousMenusState.put("btnPresences", presencesOpen);
        sousMenusState.put("btnNotes",     notesOpen);
        sousMenusState.put("btnPaiement",  paiementOpen);
        sousMenusState.put("btnSettings",  settingsOpen);

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
        if (settingsOpen) {
            sousMenuSettings.setVisible(true);
            sousMenuSettings.setManaged(true);
            chevronSettings.setRotate(180);   // ✅ Corrigé : chevron, pas VBox
        }
    }

    /* ============================================================
       NAVIGATION
       ============================================================ */
    @FXML void handleDashboardHome(ActionEvent e)       { loadPage("/org/erpklassup/erpklassup/view/Dashboard.fxml",           btnDashboard); }
    @FXML void handleNouvelleInscription(ActionEvent e) { loadPage("/org/erpklassup/erpklassup/view/nouvelle-inscription.fxml", btnStudents); }
    @FXML void handleListeEleves(ActionEvent e)         { loadPage("/org/erpklassup/erpklassup/view/AccueilParametre.fxml",    btnStudents); }
    @FXML void handleReinscriptions(ActionEvent e)      { loadPage("/org/erpklassup/erpklassup/view/reinscriptions.fxml",      btnStudents); }
    @FXML void handleDossiersEleves(ActionEvent e)      { loadPage("/org/erpklassup/erpklassup/view/dossiers-eleves.fxml",     btnStudents); }
    @FXML void handleFeuilleAppel(ActionEvent e)        { loadPage("/org/erpklassup/erpklassup/view/feuille-appel.fxml",       btnPresences); }
    @FXML void handleRetards(ActionEvent e)             { loadPage("/org/erpklassup/erpklassup/view/retards-absences.fxml",    btnPresences); }
    @FXML void handleStatsAssiduite(ActionEvent e)      { loadPage("/org/erpklassup/erpklassup/view/stats-assiduite.fxml",     btnPresences); }
    @FXML void handleClassesMenu(ActionEvent e)         { loadPage("/org/erpklassup/erpklassup/view/classes.fxml",             btnClasses); }
    @FXML void handleSaisieNotes(ActionEvent e)         { loadPage("/org/erpklassup/erpklassup/view/saisie-notes.fxml",        btnNotes); }
    @FXML void handleBulletins(ActionEvent e)           { loadPage("/org/erpklassup/erpklassup/view/bulletins.fxml",           btnNotes); }
    @FXML void handleExamens(ActionEvent e)             { loadPage("/org/erpklassup/erpklassup/view/examens.fxml",             btnNotes); }
    @FXML void handleGuichet(ActionEvent e)             { loadPage("/org/erpklassup/erpklassup/view/guichet-paiements.fxml",    btnPaiement); }
    @FXML void handleFraisInscription(ActionEvent e)    { loadPage("/org/erpklassup/erpklassup/view/frais-inscription.fxml",   btnPaiement); }
    @FXML void handleRecouvrement(ActionEvent e)        { loadPage("/org/erpklassup/erpklassup/view/recouvrements.fxml",       btnPaiement); }
    @FXML void handleFactures(ActionEvent e)            { loadPage("/org/erpklassup/erpklassup/view/factures.fxml",            btnPaiement); }
    @FXML void handleComptabilite(ActionEvent e)        { loadPage("/org/erpklassup/erpklassup/view/comptabilite.fxml",        btnComptabilite); }
    @FXML void handlePersonnel(ActionEvent e)           { loadPage("/org/erpklassup/erpklassup/view/GRH/TabRH.fxml",           btnPersonnel, "Ressources Humaines"); }
    @FXML void handlePaie(ActionEvent e)                { loadPage("/org/erpklassup/erpklassup/view/paie.fxml",                btnPaie, "Ressources Humaines"); }
    @FXML void handleMessages(ActionEvent e)            { loadPage("/org/erpklassup/erpklassup/view/GRH/tt.fxml",    btnMessages); }
    @FXML void handleDocuments(ActionEvent e)           { loadPage("/org/erpklassup/erpklassup/view/documents-officiels.fxml", btnDocuments); }

    @FXML void handleSettings(ActionEvent e) {
        loadPage("/org/erpklassup/erpklassup/view/module2/parametreAcademique.fxml",
                btnSettings, "Paramètres");
    }

    @FXML void handleEcole(ActionEvent e) {
        loadPage("/org/erpklassup/erpklassup/view/ecoleView.fxml",
                btnSettings, "Paramètres");
    }

    @FXML void handleUtilisateurs(ActionEvent e) {
        loadPage("/org/erpklassup/erpklassup/view/user.fxml",
                btnUtilisateurs, "Paramètres");
    }

    /* ============================================================
       DÉCONNEXION
       ============================================================ */
    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        Window currentWindow = btnLogout.getScene().getWindow();

        boolean confirme = AlertUtil.afficherConfirmation(
                I18nManager.getInstance().t("logout.title"),
                I18nManager.getInstance().t("logout.confirm"),
                currentWindow
        );

        if (confirme) {
            SessionManager sessionManager = SessionManager.getInstance();

            String nomUtilisateur = "Inconnu";
            if (sessionManager.getUtilisateurCourant() != null) {
                nomUtilisateur = sessionManager.getUtilisateurCourant().getNomComplet();
            }

            auditService.tracerActionAsync(
                    "HABILITATION",
                    "DECONNEXION_UTILISATEUR",
                    "Utilisateur " + nomUtilisateur + " Déconnecté"
            );

            if (viewRegistry != null) {
                viewRegistry.toutReinitialiser();
            }
            sessionManager.terminerSession();

            dispose();

            retournerAuLogin((Stage) currentWindow);

            Stage stage = (Stage) currentWindow;
            PauseTransition pause = new PauseTransition(Duration.millis(600));
            pause.setOnFinished(e -> {
                if (stage.isShowing()) {
                    ToastNotification.info(stage,
                            I18nManager.getInstance().t("logout.success"));
                }
            });
            pause.play();
        }
    }

    public void retournerAuLogin(Stage currentStage) throws IOException {
        NavigationUtil.retournerAuLogin(currentStage);
    }

    /* ============================================================
       NAVIGATION — loadPage
       ============================================================ */

    /** Surcharge sans parent (pour les sections racines). */
    private void loadPage(String fxmlFile, Button activeButton) {
        loadPage(fxmlFile, activeButton, "");
    }

    /** Surcharge principale : avec parent explicite. */
    private void loadPage(String fxmlFile, Button activeButton, String parentLabel) {
        this.cheminPageCourante = fxmlFile;

        if (activeButton != null) {
            BreadcrumbManager.getInstance().definirSection(parentLabel, activeButton.getText());
        }

        viewRegistry.afficherVue(fxmlFile);

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