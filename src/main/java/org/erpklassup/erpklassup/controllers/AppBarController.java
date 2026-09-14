package org.erpklassup.erpklassup.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import org.erpklassup.erpklassup.controllers.actionPage.UserProfileController;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ModalUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class AppBarController implements Initializable {

    // ===== FXML =====
    @FXML private HBox userProfilBox;
    @FXML private Label avatarLabelAppBar;
    @FXML private Label nomUtilisateurAppBar;
    @FXML private Label roleUtilisateurAppBar;
    @FXML private Label lblDate;
    @FXML private Label lblStatus;
    @FXML private ToggleButton btnLangFr;
    @FXML private ToggleButton btnLangEn;

    private static final String CHEMIN_PROFIL =
            "/org/erpklassup/erpklassup/view/user-detail-selfpage.fxml";

    // Écouteur i18n stocké
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Charger l'utilisateur + date
        chargerUtilisateurConnecte();
        chargerDateDuJour();

        // 2. Configurer les boutons de langue
        configurerLangues();

        // 3. Clic sur le bloc profil → popup
        if (userProfilBox != null) {
            userProfilBox.setOnMouseClicked(e -> ouvrirPopupProfil());
        }

        // 4. Appliquer les traductions initiales
        rafraichirTextes();

        // 5. S'abonner aux changements de langue
        I18nManager.getInstance().ecouterChangement(ecouteurI18n);

        // 6. Rafraîchir la date toutes les heures
        demarrerRafraichissementDate();
    }

    // ==========================================
    // UTILISATEUR
    // ==========================================

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

    // ==========================================
    // DATE
    // ==========================================

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

    // ==========================================
    // LANGUES
    // ==========================================

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

    // ==========================================
    // ÉCOUTEUR i18n
    // ==========================================

    /**
     * ✅ Appelée automatiquement quand la langue change.
     * Rafraîchit les textes traduits de l'app-bar.
     */
    private void rafraichirTextes() {
        // La date dépend de la locale
        chargerDateDuJour();

        // Synchronise les toggles
        boolean isFr = I18nManager.getInstance().getLocale().equals(Locale.FRENCH);
        if (btnLangFr != null) btnLangFr.setSelected(isFr);
        if (btnLangEn != null) btnLangEn.setSelected(!isFr);

        // Met à jour les labels traduits
        I18nManager i18n = I18nManager.getInstance();
        if (lblStatus != null) lblStatus.setText(i18n.t("app.online"));
        // ... ajoute d'autres labels selon ton FXML
    }

    /** À appeler pour se désabonner proprement. */
    public void dispose() {
        I18nManager.getInstance().arreterEcoute(ecouteurI18n);
    }
}

//package org.erpklassup.erpklassup.controllers;
//
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.scene.control.Label;
//import javafx.scene.control.ToggleButton;
//import javafx.scene.layout.HBox;
//import org.erpklassup.erpklassup.controllers.actionPage.UserProfileController;
//import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
//import org.erpklassup.erpklassup.models.Role;
//import org.erpklassup.erpklassup.models.Utilisateur;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.util.I18nManager;
//import org.erpklassup.erpklassup.util.ModalUtil;
//
//import java.net.URL;
//import java.util.List;
//import java.util.Locale;
//import java.util.ResourceBundle;
//
//public class AppBarController implements Initializable {
//
//    @FXML private HBox userProfilBox;
//    @FXML private Label avatarLabelAppBar;
//    @FXML private Label nomUtilisateurAppBar;
//    @FXML private Label roleUtilisateurAppBar;
//    @FXML private Label lblDate;
//
//    @FXML private ToggleButton btnLangFr;
//    @FXML private ToggleButton btnLangEn;
//
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        chargerUtilisateurConnecte();
//        chargerDateDuJour();
//
//        if (userProfilBox != null) {
//            userProfilBox.setOnMouseClicked(e -> ouvrirPopupProfil());
//        }
//
//        demarrerRafraichissementDate();
//        configurerLangues();   // ✅ AJOUTER
//    }
//
//    private void configurerLangues() {
//        if (btnLangFr == null || btnLangEn == null) return;
//
//        I18nManager i18n = I18nManager.getInstance();
//
//        // Synchroniser avec la langue actuelle
//        boolean isFr = i18n.getLocale().equals(Locale.FRENCH);
//        btnLangFr.setSelected(isFr);
//        btnLangEn.setSelected(!isFr);
//
//        // Handler FR
//        btnLangFr.setOnAction(e -> {
//            i18n.setLocale(Locale.FRENCH);
//            btnLangFr.setSelected(true);
//            btnLangEn.setSelected(false);
//            appliquerLangue();
//        });
//
//        // Handler EN
//        btnLangEn.setOnAction(e -> {
//            i18n.setLocale(Locale.ENGLISH);
//            btnLangFr.setSelected(false);
//            btnLangEn.setSelected(true);
//            appliquerLangue();
//        });
//    }
//
//    /**
//     * Notifie tous les écouteurs pour rafraîchir les textes.
//     */
//    private void appliquerLangue() {
//        // Rafraîchit l'app-bar elle-même
//        chargerDateDuJour();
//
//        // Notifie le reste de l'app (via un mécanisme d'écouteur)
//        I18nManager.getInstance().localeProperty().set(
//                I18nManager.getInstance().getLocale()
//        );
//    }
//
//    private static final String CHEMIN_PROFIL =
//            "/org/erpklassup/erpklassup/view/user-detail-selfpage.fxml";
//
//
//    @FXML
//    public void handleOuvrirProfil(){
//        ouvrirPopupProfil();
//    }
//
//    private void chargerUtilisateurConnecte() {
//        Utilisateur user = SessionManager.getInstance().getUtilisateurCourant();
//        if (user == null) {
//            nomUtilisateurAppBar.setText("Non connecté");
//            roleUtilisateurAppBar.setText("—");
//            avatarLabelAppBar.setText("?");
//            return;
//        }
//
//        nomUtilisateurAppBar.setText(user.getNomComplet());
//        avatarLabelAppBar.setText(calculerInitiales(user));
//
//        try {
//            List<Role> roles = new UtilisateurRoleDAO()
//                    .findRolesByUtilisateur(user.getIdUtilisateur());
//            roleUtilisateurAppBar.setText(roles.isEmpty() ? "Aucun rôle" : roles.get(0).getNomRole());
//        } catch (Exception e) {
//            roleUtilisateurAppBar.setText("—");
//        }
//    }
//
//    private String calculerInitiales(Utilisateur user) {
//        String nom = user.getNom() != null ? user.getNom() : "";
//        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
//        String initiales = "";
//        if (!prenom.isEmpty()) initiales += prenom.substring(0, 1);
//        if (!nom.isEmpty()) initiales += nom.substring(0, 1);
//        return initiales.isEmpty() ? "?" : initiales.toUpperCase();
//    }
//
//    /**
//     * ✅ Ouvre la popup profil via ModalUtil, positionnée sous le bloc utilisateur.
//     */
//    private void ouvrirPopupProfil() {
//        UserProfileController ctrl = ModalUtil.ouvrirPopupSous(
//                getClass(),
//                "/org/erpklassup/erpklassup/view/user-detail-selfpage.fxml",
//                "",
//                userProfilBox.getScene().getWindow(),
//                userProfilBox,
//                480,      // largeur visible
//                640       // hauteur visible
//        );
//
//        if (ctrl == null) {
//            System.err.println("❌ Impossible d'ouvrir la popup profil");
//        }
//    }
//
//    // ========================================
//
//    /**
//     * Affiche la date du jour formatée : "Jeu. 14 Sept. 2026"
//     */
//    private void chargerDateDuJour() {
//        if (lblDate == null) return;
//
//        java.time.LocalDate aujourdHui = java.time.LocalDate.now();
//
//        // Format français abrégé : "Jeu. 14 Sept. 2026"
//        java.time.format.DateTimeFormatter formatter =
//                java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM yyyy", java.util.Locale.FRENCH);
//
//        String dateFormatee = aujourdHui.format(formatter);
//
//        // Capitaliser la première lettre ("jeu." → "Jeu.")
//        if (!dateFormatee.isEmpty()) {
//            dateFormatee = dateFormatee.substring(0, 1).toUpperCase()
//                    + dateFormatee.substring(1);
//        }
//
//        lblDate.setText(dateFormatee);
//    }
//
//    /**
//     * Programme un rafraîchissement de la date toutes les heures.
//     * Utile si l'app reste ouverte après minuit.
//     */
//    private void demarrerRafraichissementDate() {
//        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
//                new javafx.animation.KeyFrame(
//                        javafx.util.Duration.hours(1),
//                        e -> chargerDateDuJour()
//                )
//        );
//        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
//        timeline.play();
//    }
//}