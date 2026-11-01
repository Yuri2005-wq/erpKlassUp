package org.erpklassup.erpklassup.controllers.actionPage;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.models.Ecole;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.EcoleService;
import org.erpklassup.erpklassup.util.BreadcrumbManager;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller de l'écran École.
 *
 * <p>Fonctionne en création OU en édition selon qu'une école existe déjà
 * en base. Une seule école autorisée.</p>
 */
public class EcoleController implements Initializable {

    /* ============================================================
       FXML — LOGO
       ============================================================ */
    @FXML private ImageView imgLogoApercu;

    /* ============================================================
       FXML — CHAMPS IDENTITÉ
       ============================================================ */
    @FXML private TextField txtNomEcole;
    @FXML private TextField txtAgrement;
    @FXML private TextField txtNumeroArrete;
    @FXML private TextField txtCodeEcole;
    @FXML private TextField txtPromoteur;

    /* ============================================================
       FXML — LOCALISATION
       ============================================================ */
    @FXML private TextField  txtAdresse;
    @FXML private ComboBox<String> comboRegion;
    @FXML private ComboBox<String> comboSousSysteme;
    @FXML private TextField  txtInspectionRef;
    @FXML private TextField  txtDelegationDept;

    /* ============================================================
       FXML — CONTACTS
       ============================================================ */
    @FXML private TextField txtTelephone;
    @FXML private TextField txtTelephoneSecondaire;
    @FXML private TextField txtEmail;
    @FXML private TextField txtSiteWeb;

    /* ==================================

    ==========================
       FXML — DIVERS
       ============================================================ */
    @FXML private TextField txtDevise;
    @FXML private Label     labelMessageStatut;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final EcoleService ecoleService = new EcoleService();
    private final AuditService auditService = new AuditService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private Ecole ecoleEnEdition;
    private File logoSelectionne;

    /* Régions du Cameroun */
    private static final List<String> REGIONS = List.of(
            "Adamaoua", "Centre", "Est", "Extrême-Nord",
            "Littoral", "Nord", "Nord-Ouest", "Ouest", "Sud", "Sud-Ouest"
    );

    private static final List<String> SOUS_SYSTEMES = List.of(
            "FRANCOPHONE", "ANGLOPHONE", "BILINGUE"
    );

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerCombos();
        chargerEcoleExistante();
        configNomHeader();
    }

    private void configNomHeader(){
        BreadcrumbManager bc = BreadcrumbManager.getInstance();
        bc.definirSection("Configuration infos Ecole");
    }
    private void configurerCombos() {
        comboRegion.setItems(FXCollections.observableArrayList(REGIONS));
        comboSousSysteme.setItems(FXCollections.observableArrayList(SOUS_SYSTEMES));

        // Cellules avec placeholder
        comboRegion.setPromptText("Sélectionner la région");
        comboSousSysteme.setPromptText("Sélectionner le sous-système");
    }

    /* ============================================================
       CHARGEMENT DE L'ÉCOLE
       ============================================================ */
    private void chargerEcoleExistante() {
        ecoleService.chargerEcoleAsync(
                optEcole -> Platform.runLater(() -> {
                    if (optEcole.isPresent()) {
                        this.ecoleEnEdition = optEcole.get();
                        remplirFormulaire(this.ecoleEnEdition);
                        afficherModeEdition();
                    } else {
                        this.ecoleEnEdition = new Ecole();
                        afficherModeCreation();
                    }
                    appliquerControlesAcces();
                }),
                err -> Platform.runLater(() -> {
                    ToastNotification.erreur(getStage(),
                            "Erreur de chargement : " + err.getMessage());
                    afficherModeCreation();
                })
        );
    }


    @FXML  void handleAnnuler(){

    }

    private void afficherModeCreation() {
        labelMessageStatut.setText("Aucune école enregistrée. Créez votre établissement.");
        labelMessageStatut.setStyle("-fx-text-fill: #10B981;");
    }

    private void afficherModeEdition() {
        labelMessageStatut.setText("École existante. Modifiez les informations !.");
        labelMessageStatut.setStyle("-fx-text-fill: #16A34A;");
    }

    /* ============================================================
       REMPLISSAGE DU FORMULAIRE
       ============================================================ */
    private void remplirFormulaire(Ecole e) {
        txtNomEcole.setText(e.getNomEcole());
        txtAgrement.setText(e.getNumeroAgrement());
        txtNumeroArrete.setText(e.getNumeroArreteOuverture());
        txtCodeEcole.setText(e.getCodeEcole());
        txtPromoteur.setText(e.getNomPromoteur());

        txtAdresse.setText(e.getAdresse());
        comboRegion.getSelectionModel().select(e.getRegion());
        comboSousSysteme.getSelectionModel().select(e.getSousSysteme());
        txtInspectionRef.setText(e.getInspectionRef());
        txtDelegationDept.setText(e.getDelegationDept());

        txtTelephone.setText(e.getTelephone());
        // Le modèle ne stocke pas le téléphone secondaire → ignore
        txtEmail.setText(e.getEmail());
        // Le modèle ne stocke pas le site web → ignore

        txtDevise.setText(e.getDevise());

        // Logo
        if (e.getLogoPath() != null && !e.getLogoPath().isBlank()) {
            try {
                Image img = new Image("file:" + e.getLogoPath());
                imgLogoApercu.setImage(img);
            } catch (Exception ex) {
                System.err.println("⚠️ Logo introuvable : " + e.getLogoPath());
            }
        }
    }

    /* ============================================================
       LOGO
       ============================================================ */
    @FXML
    private void handleChoisirLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir le logo de l'école");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File fichier = chooser.showOpenDialog(getStage());
        if (fichier == null) return;

        this.logoSelectionne = fichier;
        try {
            Image img = new Image(fichier.toURI().toString());
            imgLogoApercu.setImage(img);
        } catch (Exception e) {
            ToastNotification.erreur(getStage(), "Impossible de charger l'image.");
        }
    }

    /* ============================================================
       ENREGISTREMENT
       ============================================================ */
    @FXML
    private void handleEnregistrerInfos() {
        boolean estCreation = (ecoleEnEdition == null || ecoleEnEdition.getIdEcole() == null);

        // 1. Vérification de permission
        String action = estCreation ? "ecole.creer" : "ecole.modifier";
        if (!ControleAcces.autoriseAction(action)) {
            ToastNotification.avertissement(getStage(), "Permission refusée");
            return;
        }

        // 2. Validation
        if (!validerFormulaire()) return;

        // 3. Construire l'école
        Ecole e = (ecoleEnEdition != null) ? ecoleEnEdition : new Ecole();
        e.setNomEcole(txtNomEcole.getText().trim());
        e.setNumeroAgrement(txtAgrement.getText().trim());
        e.setNumeroArreteOuverture(texte(txtNumeroArrete));
        e.setCodeEcole(texte(txtCodeEcole));
        e.setNomPromoteur(texte(txtPromoteur));
        e.setAdresse(txtAdresse.getText().trim());
        e.setRegion(comboRegion.getSelectionModel().getSelectedItem());
        e.setSousSysteme(comboSousSysteme.getSelectionModel().getSelectedItem());
        e.setInspectionRef(texte(txtInspectionRef));
        e.setDelegationDept(texte(txtDelegationDept));
        e.setTelephone(txtTelephone.getText().trim());
        e.setEmail(texte(txtEmail));
        e.setDevise(texte(txtDevise));

        if (logoSelectionne != null) {
            e.setLogoPath(logoSelectionne.getAbsolutePath());
        }

        // 4. Sauvegarder
        ecoleService.sauvegarderAsync(e,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync("PARAMETRAGE",
                            estCreation ? "CREATION_ECOLE" : "MODIFICATION_ECOLE",
                            "École : " + e.getNomEcole());

                    ToastNotification.succes(getStage(),
                            estCreation ? "École créée avec succès." : "École mise à jour.");

                    // Recharger pour rafraîchir l'état (nouvel ID par ex.)
                    chargerEcoleExistante();
                }),
                err -> Platform.runLater(() -> {
                    ToastNotification.erreur(getStage(),
                            err.getMessage() != null ? err.getMessage() : "Erreur d'enregistrement.");
                })
        );
    }

    /* ============================================================
       RÉINITIALISER
       ============================================================ */
    @FXML
    private void handleReinitialiser() {
        if (ecoleEnEdition != null && ecoleEnEdition.getIdEcole() != null) {
            // En édition : revenir aux valeurs en base
            remplirFormulaire(ecoleEnEdition);
        } else {
            // En création : vider
            viderFormulaire();
        }
        logoSelectionne = null;
    }

    private void viderFormulaire() {
        txtNomEcole.clear();
        txtAgrement.clear();
        txtNumeroArrete.clear();
        txtCodeEcole.clear();
        txtPromoteur.clear();
        txtAdresse.clear();
        comboRegion.getSelectionModel().clearSelection();
        comboSousSysteme.getSelectionModel().clearSelection();
        txtInspectionRef.clear();
        txtDelegationDept.clear();
        txtTelephone.clear();
        txtTelephoneSecondaire.clear();
        txtEmail.clear();
        txtSiteWeb.clear();
        txtDevise.clear();
        imgLogoApercu.setImage(null);
    }

    /* ============================================================
       VALIDATION
       ============================================================ */
    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();

        if (estVide(txtNomEcole.getText()))   erreurs.append("• Le nom est obligatoire.\n");
        if (estVide(txtAgrement.getText()))   erreurs.append("• Le numéro d'agrément est obligatoire.\n");
        if (estVide(txtAdresse.getText()))    erreurs.append("• L'adresse est obligatoire.\n");
        if (comboRegion.getSelectionModel().getSelectedItem() == null)
            erreurs.append("• La région est obligatoire.\n");
        if (comboSousSysteme.getSelectionModel().getSelectedItem() == null)
            erreurs.append("• Le sous-système est obligatoire.\n");
        if (estVide(txtTelephone.getText()))  erreurs.append("• Le téléphone est obligatoire.\n");

        if (erreurs.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(getStage());
            alert.setTitle("Champs obligatoires");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(erreurs.toString());
            alert.showAndWait();
            return false;
        }
        return true;
    }

    /* ============================================================
       PERMISSIONS
       ============================================================ */
    private void appliquerControlesAcces() {
        boolean estCreation = (ecoleEnEdition == null || ecoleEnEdition.getIdEcole() == null);
        String action = estCreation ? "ecole.creer" : "ecole.modifier";

        // On pourrait désactiver des champs selon la permission
        // Pour l'instant, seule la vérification au clic suffit
    }

    /* ============================================================
       HELPERS
       ============================================================ */
    private String texte(TextField field) {
        return (field != null && field.getText() != null) ? field.getText().trim() : "";
    }

    private boolean estVide(String s) {
        return s == null || s.isBlank();
    }

    private Stage getStage() {
        return (Stage) txtNomEcole.getScene().getWindow();
    }
}