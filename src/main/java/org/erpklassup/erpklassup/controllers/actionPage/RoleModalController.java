package org.erpklassup.erpklassup.controllers.actionPage;


import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.RoleService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.util.function.Consumer;

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
    SessionManager session = SessionManager.getInstance();
    private String idEcole = session.getIdSessionCourante();
    private RoleOption roleAEditer; // null si création
    private Runnable onSuccessCallback;

    /**
     * Initialise la modale pour la CRÉATION d'un rôle.
     */
    public void initCreation(String idEcole, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.roleAEditer = null;
        this.onSuccessCallback = onSuccess;


        if (lblTitreModal != null) lblTitreModal.setText("Nouveau Rôle");
        if (lblSousTitreModal != null) lblSousTitreModal.setText("Définissez les informations de base pour ce rôle.");
        btnEnregistrer.setText("Créer le rôle");
    }

    /**
     * Initialise la modale pour la MODIFICATION d'un rôle existant.
     */
    public void initEdition(String idEcole, RoleOption role, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.roleAEditer = role;
        this.onSuccessCallback = onSuccess;

        if (lblTitreModal != null) lblTitreModal.setText("Modifier le Rôle");
        if (lblSousTitreModal != null) lblSousTitreModal.setText("Ajustez le nom et la description du rôle sélectionné.");

        txtNomRole.setText(role.nomRole());
        txtDescription.setText(role.description() != null ? role.description() : "");
        btnEnregistrer.setText("Enregistrer");
    }

    @FXML
    private void handleEnregistrer() {
        // Ne pas réécraser this.idEcole ici !
        String nom = txtNomRole.getText() != null ? txtNomRole.getText().trim() : "";
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : "";
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();

        if (nom.isBlank()) {
            ToastNotification.avertissement(stage, "Le nom du rôle est obligatoire.");
            return;
        }

        if (this.idEcole == null || this.idEcole.isBlank()) {
            ToastNotification.erreur(stage, "Aucune école sélectionnée.");
            return;
        }

        btnEnregistrer.setDisable(true);

        if (roleAEditer == null) {
            // MODE CRÉATION
            roleService.creerRoleAsync(idEcole, nom, description,
                    roleCree -> javafx.application.Platform.runLater(() -> {
                        auditService.tracerActionAsync("HABILITATIONS", "CREATION_ROLE", "Création du rôle : " + nom);
                        ToastNotification.succes(stage, "Rôle \"" + nom + "\" créé avec succès.");
                        fermerEtRafrachir();
                    }),
                    erreur -> javafx.application.Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        ToastNotification.erreur(stage, "Erreur : " + erreur.getMessage());
                    })
            );
        } else {
            // MODE MODIFICATION
            roleService.renommerRoleAsync(roleAEditer.idRole(), nom, description,
                    () -> javafx.application.Platform.runLater(() -> {
                        auditService.tracerActionAsync("HABILITATIONS", "MODIFICATION_ROLE", "Rôle modifié : " + nom);
                        ToastNotification.succes(stage, "Rôle mis à jour avec succès.");
                        fermerEtRafrachir();
                    }),
                    erreur -> javafx.application.Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        ToastNotification.erreur(stage, "Erreur lors de la modification du rôle.");
                    })
            );
        }
    }

    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    private void fermerEtRafrachir() {
        if (onSuccessCallback != null) {
            onSuccessCallback.run();
        }
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }
}