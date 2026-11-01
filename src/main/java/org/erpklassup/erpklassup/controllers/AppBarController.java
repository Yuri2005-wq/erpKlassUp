package org.erpklassup.erpklassup.controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.controllers.actionPage.UserProfileController;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
import org.erpklassup.erpklassup.util.BreadcrumbManager;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ModalUtil;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class AppBarController implements Initializable {

    /* ============================================================
       FXML — Profil / Système
       ============================================================ */
    @FXML private HBox      appBar;
    @FXML private HBox      userProfilBox;
    @FXML private Label     avatarLabelAppBar;
    @FXML private Label     nomUtilisateurAppBar;
    @FXML private Label     roleUtilisateurAppBar;
    @FXML private Label     lblDate;
    @FXML private Label     lblStatus;
    @FXML private ToggleButton btnLangFr;
    @FXML private ToggleButton btnLangEn;

    /* ============================================================
       FXML — Badge Année Active
       ============================================================ */
    @FXML private HBox   anneeActiveBadge;
    @FXML private Circle anneeActiveDot;
    @FXML private Label  lblAnneeActive;

    /* ============================================================
       FXML — Fil d'Ariane (3 niveaux)
       ============================================================ */
    @FXML private Label breadcrumbParent;
    @FXML private Label breadcrumbSeparator1;
    @FXML private Label breadcrumbSection;
    @FXML private Label breadcrumbSeparator2;
    @FXML private Label breadcrumbSousSection;

    /* ============================================================
       SERVICES & CONSTANTES
       ============================================================ */
    private final AnneeScolaireService anneeService = new AnneeScolaireService();

    private static final String CHEMIN_PROFIL =
            "/org/erpklassup/erpklassup/view/user-detail-selfpage.fxml";

    private final Runnable ecouteurI18n = this::rafraichirTextes;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerUtilisateurConnecte();
        chargerDateDuJour();
        configurerLangues();
        configurerFilDAriane();
        chargerAnneeActiveBadge();

        // Dans AppBarController.java
        SessionManager.getInstance().ecouterChangementAnnee(() -> Platform.runLater(this::chargerAnneeActiveBadge));

        if (userProfilBox != null) {
            userProfilBox.setOnMouseClicked(e -> ouvrirPopupProfil());
        }

        rafraichirTextes();
        I18nManager.getInstance().ecouterChangement(ecouteurI18n);
        demarrerRafraichissementDate();
    }

    /* ============================================================
       BADGE ANNÉE ACTIVE
       ============================================================ */

    /**
     * Charge et affiche l'année scolaire active dans l'AppBar.
     */
    /**
     * Charge et affiche l'année scolaire active dans l'AppBar de manière asynchrone.
     */
    private void chargerAnneeActiveBadge() {
        String idEcole = SessionManager.getInstance().getIdEcoleCourante();
        if (idEcole == null) {
            afficherAnneeBadge(null);
            return;
        }

        // Appel asynchrone pour ne pas bloquer l'I/O sur le Thread JavaFX Application
        anneeService.chargerCouranteAsync(idEcole,
                opt -> Platform.runLater(() -> afficherAnneeBadge(opt.orElse(null))),
                err -> Platform.runLater(() -> {
                    System.err.println("❌ Erreur chargement année active : " + err.getMessage());
                    afficherAnneeBadge(null);
                })
        );
    }

    /**
     * Affiche le badge avec l'année et la couleur selon son statut.
     */
    private void afficherAnneeBadge(AnneeScolaire annee) {
        if (lblAnneeActive == null || anneeActiveDot == null) return;

        // Retirer les anciennes classes de statut
        anneeActiveDot.getStyleClass().removeAll(
                "statut-preparation", "statut-cloturee",
                "statut-archivee", "statut-inactive");

        if (annee == null) {
            lblAnneeActive.setText("Aucune année");
            anneeActiveDot.getStyleClass().add("statut-inactive");
            return;
        }

        lblAnneeActive.setText(annee.getLibelle());

        if (annee.isEstCourante() && annee.getStatut() == AnneeScolaire.Statut.EN_COURS) {
            // Vert = actif et en cours (style par défaut)
        } else if (annee.getStatut() == AnneeScolaire.Statut.PREPARATION) {
            anneeActiveDot.getStyleClass().add("statut-preparation");
        } else if (annee.getStatut() == AnneeScolaire.Statut.CLOTUREE) {
            anneeActiveDot.getStyleClass().add("statut-cloturee");
        } else if (annee.getStatut() == AnneeScolaire.Statut.ARCHIVEE) {
            anneeActiveDot.getStyleClass().add("statut-archivee");
        } else {
            anneeActiveDot.getStyleClass().add("statut-inactive");
        }
    }

    /**
     * Handler clic sur le badge "Année active".
     * Ouvre un menu contextuel avec toutes les années disponibles.
     */
    @FXML
    private void handleChangerAnnee(MouseEvent event) {
        String idEcole = SessionManager.getInstance().getIdEcoleCourante();
        if (idEcole == null) return;

        anneeService.listerAnneesAsync(idEcole,
                annees -> Platform.runLater(() -> {
                    if (annees == null || annees.isEmpty()) {
                        ToastNotification.info(getStage(),
                                "Aucune année scolaire enregistrée.");
                        return;
                    }

                    ContextMenu menu = new ContextMenu();

                    for (AnneeScolaire a : annees) {
                        String label = a.getLibelle();

                        if (a.isEstCourante() && a.getStatut() == AnneeScolaire.Statut.EN_COURS) {
                            label += "  ●  active";
                        } else if (a.getStatut() != null) {
                            label += "  (" + a.getStatut().getLibelle() + ")";
                        }

                        MenuItem item = new MenuItem(label);
                        item.setOnAction(e -> {
                            if (a.isEstCourante()
                                    && a.getStatut() == AnneeScolaire.Statut.EN_COURS) {
                                ToastNotification.info(getStage(),
                                        "Cette année est déjà active.");
                            } else {
                                ToastNotification.info(getStage(),
                                        "Passage à « " + a.getLibelle()
                                                + " » (à implémenter).");
                            }
                        });
                        menu.getItems().add(item);
                    }

                    menu.show(anneeActiveBadge, Side.BOTTOM, 0, 4);
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(),
                                "Erreur : " + err.getMessage())));
    }

    /* ============================================================
       FIL D'ARIANE — 3 niveaux avec binding
       ============================================================ */
    private void configurerFilDAriane() {
        BreadcrumbManager bc = BreadcrumbManager.getInstance();

        if (breadcrumbParent != null) {
            breadcrumbParent.textProperty().bind(bc.parentProperty());
        }
        if (breadcrumbSection != null) {
            breadcrumbSection.textProperty().bind(bc.sectionProperty());
        }
        if (breadcrumbSousSection != null) {
            breadcrumbSousSection.textProperty().bind(bc.sousSectionProperty());
        }

        BooleanBinding hasParent      = Bindings.isNotEmpty(bc.parentProperty());
        BooleanBinding hasSousSection = Bindings.isNotEmpty(bc.sousSectionProperty());

        if (breadcrumbParent != null) {
            breadcrumbParent.visibleProperty().bind(hasParent);
            breadcrumbParent.managedProperty().bind(hasParent);
        }
        if (breadcrumbSeparator1 != null) {
            breadcrumbSeparator1.visibleProperty().bind(hasParent);
            breadcrumbSeparator1.managedProperty().bind(hasParent);
        }
        if (breadcrumbSousSection != null) {
            breadcrumbSousSection.visibleProperty().bind(hasSousSection);
            breadcrumbSousSection.managedProperty().bind(hasSousSection);
        }
        if (breadcrumbSeparator2 != null) {
            breadcrumbSeparator2.visibleProperty().bind(hasSousSection);
            breadcrumbSeparator2.managedProperty().bind(hasSousSection);
        }
    }

    /* ============================================================
       UTILISATEUR
       ============================================================ */
    @FXML
    public void handleOuvrirProfil() {
        ouvrirPopupProfil();
    }

    private void chargerUtilisateurConnecte() {
        Utilisateur user = SessionManager.getInstance().getUtilisateurCourant();
        if (user == null) {
            if (nomUtilisateurAppBar != null)
                nomUtilisateurAppBar.setText(I18nManager.getInstance().t("app.not_connected"));
            if (roleUtilisateurAppBar != null)
                roleUtilisateurAppBar.setText("—");
            if (avatarLabelAppBar != null)
                avatarLabelAppBar.setText("?");
            return;
        }

        if (nomUtilisateurAppBar != null)
            nomUtilisateurAppBar.setText(user.getNomComplet());
        if (avatarLabelAppBar != null)
            avatarLabelAppBar.setText(calculerInitiales(user));

        chargerRoleEnArrierePlan(user.getIdUtilisateur());
    }

    private void chargerRoleEnArrierePlan(String idUtilisateur) {
        Thread t = new Thread(() -> {
            try {
                List<Role> roles = new UtilisateurRoleDAO()
                        .findRolesByUtilisateur(idUtilisateur);
                String roleTexte = roles.isEmpty()
                        ? I18nManager.getInstance().t("app.no_role")
                        : roles.get(0).getNomRole();

                Platform.runLater(() -> {
                    if (roleUtilisateurAppBar != null)
                        roleUtilisateurAppBar.setText(roleTexte);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (roleUtilisateurAppBar != null)
                        roleUtilisateurAppBar.setText("—");
                });
            }
        }, "AppBar-Role-Loader");
        t.setDaemon(true);
        t.start();
    }

    private String calculerInitiales(Utilisateur user) {
        String nom = user.getNom() != null ? user.getNom() : "";
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String initiales = "";
        if (!prenom.isEmpty()) initiales += prenom.substring(0, 1);
        if (!nom.isEmpty()) initiales += nom.substring(0, 1);
        return initiales.isEmpty() ? "?" : initiales.toUpperCase();
    }

    private void ouvrirPopupProfil() {
        UserProfileController ctrl = ModalUtil.ouvrirPopupSous(
                getClass(),
                CHEMIN_PROFIL,
                "",
                userProfilBox.getScene().getWindow(),
                userProfilBox,
                480,
                640
        );
        if (ctrl == null) {
            System.err.println("❌ Impossible d'ouvrir la popup profil");
        }
    }

    /* ============================================================
       DATE
       ============================================================ */
    private void chargerDateDuJour() {
        if (lblDate == null) return;

        LocalDate aujourdHui = LocalDate.now();
        Locale locale = I18nManager.getInstance().getLocale();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd MMM yyyy", locale);
        String dateFormatee = aujourdHui.format(formatter);

        if (!dateFormatee.isEmpty()) {
            dateFormatee = dateFormatee.substring(0, 1).toUpperCase()
                    + dateFormatee.substring(1);
        }

        lblDate.setText(dateFormatee);
    }

    private void demarrerRafraichissementDate() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.hours(1),
                        e -> chargerDateDuJour()
                )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    /* ============================================================
       LANGUES
       ============================================================ */
    private void configurerLangues() {
        if (btnLangFr == null || btnLangEn == null) return;

        boolean isFr = I18nManager.getInstance().getLocale().equals(Locale.FRENCH);
        btnLangFr.setSelected(isFr);
        btnLangEn.setSelected(!isFr);

        btnLangFr.setOnAction(e -> {
            I18nManager.getInstance().setLocale(Locale.FRENCH);
            btnLangFr.setSelected(true);
            btnLangEn.setSelected(false);
        });

        btnLangEn.setOnAction(e -> {
            I18nManager.getInstance().setLocale(Locale.ENGLISH);
            btnLangFr.setSelected(false);
            btnLangEn.setSelected(true);
        });
    }

    /* ============================================================
       i18n
       ============================================================ */
    private void rafraichirTextes() {
        chargerDateDuJour();

        boolean isFr = I18nManager.getInstance().getLocale().equals(Locale.FRENCH);
        if (btnLangFr != null) btnLangFr.setSelected(isFr);
        if (btnLangEn != null) btnLangEn.setSelected(!isFr);

        I18nManager i18n = I18nManager.getInstance();
        if (lblStatus != null) lblStatus.setText(i18n.t("app.online"));
    }

    /* ============================================================
       DISPOSE
       ============================================================ */
    public void dispose() {
        I18nManager.getInstance().arreterEcoute(ecouteurI18n);
    }

    /* ============================================================
       UTILITAIRES
       ============================================================ */
    private Stage getStage() {
        if (appBar == null || appBar.getScene() == null) return null;
        return (Stage) appBar.getScene().getWindow();
    }
}