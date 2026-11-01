package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.module2.Filiere;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.erpklassup.erpklassup.util.ValidationUtil.forcerCasseTitre;
import static org.erpklassup.erpklassup.util.ValidationUtil.forcerMajuscules;

public class FiliereFormController implements Initializable {

    @FXML private FontIcon  iconModal;
    @FXML private Label     lblTitreModal;
    @FXML private TextField txtCode;
    @FXML private TextField txtNom;
    @FXML private Label errCode;
    @FXML private Label errNom;
    @FXML private Label errGlobal;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    private final AcademiqueService service = new AcademiqueService();
    private final AuditService auditService = new AuditService();

    private String idEcoleCourante;
    private Filiere filiereEnEdition;
    private Runnable onSuccess;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
        Platform.runLater(() -> txtCode.requestFocus());
        forcerMajuscules(txtCode);
        forcerCasseTitre(txtNom);
    }

    public void setFiliere(Filiere filiere) {
        this.filiereEnEdition = filiere;

        if (filiere == null) {
            lblTitreModal.setText("Nouvelle filière");
            iconModal.setIconLiteral("fth-target");
            txtCode.clear();
            txtNom.clear();
        } else {
            lblTitreModal.setText("Modifier la filière");
            iconModal.setIconLiteral("fth-edit-2");
            txtCode.setText(filiere.getCodeFiliere());
            txtNom.setText(filiere.getNomFiliere());
        }

//        appliquerControlesAcces();
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    private void appliquerControlesAcces() {
        String actionRequise = (filiereEnEdition == null)
                ? "academique.filiere.creer"
                : "academique.filiere.modifier";

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

        Filiere filiere = (filiereEnEdition == null) ? new Filiere() : filiereEnEdition;
        if (filiereEnEdition == null) {
            filiere.setId(UUID.randomUUID().toString());
            filiere.setIdEcole(idEcoleCourante);
        }
        filiere.setCodeFiliere(txtCode.getText().trim().toUpperCase());
        filiere.setNomFiliere(txtNom.getText().trim());

        btnEnregistrer.setDisable(true);

        Runnable onOk = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            auditService.tracerActionAsync("STRUCTURE",
                    filiereEnEdition == null ? "CREATION_FILIERE" : "MODIFICATION_FILIERE",
                    "Filière : " + filiere.getCodeFiliere());
            ToastNotification.succes(getStage(),
                    filiereEnEdition == null ? "Filière créée" : "Filière modifiée");
            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onErr = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            errGlobal.setText(err.getMessage());
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        });

        if (filiereEnEdition == null) {
            service.creerFiliereAsync(filiere, onOk, onErr);
        } else {
            service.modifierFiliereAsync(filiere, onOk, onErr);
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