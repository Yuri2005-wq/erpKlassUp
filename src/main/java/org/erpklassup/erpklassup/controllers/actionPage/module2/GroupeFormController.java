package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.module2.GroupeMatiere;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.erpklassup.erpklassup.util.ValidationUtil.*;

public class GroupeFormController implements Initializable {

    @FXML private FontIcon  iconModal;
    @FXML private Label     lblTitreModal;
    @FXML private TextField txtCode;
    @FXML private TextField txtNom;
    @FXML private TextField txtOrdre;
    @FXML private Label errCode;
    @FXML private Label errNom;
    @FXML private Label errGlobal;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    private final AcademiqueService service = new AcademiqueService();
    private final AuditService auditService = new AuditService();

    private String idEcoleCourante;
    private GroupeMatiere groupeEnEdition;
    private Runnable onSuccess;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
        Platform.runLater(() -> txtCode.requestFocus());
        forcerMajuscules(txtCode);
        forcerCasseTitre(txtNom);
        forcerEntier(txtOrdre);
    }

    public void setGroupe(GroupeMatiere groupe) {
        this.groupeEnEdition = groupe;

        if (groupe == null) {
            lblTitreModal.setText("Nouveau groupe de matières");
            iconModal.setIconLiteral("fth-layers");
            txtCode.clear();
            txtNom.clear();
            txtOrdre.setText("0");
        } else {
            lblTitreModal.setText("Modifier le groupe");
            iconModal.setIconLiteral("fth-edit-2");
            txtCode.setText(groupe.getCodeGroupe());
            txtNom.setText(groupe.getNomGroupe());
            txtOrdre.setText(String.valueOf(groupe.getOrdreAffichage()));
        }

//        appliquerControlesAcces();
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    private void appliquerControlesAcces() {
        String actionRequise = (groupeEnEdition == null)
                ? "academique.groupematiere.creer"
                : "academique.groupematiere.modifier";

        boolean autorise = ControleAcces.autoriseAction(actionRequise);
        btnEnregistrer.setVisible(autorise);
        btnEnregistrer.setManaged(autorise);

        if (!autorise) {
            errGlobal.setText("Vous n'avez pas la permission d'effectuer cette action.");
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        }
    }

    @FXML
    private void handleAnnuler() { fermer(); }

    @FXML
    private void handleEnregistrer() {
        if (!validerFormulaire()) return;

        GroupeMatiere groupe = (groupeEnEdition == null)
                ? new GroupeMatiere() : groupeEnEdition;

        if (groupeEnEdition == null) {
            groupe.setId(UUID.randomUUID().toString());
            groupe.setIdEcole(idEcoleCourante);
        }

        groupe.setCodeGroupe(txtCode.getText().trim().toUpperCase());
        groupe.setNomGroupe(txtNom.getText().trim());

        try {
            int ordre = Integer.parseInt(txtOrdre.getText().trim());
            groupe.setOrdreAffichage(ordre);
        } catch (NumberFormatException e) {
            groupe.setOrdreAffichage(0);
        }

        btnEnregistrer.setDisable(true);

        Runnable onOk = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            auditService.tracerActionAsync("STRUCTURE",
                    groupeEnEdition == null ? "CREATION_GROUPE_MATIERE" : "MODIFICATION_GROUPE_MATIERE",
                    "Groupe : " + groupe.getCodeGroupe());
            ToastNotification.succes(getStage(),
                    groupeEnEdition == null ? "Groupe créé" : "Groupe modifié");
            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onErr = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            errGlobal.setText(err.getMessage());
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        });

        if (groupeEnEdition == null) {
            service.creerGroupeMatiereAsync(groupe, onOk, onErr);
        } else {
            service.modifierGroupeMatiereAsync(groupe, onOk, onErr);
        }
    }

    private boolean validerFormulaire() {
        hideErrors();
        boolean ok = true;
        if (txtCode.getText() == null || txtCode.getText().isBlank()) {
            showError(errCode, "Le code est obligatoire");
            ok = false;
        }
        if (txtNom.getText() == null || txtNom.getText().isBlank()) {
            showError(errNom, "Le nom est obligatoire");
            ok = false;
        }
        return ok;
    }

    private void showError(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideErrors() {
        for (Label l : new Label[]{errCode, errNom, errGlobal}) {
            if (l != null) { l.setVisible(false); l.setManaged(false); }
        }
    }

    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }
}