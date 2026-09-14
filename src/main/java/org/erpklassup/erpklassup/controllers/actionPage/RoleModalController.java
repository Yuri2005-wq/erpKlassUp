package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.RoleService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ToastNotification;

public class RoleModalController {

    @FXML private Label lblTitreModal;
    @FXML private Label lblSousTitreModal;
    @FXML private TextField txtNomRole;
    @FXML private TextArea txtDescription;
    @FXML private CheckBox chkIsActive;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    private final RoleService roleService = new RoleService();
    private final AuditService auditService = new AuditService();
    private final SessionManager session = SessionManager.getInstance();

    private String idEcole;
    private RoleOption roleAEditer; // null si création
    private Runnable onSuccessCallback;

    // ✅ Raccourci i18n
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    /**
     * Initialise la modale pour la CRÉATION d'un rôle.
     */
    public void initCreation(String idEcole, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.roleAEditer = null;
        this.onSuccessCallback = onSuccess;

        if (lblTitreModal != null) lblTitreModal.setText(i18n().t("role.modal.create.title"));
        if (lblSousTitreModal != null) lblSousTitreModal.setText(i18n().t("role.modal.create.subtitle"));
        if (btnEnregistrer != null) btnEnregistrer.setText(i18n().t("role.modal.create.button"));
        if (chkIsActive != null) chkIsActive.setSelected(true);
    }

    /**
     * Initialise la modale pour la MODIFICATION d'un rôle existant.
     */
    public void initEdition(String idEcole, RoleOption role, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.roleAEditer = role;
        this.onSuccessCallback = onSuccess;

        if (lblTitreModal != null) lblTitreModal.setText(i18n().t("role.modal.edit.title"));
        if (lblSousTitreModal != null) lblSousTitreModal.setText(i18n().t("role.modal.edit.subtitle"));

        if (txtNomRole != null) {
            txtNomRole.setText(role.nomRole() != null ? role.nomRole() : "");
        }
        if (txtDescription != null) {
            txtDescription.setText(role.description() != null ? role.description() : "");
        }
        if (btnEnregistrer != null) {
            btnEnregistrer.setText(i18n().t("common.save"));
        }
    }

    @FXML
    private void handleEnregistrer() {
        String nom = txtNomRole != null && txtNomRole.getText() != null
                ? txtNomRole.getText().trim() : "";
        String description = txtDescription != null && txtDescription.getText() != null
                ? txtDescription.getText().trim() : "";

        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();

        // ✅ Validation
        if (nom.isBlank()) {
            ToastNotification.avertissement(stage, i18n().t("role.error.name_required"));
            return;
        }

        if (this.idEcole == null || this.idEcole.isBlank()) {
            ToastNotification.erreur(stage, i18n().t("role.error.no_school"));
            return;
        }

        btnEnregistrer.setDisable(true);

        if (roleAEditer == null) {
            // ==========================================
            // MODE CRÉATION
            // ==========================================
            roleService.creerRoleAsync(idEcole, nom, description,
                    roleCree -> Platform.runLater(() -> {
                        auditService.tracerActionAsync(
                                "HABILITATIONS",
                                "CREATION_ROLE",
                                "Création du rôle : " + nom
                        );
                        ToastNotification.succes(stage, i18n().t("role.toast.created", nom));
                        fermerEtRafraichir();
                    }),
                    erreur -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        ToastNotification.erreur(stage,
                                i18n().t("common.error_detail", erreur.getMessage()));
                    })
            );
        } else {
            // ==========================================
            // MODE MODIFICATION
            // ==========================================
            roleService.renommerRoleAsync(roleAEditer.idRole(), nom, description,
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync(
                                "HABILITATIONS",
                                "MODIFICATION_ROLE",
                                "Rôle modifié : " + nom
                        );
                        ToastNotification.succes(stage, i18n().t("role.toast.updated"));
                        fermerEtRafraichir();
                    }),
                    erreur -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        ToastNotification.erreur(stage, i18n().t("role.error.update_failed"));
                    })
            );
        }
    }

    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    private void fermerEtRafraichir() {
        if (onSuccessCallback != null) {
            onSuccessCallback.run();
        }
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }
}