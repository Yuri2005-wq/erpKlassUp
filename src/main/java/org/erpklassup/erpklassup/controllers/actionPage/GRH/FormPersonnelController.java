package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.enums.LieuNaissance;
import org.erpklassup.erpklassup.enums.Sexe;
import org.erpklassup.erpklassup.enums.StatutEmploi;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.models.GRH.Personnel;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.GRH.PersonnelService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class FormPersonnelController implements Initializable, VueDisposable {

    /* ============================================================
       INJECTION DES COMPOSANTS FXML
       ============================================================ */
    @FXML private HBox bandeauMessage;
    @FXML private Label lblMessage;

    // En-tête principal
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtPosteFonction;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private ComboBox<String> comboTypePersonnel;
    @FXML private ComboBox<Departement> comboDepartement;
    @FXML private ComboBox<Personnel> comboResponsable;

    // Photo
    @FXML private ImageView imgPhoto;
    @FXML private VBox photoPlaceholder;

    // Onglet 1 : Informations professionnelles
    @FXML private TextField txtMatricule;
    @FXML private TextField txtMatriculeFp;
    @FXML private TextField txtQualification;
    @FXML private ComboBox<String> comboStatutContractuel;

    @FXML private TextField txtQuota;
    @FXML private TextField txtDecharge;
    @FXML private DatePicker dateEmbauche;
    @FXML private DatePicker dateDepart;
    @FXML private TextField txtMotifDepart;

    // Organigramme (Structure Odoo)
    @FXML private Label lblOrgNbSubordonnes;
    @FXML private VBox orgBoxResponsable;
    @FXML private ImageView imgOrgResponsable;
    @FXML private Label lblOrgResponsableInitiales;
    @FXML private Label lblOrgResponsableNom;
    @FXML private Label lblOrgResponsablePoste;

    @FXML private ImageView imgOrgEmploye;
    @FXML private Label lblOrgEmployeInitiales;
    @FXML private Label lblOrgEmployeNom;
    @FXML private Label lblOrgEmployePoste;
    @FXML private Label lblOrgSansResponsable;

    @FXML private Region orgLienSubordonnes;
    @FXML private Button btnOrgSubordonnes;
    @FXML private VBox orgBoxSubordonnes;
    @FXML private FlowPane flowSubordonnes;

    // Onglet 2 : Informations privées
    @FXML private ComboBox<Sexe> comboSexe;
    @FXML private DatePicker dateNaissance;
    @FXML private TextField txtLieuNaissance;
    @FXML private TextField txtNationalite;
    @FXML private ComboBox<String> comboSituation;
    @FXML private TextField txtEnfants;

    @FXML private TextField txtCni;
    @FXML private TextField txtCnps;
    @FXML private TextField txtNiu;

    @FXML private TextField txtAdresse;
    @FXML private TextField txtVille;
    @FXML private TextField txtUrgenceNom;
    @FXML private TextField txtUrgenceTel;

    // Onglet 3 : Paie
    @FXML private ComboBox<String> comboModePaiement;
    @FXML private GridPane gridBanque;
    @FXML private TextField txtBanque;
    @FXML private TextField txtIban;
    @FXML private GridPane gridMobile;
    @FXML private TextField txtMobileMoney;

    // Onglet 4 : Paramètres RH
    @FXML private ComboBox<Utilisateur> comboUtilisateur;
    @FXML private ComboBox<StatutEmploi> comboStatutEmploi;

    // Boutons de validation
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ============================================================
       ATTRIBUTS DU CONTRÔLEUR ET ÉTAT
       ============================================================ */
    private final PersonnelService personnelService = new PersonnelService();
    private final AuditService auditService = new AuditService();

    private String idEcoleCourante;
    private Personnel personnelEnEdition; // null si création
    private Runnable onSucces;
    private File fichierPhotoChoisi = null;
    private boolean subordonnesAffiches = false;

    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        configurerChampsEtValidations();
        configurerCombos();
        configurerEcouteursOrganigramme();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);

        Platform.runLater(() -> txtNom.requestFocus());
    }

    @Override
    public void disposer() {
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    /* ============================================================
       API PUBLIQUE — MODALE (Pattern Standard ERPKlassUp)
       ============================================================ */

    /**
     * Initialise la modale en mode CRÉATION.
     */
    public void initCreation(String idEcole, Runnable onSucces) {
        this.idEcoleCourante = idEcole;
        this.personnelEnEdition = null;
        this.onSucces = onSucces;

        initialiserFormulaireVide();
        chargerDonneesRef();
        rafraichirVueOrganigramme();
        appliquerControlesAcces();
    }

    /**
     * Initialise la modale en mode ÉDITION.
     */
    public void initEdition(Personnel personnel, Runnable onSucces) {
        if (personnel == null) return;

        this.idEcoleCourante = personnel.getIdEcole();
        this.personnelEnEdition = personnel;
        this.onSucces = onSucces;

        remplirChamps(personnel);
        chargerDonneesRef();
        chargerSubordonnesEtRendreOrganigramme();
        appliquerControlesAcces();
    }

    /* ============================================================
       CONTRÔLE D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
        String actionRequise = (personnelEnEdition == null) ? "personnel.creer" : "personnel.modifier";
        boolean autorise = ControleAcces.autoriseAction(actionRequise);

        btnEnregistrer.setVisible(autorise);
        btnEnregistrer.setManaged(autorise);

        if (!autorise) {
            afficherErreur("Vous n'avez pas la permission d'effectuer cette action.");
        }
    }

    /* ============================================================
       CONFIGURATION DES CHAMPS & VALIDATIONS
       ============================================================ */
    private void configurerChampsEtValidations() {
        ValidationUtil.trimAuBlur(txtNom);
        ValidationUtil.trimAuBlur(txtPrenom);
        ValidationUtil.trimAuBlur(txtEmail);

        ValidationUtil.forcerMajuscules(txtMatricule);
        ValidationUtil.forcerMajuscules(txtMatriculeFp);
        ValidationUtil.forcerMajuscules(txtCni);
        ValidationUtil.forcerMajuscules(txtNiu);

        ValidationUtil.forcerDecimal(txtQuota);
        ValidationUtil.forcerDecimal(txtDecharge);
        ValidationUtil.forcerEntier(txtEnfants);

        // Gestion conditionnelle des modes de paiement
        comboModePaiement.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean estBanque = "VIREMENT".equalsIgnoreCase(newVal) || "CHEQUE".equalsIgnoreCase(newVal);
            boolean estMobile = "MOBILE_MONEY".equalsIgnoreCase(newVal);

            gridBanque.setVisible(estBanque);
            gridBanque.setManaged(estBanque);

            gridMobile.setVisible(estMobile);
            gridMobile.setManaged(estMobile);
        });
    }

    private void configurerCombos() {
        comboTypePersonnel.setItems(FXCollections.observableArrayList(
                "ENSEIGNANT", "ADMINISTRATIF", "SUPPORT", "VACATAIRE", "DIRECTION"
        ));

        comboStatutContractuel.setItems(FXCollections.observableArrayList(
                "PERMANENT", "CDD", "CDI", "VACATAIRE", "STAGIAIRE", "PRESTATAIRE"
        ));

        comboSexe.setItems(FXCollections.observableArrayList(Sexe.values()));
        comboSexe.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Sexe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLibelle());
            }
        });
        comboSexe.setButtonCell(comboSexe.getCellFactory().call(null));

        comboSituation.setItems(FXCollections.observableArrayList(
                "CELIBATAIRE", "MARIE(E)", "DIVORCE(E)", "VEUF(VE)"
        ));

        comboModePaiement.setItems(FXCollections.observableArrayList(
                "ESPECES", "VIREMENT", "MOBILE_MONEY", "CHEQUE"
        ));

        comboStatutEmploi.setItems(FXCollections.observableArrayList(StatutEmploi.values()));
        comboStatutEmploi.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StatutEmploi item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLibelle());
            }
        });
        comboStatutEmploi.setButtonCell(comboStatutEmploi.getCellFactory().call(null));
    }

    private void chargerDonneesRef() {
        if (idEcoleCourante == null) return;
        personnelService.chargerParEcoleAsync(idEcoleCourante,
                liste -> Platform.runLater(() -> {
                    ObservableList<Personnel> obs = FXCollections.observableArrayList(liste);
                    if (personnelEnEdition != null && personnelEnEdition.getIdPersonnel() != null) {
                        obs.removeIf(p -> p.getIdPersonnel().equals(personnelEnEdition.getIdPersonnel()));
                    }
                    comboResponsable.setItems(obs);
                }),
                err -> {}
        );
    }

    /* ============================================================
       ORGANIGRAMME PATTERN ODOO
       ============================================================ */
    private void configurerEcouteursOrganigramme() {
        txtNom.textProperty().addListener((o, a, n) -> rafraichirVueOrganigramme());
        txtPrenom.textProperty().addListener((o, a, n) -> rafraichirVueOrganigramme());
        txtPosteFonction.textProperty().addListener((o, a, n) -> rafraichirVueOrganigramme());

        comboResponsable.valueProperty().addListener((obs, oldVal, newVal) -> rafraichirVueOrganigramme());
    }

    private void chargerSubordonnesAndRendreOrganigramme(String idPersonnel) {
        personnelService.chargerSubordonnesAsync(idPersonnel,
                subordonnes -> Platform.runLater(() -> {
                    flowSubordonnes.getChildren().clear();

                    int total = subordonnes.size();
                    lblOrgNbSubordonnes.setText(total == 0
                            ? "Aucun subordonné direct"
                            : total + " subordonné(s) direct(s)");

                    boolean aDesSubordonnes = total > 0;
                    btnOrgSubordonnes.setVisible(aDesSubordonnes);
                    btnOrgSubordonnes.setManaged(aDesSubordonnes);
                    orgLienSubordonnes.setVisible(aDesSubordonnes);
                    orgLienSubordonnes.setManaged(aDesSubordonnes);

                    for (Personnel sub : subordonnes) {
                        HBox carteSub = creerCarteOrganigramme(
                                sub.getPhoto(),
                                sub.getNomComplet(),
                                sub.getPosteOuFonction(),
                                "#F8FAFC",
                                "#E2E8F0"
                        );
                        flowSubordonnes.getChildren().add(carteSub);
                    }

                    rafraichirVueOrganigramme();
                }),
                err -> rafraichirVueOrganigramme()
        );
    }

    private void chargerSubordonnesEtRendreOrganigramme() {
        if (personnelEnEdition != null && personnelEnEdition.getIdPersonnel() != null) {
            chargerSubordonnesAndRendreOrganigramme(personnelEnEdition.getIdPersonnel());
        } else {
            rafraichirVueOrganigramme();
        }
    }

    private void rafraichirVueOrganigramme() {
        String nomComplet = (txtNom.getText() + " " + txtPrenom.getText()).trim();
        lblOrgEmployeNom.setText(nomComplet.isEmpty() ? "Nouvel Employé" : nomComplet);
        lblOrgEmployePoste.setText(txtPosteFonction.getText().isBlank() ? "Poste non défini" : txtPosteFonction.getText());
        lblOrgEmployeInitiales.setText(extraireInitiales(txtNom.getText(), txtPrenom.getText()));

        if (fichierPhotoChoisi != null) {
            try {
                imgOrgEmploye.setImage(new Image(new FileInputStream(fichierPhotoChoisi)));
                imgOrgEmploye.setVisible(true);
            } catch (Exception ignored) {}
        } else if (personnelEnEdition != null && personnelEnEdition.getPhoto() != null) {
            imgOrgEmploye.setImage(new Image(personnelEnEdition.getPhoto(), true));
            imgOrgEmploye.setVisible(true);
        } else {
            imgOrgEmploye.setVisible(false);
        }

        Personnel resp = comboResponsable.getValue();
        if (resp != null) {
            orgBoxResponsable.setVisible(true);
            orgBoxResponsable.setManaged(true);
            lblOrgSansResponsable.setVisible(false);
            lblOrgSansResponsable.setManaged(false);

            lblOrgResponsableNom.setText(resp.getNomComplet());
            lblOrgResponsablePoste.setText(resp.getPosteOuFonction() != null ? resp.getPosteOuFonction() : "");
            lblOrgResponsableInitiales.setText(extraireInitiales(resp.getNom(), resp.getPrenom()));

            if (resp.getPhoto() != null) {
                imgOrgResponsable.setImage(new Image(resp.getPhoto(), true));
                imgOrgResponsable.setVisible(true);
            } else {
                imgOrgResponsable.setVisible(false);
            }
        } else {
            orgBoxResponsable.setVisible(false);
            orgBoxResponsable.setManaged(false);
            lblOrgSansResponsable.setVisible(true);
            lblOrgSansResponsable.setManaged(true);
        }
    }

    private String extraireInitiales(String nom, String prenom) {
        StringBuilder sb = new StringBuilder();
        if (prenom != null && !prenom.isBlank()) sb.append(Character.toUpperCase(prenom.charAt(0)));
        if (nom != null && !nom.isBlank()) sb.append(Character.toUpperCase(nom.charAt(0)));
        return !sb.isEmpty() ? sb.toString() : "?";
    }

    private HBox creerCarteOrganigramme(String photoPath, String nom, String poste, String bgHex, String borderHex) {
        HBox carte = new HBox(12);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPrefWidth(300);
        carte.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-radius: 8; -fx-padding: 8 10;", bgHex, borderHex));

        StackPane avatarPane = new StackPane();
        avatarPane.setStyle("-fx-background-color: #E2E8F0; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40;");

        Label init = new Label(extraireInitiales(nom, ""));
        init.setStyle("-fx-font-weight: 800; -fx-text-fill: #334155; -fx-font-size: 13px;");
        avatarPane.getChildren().add(init);

        if (photoPath != null && !photoPath.isBlank()) {
            ImageView img = new ImageView(new Image(photoPath, true));
            img.setFitWidth(40);
            img.setFitHeight(40);
            img.setPreserveRatio(true);
            avatarPane.getChildren().add(img);
        }

        VBox info = new VBox(2);
        Label lblN = new Label(nom);
        lblN.setStyle("-fx-font-weight: 700; -fx-text-fill: #16213A; -fx-font-size: 12px;");
        Label lblP = new Label(poste != null ? poste : "");
        lblP.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        info.getChildren().addAll(lblN, lblP);

        carte.getChildren().addAll(avatarPane, info);
        return carte;
    }

    @FXML
    private void handleToggleSubordonnes(ActionEvent event) {
        subordonnesAffiches = !subordonnesAffiches;
        orgBoxSubordonnes.setVisible(subordonnesAffiches);
        orgBoxSubordonnes.setManaged(subordonnesAffiches);
        btnOrgSubordonnes.setText(subordonnesAffiches ? "Masquer les subordonnés" : "Afficher les subordonnés");
    }

    /* ============================================================
       GESTION DES ACTIONS & ENREGISTREMENT
       ============================================================ */
    @FXML
    private void handleChoisirPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner la photo du membre");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            this.fichierPhotoChoisi = file;
            try {
                imgPhoto.setImage(new Image(new FileInputStream(file)));
                photoPlaceholder.setVisible(false);
            } catch (Exception e) {
                afficherErreur("Chargement de l'image échoué.");
            }
            rafraichirVueOrganigramme();
        }
    }

    @FXML
    private void handleEnregistrer(ActionEvent event) {
        masquerMessage();

        if (!validerChamps()) {
            return;
        }

        Personnel personnel = (personnelEnEdition == null) ? new Personnel() : personnelEnEdition;
        mapparationFormulaireVersObjet(personnel);

        btnEnregistrer.setDisable(true);

        if (personnelEnEdition == null) {
            String idCreateur = SessionManager.getInstance().getIdUtilisateurCourant();
            personnelService.creerAsync(personnel, idEcoleCourante, idCreateur,
                    () -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        auditService.tracerActionAsync("GRH", "CREATION_PERSONNEL", "Personnel créé : " + personnel.getNomComplet());
                        ToastNotification.succes(getStage(), "Personnel enregistré avec succès.");
                        if (onSucces != null) onSucces.run();
                        fermer();
                    }),
                    err -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        afficherErreur(err.getMessage());
                    })
            );
        } else {
            personnelService.modifierAsync(personnel,
                    () -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        auditService.tracerActionAsync("GRH", "MODIFICATION_PERSONNEL", "Personnel modifié : " + personnel.getNomComplet());
                        ToastNotification.succes(getStage(), "Personnel mis à jour avec succès.");
                        if (onSucces != null) onSucces.run();
                        fermer();
                    }),
                    err -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        afficherErreur(err.getMessage());
                    })
            );
        }
    }

    @FXML
    private void handleAnnuler(ActionEvent event) {
        fermer();
    }

    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }

    /* ============================================================
       MAPPING ET VALIDATIONS
       ============================================================ */
    private boolean validerChamps() {
        if (!ValidationUtil.verifierNonVide(txtNom, "Nom", getStage())) return false;
        if (!ValidationUtil.verifierNonVide(txtPosteFonction, "Intitulé du poste", getStage())) return false;
        if (!ValidationUtil.verifierNonVide(txtMatricule, "Matricule interne", getStage())) return false;

        if (comboTypePersonnel.getValue() == null) {
            afficherErreur("Le type de personnel est obligatoire.");
            return false;
        }

        if (txtEmail.getText() != null && !txtEmail.getText().isBlank()) {
            if (!ValidationUtil.verifierEmail(txtEmail, getStage(), false)) return false;
        }

        if (txtTelephone.getText() != null && !txtTelephone.getText().isBlank()) {
            if (!ValidationUtil.verifierTelephone(txtTelephone, getStage(), false)) return false;
        }

        return true;
    }

    private void mapparationFormulaireVersObjet(Personnel personnel) {
        personnel.setNom(txtNom.getText().trim());
        personnel.setPrenom(txtPrenom.getText() != null ? txtPrenom.getText().trim() : null);
        personnel.setPosteOuFonction(txtPosteFonction.getText().trim());
        personnel.setEmail(txtEmail.getText() != null ? txtEmail.getText().trim() : null);
        personnel.setTelephone(txtTelephone.getText() != null ? txtTelephone.getText().trim() : null);
        personnel.setTypePersonnel(comboTypePersonnel.getValue());

        if (comboDepartement.getValue() != null) {
            personnel.setIdDepartement(comboDepartement.getValue().getIdDepartement());
        }

        if (comboResponsable.getValue() != null) {
            personnel.setIdResponsable(comboResponsable.getValue().getIdPersonnel());
        } else {
            personnel.setIdResponsable(null);
        }

        personnel.setMatriculeInterne(txtMatricule.getText().trim());
        personnel.setMatriculeFonctionPublique(txtMatriculeFp.getText());
        personnel.setQualification(txtQualification.getText());
        personnel.setStatutContractuel(comboStatutContractuel.getValue());

        if (!txtQuota.getText().isBlank()) {
            personnel.setQuotaHeuresHebdo(new BigDecimal(txtQuota.getText().trim()));
        }
        if (!txtDecharge.getText().isBlank()) {
            personnel.setDechargeHeures(new BigDecimal(txtDecharge.getText().trim()));
        }

        personnel.setDateEmbauche(dateEmbauche.getValue());
        personnel.setDateDepart(dateDepart.getValue());
        personnel.setMotifDepart(txtMotifDepart.getText());

        // Privé
        if (comboSexe.getValue() != null) {
            personnel.setSexe(comboSexe.getValue().name());
        }
        personnel.setDateNaissance(dateNaissance.getValue());
        personnel.setLieuNaissance(txtLieuNaissance.getText());
        personnel.setNationalite(txtNationalite.getText());
        personnel.setSituationMatrimoniale(comboSituation.getValue());

        if (!txtEnfants.getText().isBlank()) {
            personnel.setNombreEnfants(Integer.parseInt(txtEnfants.getText().trim()));
        }

        personnel.setNumeroCni(txtCni.getText());
        personnel.setNumCnps(txtCnps.getText());
        personnel.setNiu(txtNiu.getText());

        personnel.setAdresse(txtAdresse.getText());
        personnel.setVille(txtVille.getText());
        personnel.setContactUrgenceNom(txtUrgenceNom.getText());
        personnel.setContactUrgence(txtUrgenceTel.getText());

        // Paie
        personnel.setModePaiement(comboModePaiement.getValue());
        personnel.setNomBanque(txtBanque.getText());
        personnel.setIbanOuRib(txtIban.getText());
        personnel.setNumeroMobileMoney(txtMobileMoney.getText());

        // RH
        if (comboUtilisateur.getValue() != null) {
            personnel.setIdUtilisateur(comboUtilisateur.getValue().getIdUtilisateur());
        }
        if (comboStatutEmploi.getValue() != null) {
            personnel.setStatutEmploi(comboStatutEmploi.getValue().name());
        }
    }

    private void remplirChamps(Personnel p) {
        txtNom.setText(p.getNom());
        txtPrenom.setText(p.getPrenom());
        txtPosteFonction.setText(p.getPosteOuFonction());
        txtEmail.setText(p.getEmail());
        txtTelephone.setText(p.getTelephone());
        comboTypePersonnel.setValue(p.getTypePersonnel());

        txtMatricule.setText(p.getMatriculeInterne());
        txtMatriculeFp.setText(p.getMatriculeFonctionPublique());
        txtQualification.setText(p.getQualification());
        comboStatutContractuel.setValue(p.getStatutContractuel());

        if (p.getQuotaHeuresHebdo() != null) txtQuota.setText(p.getQuotaHeuresHebdo().toString());
        if (p.getDechargeHeures() != null) txtDecharge.setText(p.getDechargeHeures().toString());

        dateEmbauche.setValue(p.getDateEmbauche());
        dateDepart.setValue(p.getDateDepart());
        txtMotifDepart.setText(p.getMotifDepart());

        if (p.getSexe() != null) {
            try {
                comboSexe.setValue(Sexe.valueOf(p.getSexe()));
            } catch (IllegalArgumentException ignored) {}
        }
        dateNaissance.setValue(p.getDateNaissance());
        txtLieuNaissance.setText(p.getLieuNaissance());
        txtNationalite.setText(p.getNationalite());
        comboSituation.setValue(p.getSituationMatrimoniale());
        txtEnfants.setText(String.valueOf(p.getNombreEnfants()));

        txtCni.setText(p.getNumeroCni());
        txtCnps.setText(p.getNumCnps());
        txtNiu.setText(p.getNiu());

        txtAdresse.setText(p.getAdresse());
        txtVille.setText(p.getVille());
        txtUrgenceNom.setText(p.getContactUrgenceNom());
        txtUrgenceTel.setText(p.getContactUrgence());

        comboModePaiement.setValue(p.getModePaiement());
        txtBanque.setText(p.getNomBanque());
        txtIban.setText(p.getIbanOuRib());
        txtMobileMoney.setText(p.getNumeroMobileMoney());

        if (p.getStatutEmploi() != null) {
            try {
                comboStatutEmploi.setValue(StatutEmploi.valueOf(p.getStatutEmploi()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (p.getPhoto() != null && !p.getPhoto().isBlank()) {
            imgPhoto.setImage(new Image(p.getPhoto(), true));
            photoPlaceholder.setVisible(false);
        }
    }

    private void initialiserFormulaireVide() {
        comboStatutEmploi.setValue(StatutEmploi.ACTIF);
        comboModePaiement.setValue("ESPECES");
        comboTypePersonnel.setValue("ENSEIGNANT");
        comboStatutContractuel.setValue("PERMANENT");
        dateEmbauche.setValue(LocalDate.now());
    }

    private void afficherErreur(String msg) {
        lblMessage.setText(msg);
        bandeauMessage.setVisible(true);
        bandeauMessage.setManaged(true);
    }

    private void masquerMessage() {
        bandeauMessage.setVisible(false);
        bandeauMessage.setManaged(false);
    }
}