package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.PeriodeEvaluationService;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class GenererPeriodesModalController implements Initializable {

    /* ===== FXML ===== */
    @FXML private ComboBox<PeriodeEvaluationService.ModeGeneration> comboMode;
    @FXML private DatePicker pickerDebut;
    @FXML private DatePicker pickerFin;
    @FXML private Label lblApercu;
    @FXML private Label errDebut;
    @FXML private Label errFin;
    @FXML private Label errGlobal;
    @FXML private Button btnAnnuler;
    @FXML private Button btnGenerer;

    /* ===== ÉTAT ===== */
    private String idEcole;
    private String idAnnee;
    private Runnable onSuccess;
    private AnneeScolaire a = SessionManager.getInstance().getAnneeActive();


    private final PeriodeEvaluationService service = new PeriodeEvaluationService();

    private static final DateTimeFormatter FORMAT_AFFICHAGE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /* ===== INITIALISATION ===== */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remplir la ComboBox
        comboMode.setItems(FXCollections.observableArrayList(
                PeriodeEvaluationService.ModeGeneration.values()));

        comboMode.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PeriodeEvaluationService.ModeGeneration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : libelleMode(item));
            }
        });
        comboMode.setButtonCell(comboMode.getCellFactory().call(null));
        comboMode.getSelectionModel().select(PeriodeEvaluationService.ModeGeneration.TRIMESTRES_SEQUENCES);

        // Forcer le format de date
        appliquerFormatDate(pickerDebut);
        appliquerFormatDate(pickerFin);

        // Aperçu : mise à jour à chaque changement
        comboMode.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> mettreAJourApercu());
        pickerDebut.valueProperty().addListener((o, a, n) -> mettreAJourApercu());
        pickerFin.valueProperty().addListener((o, a, n) -> mettreAJourApercu());

        Platform.runLater(pickerDebut::requestFocus);

        pickerDebut.setValue(a.getDateDebut());
        pickerFin.setValue(a.getDateFin());
    }

    /* ===== API PUBLIQUE ===== */
    public void initData(String idEcole, String idAnnee, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.idAnnee = idAnnee;
        this.onSuccess = onSuccess;
    }

    /* ===== ACTIONS ===== */
    @FXML
    private void handleAnnuler() {
        fermer();
    }

    @FXML
    private void handleGenerer() {
        if (!ControleAcces.autoriseAction("user.creer")) {
            ToastNotification.avertissement(getStage(), "Permission refusée");
            return;
        }

        if (!validerFormulaire()) return;
        AnneeScolaire a = SessionManager.getInstance().getAnneeActive();
        if (a == null || a.getDateDebut() == null || a.getDateFin() == null) {
            ToastNotification.erreur(getStage(), "Dates de l'année manquantes.");
            return;
        }


        LocalDate debut = pickerDebut.getValue();
        LocalDate fin = pickerFin.getValue();
        PeriodeEvaluationService.ModeGeneration mode =
                comboMode.getSelectionModel().getSelectedItem();

        btnGenerer.setDisable(true);
        cacherErreurs();

        service.genererStructureStandardAsync(idEcole, idAnnee, debut, fin, mode,
                () -> Platform.runLater(() -> {
                    btnGenerer.setDisable(false);
                    ToastNotification.succes(getStage(),
                            "Structure générée avec succès (" + libelleMode(mode) + ").");
                    if (onSuccess != null) onSuccess.run();
                    fermer();
                }),
                err -> Platform.runLater(() -> {
                    btnGenerer.setDisable(false);
                    String msg = err.getMessage() != null
                            ? err.getMessage()
                            : "Échec de la génération.";
                    errGlobal.setText(msg);
                    errGlobal.setVisible(true);
                    errGlobal.setManaged(true);
                }));
    }

    /* ===== APERÇU ===== */
    private void mettreAJourApercu() {
        LocalDate debut = pickerDebut.getValue();
        LocalDate fin = pickerFin.getValue();
        PeriodeEvaluationService.ModeGeneration mode =
                comboMode.getSelectionModel().getSelectedItem();

        if (debut == null || fin == null || mode == null) {
            lblApercu.setText("Sélectionnez un mode et les dates pour voir l'aperçu.");
            return;
        }
        if (!fin.isAfter(debut)) {
            lblApercu.setText("⚠️ La date de fin doit être après la date de début.");
            return;
        }

        if (mode == PeriodeEvaluationService.ModeGeneration.SEMESTRES) {
            lblApercu.setText(
                    "• 2 semestres (S1, S2)\n" +
                            "• Période : " + debut.format(FORMAT_AFFICHAGE)
                            + " → " + fin.format(FORMAT_AFFICHAGE)
            );
        } else {
            lblApercu.setText(
                    "• 3 trimestres (T1, T2, T3)\n" +
                            "• 6 séquences (2 par trimestre)\n" +
                            "• Période : " + debut.format(FORMAT_AFFICHAGE)
                            + " → " + fin.format(FORMAT_AFFICHAGE)
            );
        }
    }

    /* ===== VALIDATION ===== */
    private boolean validerFormulaire() {
        cacherErreurs();
        boolean ok = true;

        if (comboMode.getSelectionModel().getSelectedItem() == null) {
            errGlobal.setText("Sélectionnez un mode de génération.");
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
            ok = false;
        }
        if (pickerDebut.getValue() == null) {
            afficherErreur(errDebut, "Date de début obligatoire");
            ok = false;
        }
        if (pickerFin.getValue() == null) {
            afficherErreur(errFin, "Date de fin obligatoire");
            ok = false;
        }
        if (pickerDebut.getValue() != null && pickerFin.getValue() != null
                && !pickerFin.getValue().isAfter(pickerDebut.getValue())) {
            afficherErreur(errFin, "La date de fin doit être après la date de début");
            ok = false;
        }
        return ok;
    }

    private void afficherErreur(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void cacherErreurs() {
        for (Label l : new Label[]{errDebut, errFin, errGlobal}) {
            if (l != null) { l.setVisible(false); l.setManaged(false); }
        }
    }

    /* ===== FORMAT DATE ===== */
    private void appliquerFormatDate(DatePicker picker) {
        picker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? FORMAT_AFFICHAGE.format(date) : "";
            }

            @Override
            public LocalDate fromString(String texte) {
                if (texte == null || texte.isBlank()) return null;
                try {
                    return LocalDate.parse(texte.trim(), FORMAT_AFFICHAGE);
                } catch (DateTimeParseException e) {
                    System.err.println("⚠️ Format date invalide : " + texte);
                    return null;
                }
            }
        });
        picker.setPromptText("jj/mm/aaaa");
    }

    /* ===== HELPERS ===== */
    private String libelleMode(PeriodeEvaluationService.ModeGeneration mode) {
        if (mode == null) return "";
        return switch (mode) {
            case TRIMESTRES_SEQUENCES -> "Trimestres + Séquences (3 × 2)";
            case SEMESTRES -> "Semestres (2)";
        };
    }

    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler == null || btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }
}