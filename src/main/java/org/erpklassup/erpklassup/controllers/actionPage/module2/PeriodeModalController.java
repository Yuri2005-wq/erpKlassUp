package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.models.module2.PeriodeEvaluation;
import org.erpklassup.erpklassup.service.module2.PeriodeEvaluationService;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.erpklassup.erpklassup.util.ValidationUtil;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class PeriodeModalController implements Initializable {

    /* ═══════════════ FXML ═══════════════ */
    @FXML private FontIcon iconModal;
    @FXML private Label    lblTitreModal;
    @FXML private Label    lblSousTitreModal;

    @FXML private javafx.scene.layout.VBox boxType;
    @FXML private ToggleButton btnTypeTrimestre;
    @FXML private ToggleButton btnTypeSequence;

    @FXML private TextField champCode;
    @FXML private Label     errCode;
    @FXML private TextField champOrdre;
    @FXML private Label     errOrdre;
    @FXML private TextField champLibelle;
    @FXML private Label     errLibelle;

    @FXML private DatePicker champDateDebut;
    @FXML private Label      errDateDebut;
    @FXML private DatePicker champDateFin;
    @FXML private Label      errDateFin;

    @FXML private javafx.scene.layout.VBox boxPoids;
    @FXML private TextField champPoids;
    @FXML private Label     errPoids;

    @FXML private javafx.scene.layout.VBox boxStatut;
    @FXML private FontIcon icoStatut;
    @FXML private Label    lblStatutTitre;
    @FXML private Label    lblStatutDesc;
    @FXML private Button   btnBasculerStatut;

    @FXML private Label  errGlobal;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ═══════════════ SERVICES ═══════════════ */
    private final PeriodeEvaluationService service = new PeriodeEvaluationService();

    /* ═══════════════ ÉTAT ═══════════════ */
    private String  idEcole;
    private String  idAnnee;
    private PeriodeEvaluation periodeExistante;
    private String  idParent;
    private Runnable onSuccess;

    private boolean estSequence;

    /* ═══════════════ INITIALISATION ═══════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerToggles();
        configurerChamps();
        cacherErreurs();
        Platform.runLater(champLibelle::requestFocus);
    }

    private void configurerToggles() {
        ToggleGroup group = new ToggleGroup();
        btnTypeTrimestre.setToggleGroup(group);
        btnTypeSequence.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, anc, nouv) -> {
            if (nouv == null) { group.selectToggle(anc); return; }
            estSequence = (nouv == btnTypeSequence);
            appliquerModeType();
        });
        btnTypeTrimestre.setSelected(true);
        estSequence = false;
    }

    private void configurerChamps() {
        // Ordre : uniquement numérique
        ValidationUtil.forcerEntier(champOrdre);
        ValidationUtil.forcerDecimal(champPoids);
    }

    private void appliquerModeType() {
        boolean afficherPoids = estSequence;
        boxPoids.setVisible(afficherPoids);
        boxPoids.setManaged(afficherPoids);
    }

    /* ═══════════════ API PUBLIQUE ═══════════════ */
    public void initData(String idEcole, String idAnnee,
                         PeriodeEvaluation periodeExistante,
                         String idParent,
                         Runnable onSuccess) {
        this.idEcole          = idEcole;
        this.idAnnee          = idAnnee;
        this.periodeExistante = periodeExistante;
        this.idParent         = idParent;
        this.onSuccess        = onSuccess;

        boolean estCreation = (periodeExistante == null);
        boolean sousPeriode = idParent != null
                || (periodeExistante != null && periodeExistante.getIdPeriodeParent() != null);

        estSequence = sousPeriode;
        if (estSequence) btnTypeSequence.setSelected(true);
        else             btnTypeTrimestre.setSelected(true);
        appliquerModeType();

        // Verrouille le type si la période a déjà des enfants (en édition)
        boolean aDesEnfants = periodeExistante != null && periodeExistante.aDesSousPeriodes();
        btnTypeTrimestre.setDisable(aDesEnfants);
        btnTypeSequence.setDisable(aDesEnfants);

        if (estCreation) {
            configurerPourCreation(sousPeriode);
        } else {
            configurerPourEdition(periodeExistante);
        }
    }

    private void configurerPourCreation(boolean sousPeriode) {
        lblTitreModal.setText(sousPeriode ? "Nouvelle sous-période" : "Nouvelle période") ;
        lblSousTitreModal.setText(sousPeriode
                ? "Renseignez les informations de la sous-période"
                : "Renseignez les informations de la période");
        iconModal.setIconLiteral(sousPeriode ? "fth-file-plus" : "fth-calendar");

        champLibelle.clear();
        champCode.clear();
        champOrdre.clear();
        champDateDebut.setValue(null);
        champDateFin.setValue(null);
        champPoids.setText("1.00");

        boxStatut.setVisible(false);
        boxStatut.setManaged(false);
        btnSupprimer.setVisible(false);
        btnSupprimer.setManaged(false);
    }

    private void configurerPourEdition(PeriodeEvaluation p) {
        lblTitreModal.setText("Modifier la période");
        lblSousTitreModal.setText(p.getCodePeriode() != null
                ? "Code : " + p.getCodePeriode()
                : "Renseignez les informations de la période");
        iconModal.setIconLiteral("fth-edit-3");

        champLibelle.setText(p.getLibelle());
        champCode.setText(p.getCodePeriode());
        champOrdre.setText(String.valueOf(p.getNumeroOrdre()));
        champDateDebut.setValue(p.getDateDebut());
        champDateFin.setValue(p.getDateFin());
        champPoids.setText(p.getPoids() != null ? p.getPoids().toPlainString() : "1.00");

        boxStatut.setVisible(true);
        boxStatut.setManaged(true);
        btnSupprimer.setVisible(true);
        btnSupprimer.setManaged(true);
        btnSupprimer.setDisable(p.aDesSousPeriodes());
        majStatutUI(p);
    }

    private void majStatutUI(PeriodeEvaluation p) {
        boolean cloturee = p.isEstCloturee();
        icoStatut.setIconLiteral(cloturee ? "fth-lock" : "fth-unlock");
        icoStatut.setStyle("-fx-icon-size: 20px; -fx-icon-color: "
                + (cloturee ? "#E5484D;" : "#10B981;"));
        lblStatutTitre.setText(cloturee ? "Période clôturée" : "Période ouverte");
        lblStatutDesc.setText(cloturee
                ? "Aucune note ne peut plus être saisie."
                : "La saisie des notes est autorisée.");
        btnBasculerStatut.setText(cloturee ? "Réouvrir" : "Clôturer");
    }

    /* ═══════════════ ACTIONS ═══════════════ */
    @FXML
    private void handleAnnuler() {
        fermer();
    }

    @FXML
    private void handleEnregistrer() {
        if (!validerFormulaire()) return;

        PeriodeEvaluation p = (periodeExistante != null) ? periodeExistante : new PeriodeEvaluation();

        p.setIdAnnee(idAnnee);
        p.setIdEcole(idEcole);
        p.setLibelle(champLibelle.getText().trim());
        p.setCodePeriode(blank(champCode.getText()) ? null : champCode.getText().trim());
        p.setDateDebut(champDateDebut.getValue());
        p.setDateFin(champDateFin.getValue());
        p.setIdPeriodeParent(idParent);

        if (!blank(champOrdre.getText())) {
            try { p.setNumeroOrdre(Integer.parseInt(champOrdre.getText().trim())); }
            catch (NumberFormatException ignored) {}
        }

        try {
            p.setPoids(blank(champPoids.getText())
                    ? BigDecimal.ONE
                    : new BigDecimal(champPoids.getText().trim().replace(",", ".")));
        } catch (NumberFormatException ex) {
            afficherErreur(errPoids, "Poids invalide.");
            return;
        }

        boolean estCreation = (periodeExistante == null);
        if (estCreation && (p.getId() == null || p.getId().isBlank())) {
            p.setId(java.util.UUID.randomUUID().toString());
        }

        btnEnregistrer.setDisable(true);
        btnAnnuler.setDisable(true);
        cacherErreurs();

        Runnable onSucces = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            btnAnnuler.setDisable(false);
            ToastNotification.succes(getStage(),
                    estCreation ? "Période créée." : "Période mise à jour.");
            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onErreur = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            btnAnnuler.setDisable(false);
            gererErreurEnregistrement(err);
        });

        if (estCreation) {
            service.creerAsync(p, idEcole, onSucces, onErreur);
        } else {
            service.modifierAsync(p, onSucces, onErreur);
        }
    }

    @FXML
    private void handleBasculerStatut() {
        if (periodeExistante == null || periodeExistante.getId() == null) return;

        boolean veutCloturer = !periodeExistante.isEstCloturee();
        String titre = veutCloturer ? "Clôturer la période" : "Réouvrir la période";
        String msg = veutCloturer
                ? "Clôturer \"" + periodeExistante.getLibelle() + "\" ? Aucune note ne pourra plus être saisie."
                : "Réouvrir \"" + periodeExistante.getLibelle() + "\" ?";

        Boolean ok = AlertUtil.afficherConfirmation(titre, msg, getStage());
        if (!Boolean.TRUE.equals(ok)) return;

        Runnable succes = () -> Platform.runLater(() -> {
            ToastNotification.succes(getStage(),
                    veutCloturer ? "Période clôturée." : "Période réouverte.");
            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> err = e -> Platform.runLater(() ->
                ToastNotification.erreur(getStage(), e.getMessage()));

        if (veutCloturer) service.cloturerAsync(periodeExistante, succes, err);
        else              service.reouvrirAsync(periodeExistante, succes, err);
    }

    @FXML
    private void handleSupprimer() {
        if (periodeExistante == null || periodeExistante.getId() == null) return;

        Boolean ok = AlertUtil.afficherConfirmation("Supprimer la période",
                "Supprimer définitivement \"" + periodeExistante.getLibelle() + "\" ?", getStage());
        if (!Boolean.TRUE.equals(ok)) return;

        service.supprimerAsync(periodeExistante,
                () -> Platform.runLater(() -> {
                    ToastNotification.succes(getStage(), "Période supprimée.");
                    if (onSuccess != null) onSuccess.run();
                    fermer();
                }),
                e -> Platform.runLater(() -> ToastNotification.erreur(getStage(), e.getMessage())));
    }

    /* ═══════════════ VALIDATION ═══════════════ */
    private boolean validerFormulaire() {
        cacherErreurs();
        boolean ok = true;

        if (blank(champCode.getText())) {
            afficherErreur(errCode, "Le code est obligatoire.");
            ok = false;
        }
        if (blank(champLibelle.getText())) {
            afficherErreur(errLibelle, "Le libellé est obligatoire.");
            ok = false;
        }
        if (champDateDebut.getValue() == null) {
            afficherErreur(errDateDebut, "La date de début est obligatoire.");
            ok = false;
        }
        if (champDateFin.getValue() == null) {
            afficherErreur(errDateFin, "La date de fin est obligatoire.");
            ok = false;
        }
        if (champDateDebut.getValue() != null && champDateFin.getValue() != null
                && !champDateFin.getValue().isAfter(champDateDebut.getValue())) {
            afficherErreur(errDateFin, "La date de fin doit être après la date de début.");
            ok = false;
        }
        if (idAnnee == null) {
            afficherErreurGlobale("Aucune année scolaire active — impossible d'enregistrer.");
            ok = false;
        }
        return ok;
    }

    private void gererErreurEnregistrement(Throwable erreur) {
        System.err.println("❌ Erreur enregistrement période : "
                + (erreur != null ? erreur.getMessage() : "?"));
        if (erreur != null) erreur.printStackTrace();

        String msg = (erreur != null && erreur.getMessage() != null)
                ? erreur.getMessage()
                : "Échec de l'enregistrement de la période.";

        if (msg.toLowerCase().contains("existe")) afficherErreur(errCode, msg);
        else                                       afficherErreurGlobale(msg);
    }

    /* ═══════════════ AFFICHAGE ERREURS ═══════════════ */
    private void afficherErreur(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void afficherErreurGlobale(String msg) {
        if (errGlobal == null) return;
        errGlobal.setText(msg);
        errGlobal.setVisible(true);
        errGlobal.setManaged(true);
    }

    private void cacherErreurs() {
        for (Label l : new Label[]{errCode, errOrdre, errLibelle,
                errDateDebut, errDateFin, errPoids, errGlobal}) {
            if (l != null) { l.setVisible(false); l.setManaged(false); }
        }
    }

    /* ═══════════════ FERMETURE ═══════════════ */
    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler == null || btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}