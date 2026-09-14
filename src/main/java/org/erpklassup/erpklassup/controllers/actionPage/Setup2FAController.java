package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.AuthService;
import org.erpklassup.erpklassup.service.Security2FAService;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class Setup2FAController implements Initializable {

    @FXML private ImageView qrCodeView;
    @FXML private Label secretLabel;
    @FXML private TextField codeField;
    @FXML private Label errorLabel;

    private final Security2FAService security2FAService = new Security2FAService();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final AuditService auditService = new AuditService();

    private String idUtilisateur;
    private String username;
    private String secret;
    private Consumer<AuthService.ResultatConnexion> onSucces;

    // ✅ Raccourci i18n
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            if (codeField != null) codeField.requestFocus();
        });
    }

    /**
     * Initialise le contrôleur avec les données de l'utilisateur.
     * Génère la clé secrète, le QR code, et affiche le tout.
     *
     * @param onSucces Callback exécuté après activation réussie
     */
    public void initData(String idUtilisateur,
                         String username,
                         String email,
                         String nomEcole,
                         String idEcole,
                         String ipAdresse,
                         String userAgent,
                         String nomApp,
                         Consumer<AuthService.ResultatConnexion> onSucces) {

        this.idUtilisateur = idUtilisateur;
        this.username = username;
        this.onSucces = onSucces;

        // 1. Générer la clé secrète
        this.secret = security2FAService.genererCleSecrete();

        // 2. Générer et afficher le QR code
        Image qr = security2FAService.genererQrCode(
                username != null ? username : "utilisateur",
                nomEcole != null ? nomEcole : "KlassUp",
                secret
        );

        if (qr != null) {
            qrCodeView.setImage(qr);
        } else {
            afficherErreur(i18n().t("setup2fa.error.qr_generation"));
        }

        // 3. Afficher la clé secrète en clair (fallback si le scan échoue)
        secretLabel.setText(secret);
    }

    @FXML
    private void handleValider() {
        String code = codeField.getText();
        if (code == null || code.isBlank()) {
            afficherErreur(i18n().t("setup2fa.error.code_required"));
            return;
        }

        try {
            int codeInt = Integer.parseInt(code.trim());

            if (!security2FAService.verifierCodeTOTP(secret, codeInt)) {
                afficherErreur(i18n().t("setup2fa.error.invalid_code"));
                return;
            }

            // 1. Activer la 2FA en BDD (secret + deuxFacteursActive = 1 + doitConfigurer2FA = 0)
            utilisateurDAO.finaliserSetup2FA(idUtilisateur, secret);

            // 2. Générer + enregistrer les 8 codes de secours (hachés bcrypt)
            List<String> codesSecours = security2FAService.genererCodesSecours(idUtilisateur, 8);

            // 3. Audit
            auditService.tracerActionAsync(
                    "SECURITE",
                    "ACTIVATION_2FA",
                    "2FA activée pour l'utilisateur : " + username
            );

            // 4. Toast
            ToastNotification.succes(getStage(), i18n().t("setup2fa.success"));

            // 5. Afficher les codes de secours
            afficherCodesSecours(codesSecours);

            // 6. Callback → navigation vers dashboard
            if (onSucces != null) {
                onSucces.accept(new AuthService.ResultatConnexion(true, "2FA configurée"));
            }

            fermer();

        } catch (NumberFormatException e) {
            afficherErreur(i18n().t("setup2fa.error.numeric_code"));
        } catch (Exception e) {
            afficherErreur(i18n().t("common.error_detail", e.getMessage()));
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler() {
        // La configuration 2FA est obligatoire — on ne peut pas annuler
        afficherErreur(i18n().t("setup2fa.error.mandatory"));
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void afficherCodesSecours(List<String> codes) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefWidth(400);
        area.setPrefHeight(250);
        area.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 14;");

        StringBuilder sb = new StringBuilder();
        sb.append(i18n().t("setup2fa.codes.intro")).append("\n");
        sb.append(i18n().t("setup2fa.codes.warning")).append("\n\n");
        for (String c : codes) {
            sb.append("  •  ").append(c).append("\n");
        }
        area.setText(sb.toString());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n().t("setup2fa.codes.title"));
        alert.setHeaderText(i18n().t("setup2fa.codes.header"));
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void afficherErreur(String msg) {
        if (errorLabel == null) return;
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private Stage getStage() {
        return (Stage) codeField.getScene().getWindow();
    }

    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }
}