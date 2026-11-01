package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.module2.Niveau;
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
import static org.erpklassup.erpklassup.util.ValidationUtil.forcerEntier;

public class NiveauFormController implements Initializable {

    /* ===== FXML ===== */
    @FXML private FontIcon  iconModal;
    @FXML private Label     lblTitreModal;
    @FXML private TextField txtNomNiveau;
    @FXML private ComboBox<String> comboSysteme;
    @FXML private ComboBox<String> comboCycle;
    @FXML private TextField txtOrdre;
    @FXML private Label errNomNiveau;
    @FXML private Label errGlobal;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ===== SERVICES ===== */
    private final AcademiqueService service = new AcademiqueService();
    private final AuditService auditService = new AuditService();

    /* ===== ÉTAT ===== */
    private String idEcoleCourante;
    private Niveau niveauEnEdition;   // null = création
    private Runnable onSuccess;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        // Remplir les ComboBox
        comboSysteme.setItems(FXCollections.observableArrayList(
                "FRANCOPHONE", "ANGLOPHONE"));
        comboSysteme.getSelectionModel().selectFirst();

        comboCycle.setItems(FXCollections.observableArrayList(
                "MATERNELLE", "PRIMAIRE", "PREMIER_CYCLE", "SECOND_CYCLE"));
        comboCycle.getSelectionModel().selectFirst();

        forcerCasseTitre(txtNomNiveau);
        forcerEntier(txtOrdre);
        Platform.runLater(() -> txtNomNiveau.requestFocus());
    }

    /* ===== API PUBLIQUE — Appelée par AcademiqueController ===== */

    /** Création d'un nouveau niveau. */
    public void setNiveau(Niveau niveau) {
        this.niveauEnEdition = niveau;

        if (niveau == null) {
            // Mode création
            lblTitreModal.setText("Nouveau niveau");
            iconModal.setIconLiteral("fth-award");
            txtNomNiveau.clear();
            txtOrdre.setText("0");
            comboSysteme.getSelectionModel().selectFirst();
            comboCycle.getSelectionModel().selectFirst();
        } else {
            // Mode édition
            lblTitreModal.setText("Modifier le niveau");
            iconModal.setIconLiteral("fth-edit-2");
            txtNomNiveau.setText(niveau.getNomNiveau());
            txtOrdre.setText(String.valueOf(niveau.getOrdre()));

            if (niveau.getSysteme() != null) {
                comboSysteme.getSelectionModel().select(niveau.getSysteme());
            }
            if (niveau.getCycle() != null) {
                comboCycle.getSelectionModel().select(niveau.getCycle());
            }
        }
//        appliquerControlesAcces();
    }

    /** Callback à appeler après un enregistrement réussi. */
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /* ===== PERMISSIONS ===== */
    private void appliquerControlesAcces() {
        String actionRequise = (niveauEnEdition == null)
                ? "academique.niveau.creer"
                : "academique.niveau.modifier";

        boolean autorise = ControleAcces.autoriseAction(actionRequise);
        btnEnregistrer.setVisible(autorise);
        btnEnregistrer.setManaged(autorise);

        if (!autorise) {
            errGlobal.setText("Vous n'avez pas la permission d'effectuer cette action.");
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        }
    }

    /* ===== ACTIONS ===== */
    @FXML
    private void handleAnnuler() {
        fermer();
    }

    @FXML
    private void handleEnregistrer() {
        if (!validerFormulaire()) return;

        Niveau niveau = (niveauEnEdition == null) ? new Niveau() : niveauEnEdition;
        if (niveauEnEdition == null) {
            niveau.setId(UUID.randomUUID().toString());
            niveau.setIdEcole(idEcoleCourante);
        }

        niveau.setNomNiveau(txtNomNiveau.getText().trim());
        niveau.setSysteme(comboSysteme.getSelectionModel().getSelectedItem());
        niveau.setCycle(comboCycle.getSelectionModel().getSelectedItem());

        try {
            int ordre = Integer.parseInt(txtOrdre.getText().trim());
            niveau.setOrdre(ordre);
        } catch (NumberFormatException e) {
            niveau.setOrdre(0);
        }

        btnEnregistrer.setDisable(true);

        Runnable onOk = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            auditService.tracerActionAsync("STRUCTURE",
                    niveauEnEdition == null ? "CREATION_NIVEAU" : "MODIFICATION_NIVEAU",
                    "Niveau : " + niveau.getNomNiveau());

            ToastNotification.succes(getStage(),
                    niveauEnEdition == null ? "Niveau créé" : "Niveau modifié");

            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onErr = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            errGlobal.setText(err.getMessage());
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        });

        if (niveauEnEdition == null) {
            service.creerNiveauAsync(niveau, onOk, onErr);
        } else {
            service.modifierNiveauAsync(niveau, onOk, onErr);
        }
    }

    /* ===== VALIDATION ===== */
    private boolean validerFormulaire() {
        hideErrors();
        boolean ok = true;

        if (txtNomNiveau.getText() == null || txtNomNiveau.getText().isBlank()) {
            showError(errNomNiveau, "Le nom du niveau est obligatoire");
            ok = false;
        }
        if (comboSysteme.getSelectionModel().getSelectedItem() == null) {
            showError(errGlobal, "Le système est obligatoire");
            ok = false;
        }
        if (comboCycle.getSelectionModel().getSelectedItem() == null) {
            showError(errGlobal, "Le cycle est obligatoire");
            ok = false;
        }
        return ok;
    }

    /* ===== HELPERS ===== */
    private void showError(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideErrors() {
        for (Label l : new Label[]{errNomNiveau, errGlobal}) {
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