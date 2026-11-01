package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller pour la modale de création / édition d'une année scolaire.
 *
 * <p>Utilisation :
 * <pre>{@code
 * AnneeModalController ctrl = ModalUtil.ouvrirModal(
 *     getClass(),
 *     "/org/erpklassup/erpklassup/view/module2/annee-modal-view.fxml",
 *     "Année scolaire",
 *     owner,
 *     540, 620
 * );
 * // Pour la création :
 * ctrl.initCreation(idEcoleCourante, this::lancerRecherche);
 * // Pour l'édition :
 * ctrl.initEdition(annee, this::lancerRecherche);
 * }</pre>
 * </p>
 */
public class AnneeModalController implements Initializable {

    /* ============================================================
       FXML
       ============================================================ */
    @FXML private FontIcon iconModal;
    @FXML private Label lblTitreModal;
    @FXML private Label lblSousTitreModal;

    @FXML private TextField txtLibelle;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private ComboBox<AnneeScolaire.Statut> comboStatut;
    @FXML private CheckBox chkEstCourante;

    @FXML private Label errLibelle;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;
    @FXML private Label errGlobal;

    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final AnneeScolaireService anneeService = new AnneeScolaireService();
    private final AuditService auditService = new AuditService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String idEcoleCourante;
    private AnneeScolaire anneeEnEdition;      // null si création
    private Runnable onSucces;

    /* ============================================================
       i18n
       ============================================================ */
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remplir le ComboBox des statuts
        comboStatut.setItems(FXCollections.observableArrayList(
                AnneeScolaire.Statut.values()
        ));
        comboStatut.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AnneeScolaire.Statut item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLibelle());
            }
        });
        comboStatut.setButtonCell(comboStatut.getCellFactory().call(null));
        comboStatut.getSelectionModel().select(AnneeScolaire.Statut.PREPARATION);

        // Configurer les DatePickers
        configurerDatePickers();

        // Focus initial
        Platform.runLater(() -> txtLibelle.requestFocus());
    }

    private void configurerDatePickers() {
        // Bloquer les dates futures par défaut sur "début"
        dateDebut.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                // Aucune restriction : l'admin peut planifier dans le futur
            }
        });

        // Auto-suggestion : quand dateDebut change, si dateFin vide, on met +10 mois
        dateDebut.valueProperty().addListener((obs, old, newDate) -> {
            if (newDate != null && dateFin.getValue() == null) {
                dateFin.setValue(newDate.plusMonths(10));
            }
        });

        // Auto-suggestion du libellé
        dateDebut.valueProperty().addListener((obs, old, newDate) -> {
            if (newDate != null && (txtLibelle.getText() == null || txtLibelle.getText().isBlank())) {
                int debut = newDate.getYear();
                txtLibelle.setText(debut + "-" + (debut + 1));
            }
        });
    }

    /* ============================================================
       API PUBLIQUE — Appelée par la vue parente
       ============================================================ */

    /**
     * Initialise la modale en mode CRÉATION.
     */
    public void initCreation(String idEcole, Runnable onSucces) {
        this.idEcoleCourante = idEcole;
        this.anneeEnEdition = null;
        this.onSucces = onSucces;

        lblTitreModal.setText("Nouvelle année scolaire");
        lblSousTitreModal.setText("Renseignez les informations de l'année scolaire");
        iconModal.setIconLiteral("fth-calendar");

        // Valeurs par défaut : septembre à juin de l'année suivante
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutDefaut = LocalDate.of(aujourdhui.getYear(), 9, 1);
        LocalDate finDefaut = LocalDate.of(aujourdhui.getYear() + 1, 6, 30);

        dateDebut.setValue(debutDefaut);
        dateFin.setValue(finDefaut);
        txtLibelle.setText(aujourdhui.getYear() + "-" + (aujourdhui.getYear() + 1));
        chkEstCourante.setSelected(false);
        comboStatut.getSelectionModel().select(AnneeScolaire.Statut.PREPARATION);

        // Contrôle d'accès
        appliquerControlesAcces();
    }

    /**
     * Initialise la modale en mode ÉDITION.
     */
    public void initEdition(AnneeScolaire annee, Runnable onSucces) {
        if (annee == null) return;

        this.idEcoleCourante = annee.getIdEcole();
        this.anneeEnEdition = annee;
        this.onSucces = onSucces;

        lblTitreModal.setText("Modifier l'année");
        lblSousTitreModal.setText("Mettez à jour les informations de l'année");
        iconModal.setIconLiteral("fth-edit-2");

        // Remplir les champs
        txtLibelle.setText(annee.getLibelle());
        dateDebut.setValue(annee.getDateDebut());
        dateFin.setValue(annee.getDateFin());
        chkEstCourante.setSelected(annee.isEstCourante());

        if (annee.getStatut() != null) {
            comboStatut.getSelectionModel().select(annee.getStatut());
        }

        // Contrôle d'accès
        appliquerControlesAcces();
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
        // Le bouton Enregistrer n'est visible que si l'utilisateur a la permission
        String actionRequise = (anneeEnEdition == null) ? "user.creer" : "user.creer";

        boolean autorise = ControleAcces.autoriseAction(actionRequise);
        btnEnregistrer.setVisible(autorise);
        btnEnregistrer.setManaged(autorise);

        if (!autorise) {
            // Message informatif si l'utilisateur n'a pas les droits
            errGlobal.setText("Vous n'avez pas la permission de "
                    + (anneeEnEdition == null ? "créer" : "modifier") + " une année.");
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        }
    }

    /* ============================================================
       ACTIONS
       ============================================================ */

    @FXML
    private void handleAnnuler() {
        fermer();
    }

    @FXML
    private void handleEnregistrer() {
        // 1. Vérification de permission (ceinture + bretelles)
        String actionRequise = (anneeEnEdition == null) ? "user.creer" : "user.creer";
        if (!ControleAcces.autoriseAction(actionRequise)) {
            ToastNotification.avertissement(getStage(),
                    "Vous n'avez pas la permission d'effectuer cette action.");
            return;
        }

        // 2. Validation du formulaire
        if (!validerFormulaire()) return;

        // 3. Construire l'entité
        AnneeScolaire annee = (anneeEnEdition == null)
                ? new AnneeScolaire()
                : anneeEnEdition;

        if (anneeEnEdition == null) {
            annee.setId(UUID.randomUUID().toString());
            annee.setIdEcole(idEcoleCourante);
        }

        annee.setLibelle(txtLibelle.getText().trim());
        annee.setDateDebut(dateDebut.getValue());
        annee.setDateFin(dateFin.getValue());
        annee.setEstCourante(chkEstCourante.isSelected());
        annee.setStatut(comboStatut.getSelectionModel().getSelectedItem());

        // 4. Désactiver le bouton pour éviter les doubles clics
        btnEnregistrer.setDisable(true);

        // 5. Appel du service
        if (anneeEnEdition == null) {
            // Création
            anneeService.creerAsync(annee,
                    () -> Platform.runLater(this::onCreationReussie),
                    err -> Platform.runLater(() -> onErreur(err)));
        } else {
            // Édition
            anneeService.modifierAsync(annee,
                    () -> Platform.runLater(this::onEditionReussie),
                    err -> Platform.runLater(() -> onErreur(err)));
        }
    }

    private void onCreationReussie() {
        btnEnregistrer.setDisable(false);

        // Audit
        auditService.tracerActionAsync(
                "STRUCTURE",
                "CREATION_ANNEE",
                "Année créée : " + txtLibelle.getText().trim()
        );

        ToastNotification.succes(getStage(),
                "Année « " + txtLibelle.getText().trim() + " » créée avec succès");

        if (onSucces != null) onSucces.run();
        fermer();
    }

    private void onEditionReussie() {
        btnEnregistrer.setDisable(false);

        // Audit
        auditService.tracerActionAsync(
                "STRUCTURE",
                "MODIFICATION_ANNEE",
                "Année modifiée : " + txtLibelle.getText().trim()
        );

        ToastNotification.succes(getStage(),
                "Année « " + txtLibelle.getText().trim() + " » modifiée avec succès");

        if (onSucces != null) onSucces.run();
        fermer();
    }

    private void onErreur(Throwable err) {
        btnEnregistrer.setDisable(false);
        String msg = (err != null && err.getMessage() != null)
                ? err.getMessage()
                : "Une erreur est survenue";

        // Afficher dans l'erreur globale du formulaire
        errGlobal.setText(msg);
        errGlobal.setVisible(true);
        errGlobal.setManaged(true);

        // Log console
        System.err.println("❌ Erreur enregistrement année : " + msg);
    }

    /* ============================================================
       VALIDATION
       ============================================================ */
    private boolean validerFormulaire() {
        cacherToutesErreurs();
        boolean valide = true;

        // Libellé
        String libelle = txtLibelle.getText();
        if (libelle == null || libelle.isBlank()) {
            afficherErreur(errLibelle, "Le libellé est obligatoire");
            valide = false;
        } else if (!libelle.matches("\\d{4}-\\d{4}")) {
            afficherErreur(errLibelle, "Format attendu : 2025-2026");
            valide = false;
        }

        // Date de début
        if (dateDebut.getValue() == null) {
            afficherErreur(errDateDebut, "La date de début est obligatoire");
            valide = false;
        }

        // Date de fin
        if (dateFin.getValue() == null) {
            afficherErreur(errDateFin, "La date de fin est obligatoire");
            valide = false;
        }

        // Cohérence des dates
        if (dateDebut.getValue() != null && dateFin.getValue() != null) {
            if (!dateFin.getValue().isAfter(dateDebut.getValue())) {
                afficherErreur(errDateFin,
                        "La date de fin doit être postérieure à la date de début");
                valide = false;
            }
        }

        // Statut
        if (comboStatut.getSelectionModel().getSelectedItem() == null) {
            afficherErreur(errGlobal, "Le statut est obligatoire");
            valide = false;
        }

        return valide;
    }

    private void afficherErreur(Label label, String message) {
        if (label == null) return;
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void cacherToutesErreurs() {
        for (Label l : new Label[]{errLibelle, errDateDebut, errDateFin, errGlobal}) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    /* ============================================================
       HELPERS
       ============================================================ */
    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }
}