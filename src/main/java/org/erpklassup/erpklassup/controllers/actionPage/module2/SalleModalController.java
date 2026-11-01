package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.controllers.actionPage.UserDetailController;
import org.erpklassup.erpklassup.models.module2.Salle;
import org.erpklassup.erpklassup.models.module2.Salle.TypeSalle;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.module2.SalleService;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ModalUtil;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.erpklassup.erpklassup.util.ValidationUtil.*;

/**
 * Controller de la modale de création / édition d'une salle.
 */
public class SalleModalController implements Initializable {

    /* ===== FXML ===== */
    @FXML private FontIcon iconModal;
    @FXML private Label lblTitreModal;
    @FXML private Label lblSousTitreModal;

    @FXML private TextField txtCode;
    @FXML private TextField txtBatiment;
    @FXML private TextField txtNom;
    @FXML private ComboBox<TypeSalle> comboType;
    @FXML private TextField txtCapacite;

    @FXML private FontIcon iconOccupation;
    @FXML private Label lblOccupationTitre;
    @FXML private Label lblOccupationDesc;
    @FXML private ToggleButton toggleOccupee;

    @FXML private Label errCode;
    @FXML private Label errNom;
    @FXML private Label errType;
    @FXML private Label errCapacite;
    @FXML private Label errGlobal;

    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ===== Services ===== */
    private final SalleService salleService = new SalleService();
    private final AuditService auditService = new AuditService();

    /* ===== État ===== */
    private String idEcoleCourante;
    private Salle salleEnEdition;
    private Runnable onSucces;

    /* ===== INIT ===== */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remplir la ComboBox type
        comboType.setItems(FXCollections.observableArrayList(TypeSalle.values()));
        comboType.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TypeSalle item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    FontIcon ic = new FontIcon(item.getIcone());
                    ic.setIconSize(12);
                    ic.setIconColor(Color.web("#64748B"));
                    setGraphic(ic);
                    setText(item.getLibelle());
                }
            }
        });
        comboType.setButtonCell(comboType.getCellFactory().call(null));

        // Toggle occupation : mise à jour dynamique des labels
//        toggleOccupee.selectedProperty().addListener((o, a, isOccupee) -> {
//            if (Boolean.TRUE.equals(isOccupee)) {
//                iconOccupation.setIconLiteral("fth-lock");
//                iconOccupation.setIconColor(Color.web("#E5484D"));
//                lblOccupationTitre.setText("Salle occupée");
//                lblOccupationDesc.setText("La salle est actuellement utilisée");
//            } else {
//                iconOccupation.setIconLiteral("fth-unlock");
//                iconOccupation.setIconColor(Color.web("#16A34A"));
//                lblOccupationTitre.setText("Salle libre");
//                lblOccupationDesc.setText("La salle est actuellement disponible");
//            }
//        });

        // ✅ NOUVEAU : bloquer la désélection si la salle est occupée par des classes
        toggleOccupee.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            // Si on veut désélectionner ET que la salle est actuellement occupée en BD
            if (toggleOccupee.isSelected() && salleEnEdition != null
                    && salleEnEdition.isOccupee()) {

                // Bloquer le changement immédiat
                event.consume();

                // Vérifier les classes qui occupent la salle
                salleService.listerNomsClassesDansSalleAsync(salleEnEdition.getId(),
                        noms -> Platform.runLater(() -> {
                            if (noms.isEmpty()) {
                                // Aucune classe → on peut libérer
                                toggleOccupee.setSelected(false);
                            } else {
                                // ✅ ALERTE INFORMATIVE BLOQUANTE
                                String message = "Impossible de libérer cette salle.\n\n"
                                        + "Elle est actuellement occupée par les classes suivantes :\n\n"
                                        + noms.stream().map(n -> "• " + n)
                                        .collect(java.util.stream.Collectors.joining("\n"))
                                        + "\n\nRéaffectez ces classes à une autre salle avant de pouvoir libérer celle-ci.";

                                AlertUtil.afficherInformation(
                                        "Salle occupée",
                                        message,
                                        getStage());
                                // ❌ Le toggle reste sur "occupée"
                            }
                        }),
                        err -> Platform.runLater(() -> {
                            ToastNotification.erreur(getStage(),
                                    "Erreur : " + err.getMessage());
                        }));
            }
        });


        // Force l'état initial
        toggleOccupee.setSelected(false);
        forcerCasseTitre(txtBatiment);
        forcerCasseTitre(txtNom);
        forcerMajuscules(txtCode);
        verifierEntier(txtCapacite, "capacite", getStage(), true);

        Platform.runLater(() -> txtCode.requestFocus());
        configurerToggleSwitch(toggleOccupee);
    }

    /* ===== API PUBLIQUE ===== */
    public void initCreation(String idEcole, Runnable onSucces) {
        this.idEcoleCourante = idEcole;
        this.salleEnEdition = null;
        this.onSucces = onSucces;

        lblTitreModal.setText("Nouvelle salle");
        lblSousTitreModal.setText("Renseignez les informations de la salle");
        iconModal.setIconLiteral("fth-home");

        viderFormulaire();

        // Auto-génération du code
        salleService.genererCodeSalleAsync(idEcole,
                code -> Platform.runLater(() -> txtCode.setText(code)),
                err -> System.err.println("❌ " + err.getMessage()));

//        appliquerControlesAcces();
    }

    public void initEdition(Salle salle, Runnable onSucces) {
        if (salle == null) return;

        this.idEcoleCourante = salle.getIdEcole();
        this.salleEnEdition = salle;
        this.onSucces = onSucces;

        lblTitreModal.setText("Modifier la salle");
        lblSousTitreModal.setText("Mettez à jour les informations");
        iconModal.setIconLiteral("fth-edit-2");

        txtCode.setText(salle.getCodeSalle());
        txtNom.setText(salle.getNomSalle());
        txtBatiment.setText(salle.getBatiment() != null ? salle.getBatiment() : "");
        txtCapacite.setText(String.valueOf(salle.getCapaciteMax()));
        comboType.getSelectionModel().select(salle.getTypeSalle());
        toggleOccupee.setSelected(salle.isOccupee());

        hideAllErrors();
//        appliquerControlesAcces();
    }

    /* ===== PERMISSIONS ===== */
    private void appliquerControlesAcces() {
        String actionRequise = (salleEnEdition == null) ? "salle.create" : "salle.update";
        boolean autorise = ControleAcces.autoriseAction(actionRequise);

        btnEnregistrer.setVisible(autorise);
        btnEnregistrer.setManaged(autorise);

        if (!autorise) {
            errGlobal.setText("Vous n'avez pas la permission de "
                    + (salleEnEdition == null ? "créer" : "modifier") + " une salle.");
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
//        String actionRequise = (salleEnEdition == null) ? "salle.create" : "salle.update";
//        if (!ControleAcces.autoriseAction(actionRequise)) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        if (!validerFormulaire()) return;

        Salle salle = (salleEnEdition == null) ? new Salle() : salleEnEdition;
        if (salleEnEdition == null) {
            salle.setId(UUID.randomUUID().toString());
            salle.setIdEcole(idEcoleCourante);
        }

        salle.setCodeSalle(txtCode.getText().trim());
        salle.setNomSalle(txtNom.getText().trim());
        salle.setBatiment(txtBatiment.getText() != null ? txtBatiment.getText().trim() : null);
        salle.setCapaciteMax(Integer.parseInt(txtCapacite.getText().trim()));
        salle.setTypeSalle(comboType.getSelectionModel().getSelectedItem());
        salle.setOccupee(toggleOccupee.isSelected());

        btnEnregistrer.setDisable(true);

        Runnable onSuccess = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);

            auditService.tracerActionAsync("STRUCTURE",
                    salleEnEdition == null ? "CREATION_SALLE" : "MODIFICATION_SALLE",
                    "Salle : " + salle.getCodeSalle());

            ToastNotification.succes(getStage(),
                    salleEnEdition == null ? "Salle créée avec succès" : "Salle modifiée avec succès");

            if (onSucces != null) onSucces.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onError = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            errGlobal.setText(err.getMessage());
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        });

        if (salleEnEdition == null) {
            salleService.creerAsync(salle, onSuccess, onError);
        } else {
            salleService.modifierAsync(salle, onSuccess, onError);
        }
    }

    /* ===== VALIDATION ===== */
    private boolean validerFormulaire() {
        hideAllErrors();
        boolean ok = true;

        if (txtCode.getText() == null || txtCode.getText().isBlank()) {
            showError(errCode, "Le code est obligatoire");
            ok = false;
        }
        if (txtNom.getText() == null || txtNom.getText().isBlank()) {
            showError(errNom, "Le nom est obligatoire");
            ok = false;
        }
        if (comboType.getSelectionModel().getSelectedItem() == null) {
            showError(errType, "Le type est obligatoire");
            ok = false;
        }
        try {
            int cap = Integer.parseInt(txtCapacite.getText().trim());
            if (cap <= 0) {
                showError(errCapacite, "La capacité doit être supérieure à 0");
                ok = false;
            }
        } catch (NumberFormatException e) {
            showError(errCapacite, "Capacité invalide");
            ok = false;
        }
        return ok;
    }

    /* ===== HELPERS ===== */
    private void viderFormulaire() {
        txtCode.clear();
        txtNom.clear();
        txtBatiment.clear();
        txtCapacite.setText("50");
        comboType.getSelectionModel().clearSelection();
        toggleOccupee.setSelected(false);
        hideAllErrors();
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideAllErrors() {
        for (Label l : new Label[]{errCode, errNom, errType, errCapacite, errGlobal}) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
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