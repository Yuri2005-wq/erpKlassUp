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
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.enums.Sexe;
import org.erpklassup.erpklassup.enums.StatutEmploi;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.models.GRH.Personnel;
import org.erpklassup.erpklassup.models.GRH.Poste;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.GRH.DepartementService;
import org.erpklassup.erpklassup.service.GRH.PersonnelService;
import org.erpklassup.erpklassup.service.GRH.PosteService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.erpklassup.erpklassup.util.ValidationUtil.configurerToggleSwitch;

public class PersonnelModalController implements Initializable, VueDisposable {

    @FXML private HBox bandeauMessage;
    @FXML private Label lblMessage;
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtPosteFonction;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private ComboBox<String> comboTypePersonnel;
    @FXML private ComboBox<Departement> comboDepartement;
    @FXML private ComboBox<Poste> comboPoste;
    @FXML private ComboBox<Personnel> comboResponsable;
    @FXML private ImageView imgPhoto;
    @FXML private VBox photoPlaceholder;
    @FXML private TextField txtMatricule;
    @FXML private TextField txtMatriculeFp;
    @FXML private TextField txtQualification;
    @FXML private ComboBox<String> comboStatutContractuel;
    @FXML private TextField txtQuota;
    @FXML private TextField txtDecharge;
    @FXML private DatePicker dateEmbauche;
    @FXML private DatePicker dateDepart;
    @FXML private TextField txtMotifDepart;

    @FXML private VBox organigrammeContainer;

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

    @FXML private ComboBox<String> comboModePaiement;
    @FXML private GridPane gridBanque;
    @FXML private GridPane gridMobile;
    @FXML private TextField txtBanque;
    @FXML private TextField txtIban;
    @FXML private TextField txtMobileMoney;

    @FXML private ComboBox<Utilisateur> comboUtilisateur;
    @FXML private ComboBox<StatutEmploi> comboStatutEmploi;
    @FXML private ToggleButton tglCreerCompte;
    @FXML private VBox boxFormulaireUser;
    @FXML private TextField txtUserUsername;
    @FXML private PasswordField txtUserPassword;
    @FXML private CheckBox chkDoitModifierPasse;

    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    private final PersonnelService personnelService = new PersonnelService();
    private final DepartementService departementService = new DepartementService();
    private final PosteService posteService = new PosteService();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final AuditService auditService = new AuditService();

    private String idEcoleCourante;
    private Personnel personnelEnEdition;
    private Runnable onSucces;
    private File fichierPhotoChoisi = null;
    private List<Personnel> listeSubordonnesChargee = null;

    // Stockage des données de référence pour le filtrage
    private List<Poste> tousLesPostes = new ArrayList<>();
    private List<Departement> tousLesDepartements = new ArrayList<>();

    // Flag pour éviter les boucles infinies lors de la synchronisation
    private boolean isUpdating = false;

    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
        configurerChampsEtValidations();
        configurerCombos();
        configurerConverters();
        configurerLiaisonsLogiques();
        configurerEcouteursOrganigramme();
        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        Platform.runLater(() -> txtNom.requestFocus());
        configurerToggleSwitch(tglCreerCompte);
    }

    @Override
    public void disposer() {
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    public void initCreation(String idEcole, Runnable onSucces) {
        this.idEcoleCourante = idEcole;
        this.personnelEnEdition = null;
        this.onSucces = onSucces;
        this.listeSubordonnesChargee = null;
        String matriculeAuto = PersonnelUtils.genererMatricule(idEcoleCourante);
        if (txtMatricule != null) txtMatricule.setText(matriculeAuto);
        initialiserFormulaireVide();
        chargerDonneesRef();
        rafraichirOrganigrammeComplet();
        appliquerControlesAcces();
    }

    public void initEdition(Personnel personnel, Runnable onSucces) {
        if (personnel == null) return;
        this.idEcoleCourante = personnel.getIdEcole();
        this.personnelEnEdition = personnel;
        this.onSucces = onSucces;
        this.listeSubordonnesChargee = null;
        chargerDonneesRef();
        appliquerControlesAcces();
    }

    private void appliquerControlesAcces() {}

    private void configurerChampsEtValidations() {
        ValidationUtil.trimAuBlur(txtNom);
        ValidationUtil.trimAuBlur(txtPrenom);
        ValidationUtil.trimAuBlur(txtEmail);
        ValidationUtil.forcerMajuscules(txtMatricule);
        ValidationUtil.forcerMajuscules(txtMatriculeFp);
        ValidationUtil.forcerDecimal(txtQuota);
        ValidationUtil.forcerDecimal(txtDecharge);

        if (txtMatricule != null) {
            txtMatricule.setEditable(false);
            txtMatricule.setDisable(true);
            txtMatricule.setStyle("-fx-opacity: 0.85; -fx-background-color: #F1F5F9;");
        }
        if (txtPosteFonction != null) {
            txtPosteFonction.setEditable(false);
            txtPosteFonction.setDisable(true);
            txtPosteFonction.setStyle("-fx-opacity: 0.85; -fx-background-color: #F1F5F9;");
        }
    }

    private void configurerCombos() {
        if (comboTypePersonnel != null) comboTypePersonnel.setItems(FXCollections.observableArrayList("ENSEIGNANT", "ADMINISTRATIF", "SUPPORT", "VACATAIRE", "DIRECTION"));
        if (comboStatutContractuel != null) comboStatutContractuel.setItems(FXCollections.observableArrayList("PERMANENT", "CDD", "CDI", "VACATAIRE", "STAGIAIRE", "PRESTATAIRE"));
        if (comboStatutEmploi != null) comboStatutEmploi.setItems(FXCollections.observableArrayList(StatutEmploi.values()));
        if (comboSexe != null) comboSexe.setItems(FXCollections.observableArrayList(Sexe.values()));
        if (comboModePaiement != null) comboModePaiement.setItems(FXCollections.observableArrayList("ESPECES", "VIREMENT", "MOBILE_MONEY", "CHEQUE"));
    }

    private void configurerConverters() {
        if (comboDepartement != null) comboDepartement.setConverter(new StringConverter<>() {
            @Override public String toString(Departement d) { return d != null ? d.getLibelle() : ""; }
            @Override public Departement fromString(String string) { return null; }
        });
        if (comboPoste != null) comboPoste.setConverter(new StringConverter<>() {
            @Override public String toString(Poste p) { return p != null ? p.getIntitule() : ""; }
            @Override public Poste fromString(String string) { return null; }
        });
        if (comboResponsable != null) comboResponsable.setConverter(new StringConverter<>() {
            @Override public String toString(Personnel p) { return p != null ? p.getNomComplet() : ""; }
            @Override public Personnel fromString(String string) { return null; }
        });
        if (comboUtilisateur != null) comboUtilisateur.setConverter(new StringConverter<>() {
            @Override public String toString(Utilisateur u) { return u != null ? u.getUsername() + " (" + u.getNom() + ")" : ""; }
            @Override public Utilisateur fromString(String string) { return null; }
        });
        if (comboSexe != null) {
            comboSexe.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Sexe item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getLibelle());
                }
            });
            comboSexe.setButtonCell(comboSexe.getCellFactory().call(null));
        }
        if (comboStatutEmploi != null) {
            comboStatutEmploi.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(StatutEmploi item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getLibelle());
                }
            });
            comboStatutEmploi.setButtonCell(comboStatutEmploi.getCellFactory().call(null));
        }
    }

    private void configurerLiaisonsLogiques() {
        // 1. Liaison Poste -> Département & Intitulé & Responsable
        if (comboPoste != null && txtPosteFonction != null) {
            comboPoste.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return; // Évite les boucles infinies

                if (newVal != null) {
                    txtPosteFonction.setText(newVal.getIntitule() != null ? newVal.getIntitule() : "");

                    // A. Synchroniser le Département en fonction du Poste choisi
                    if (newVal.getIdDepartement() != null && comboDepartement != null) {
                        tousLesDepartements.stream()
                                .filter(d -> d.getIdDepartement().equals(newVal.getIdDepartement()))
                                .findFirst()
                                .ifPresent(comboDepartement::setValue);
                    }

                    // B. Gérer l'accessibilité du Responsable (règle existante)
                    boolean postePermetResponsable = !newVal.getIntitule().toLowerCase().contains("directeur")
                            && !newVal.getIntitule().toLowerCase().contains("proviseur");
                    if (comboResponsable != null) {
                        comboResponsable.setDisable(!postePermetResponsable);
                        if (!postePermetResponsable) comboResponsable.getSelectionModel().clearSelection();
                    }
                } else {
                    txtPosteFonction.clear();
                }
            });
        }

        // 2. Liaison Département -> Filtrage des Postes & Responsable
        if (comboDepartement != null) {
            comboDepartement.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return; // Évite les boucles infinies
                isUpdating = true;

                try {
                    // A. Filtrer la liste des postes
                    if (comboPoste != null) {
                        if (newVal != null) {
                            // Si un département est choisi, on ne garde que ses postes
                            ObservableList<Poste> postesFiltres = FXCollections.observableArrayList(
                                    tousLesPostes.stream()
                                            .filter(p -> newVal.getIdDepartement().equals(p.getIdDepartement()))
                                            .toList()
                            );

                            // Si le poste actuel n'est pas dans le nouveau département, on le vide
                            if (comboPoste.getValue() != null && !postesFiltres.contains(comboPoste.getValue())) {
                                comboPoste.setValue(null);
                                if (txtPosteFonction != null) txtPosteFonction.clear();
                            }
                            comboPoste.setItems(postesFiltres);
                        } else {
                            // Si aucun département n'est choisi, on affiche tous les postes
                            comboPoste.setItems(FXCollections.observableArrayList(tousLesPostes));
                        }
                    }

                    // B. Sélectionner automatiquement le Chef de Département
                    if (comboResponsable != null) {
                        if (newVal != null && newVal.getIdResponsable() != null) {
                            if (comboResponsable.getItems() != null) {
                                comboResponsable.getItems().stream()
                                        .filter(p -> p.getIdPersonnel().equals(newVal.getIdResponsable()))
                                        .findFirst()
                                        .ifPresent(comboResponsable::setValue);
                            }
                        } else if (newVal == null) {
                            // Si le département est vidé, on vide le responsable par défaut
                            comboResponsable.getSelectionModel().clearSelection();
                        }
                    }
                } finally {
                    isUpdating = false;
                }
            });
        }

        // 3. Visibilité dynamique des champs de paie
        if (comboModePaiement != null) {
            comboModePaiement.valueProperty().addListener((obs, oldVal, newVal) -> {
                boolean estVirement = "VIREMENT".equals(newVal);
                boolean estMobileMoney = "MOBILE_MONEY".equals(newVal);
                if (gridBanque != null) { gridBanque.setVisible(estVirement); gridBanque.setManaged(estVirement); }
                if (gridMobile != null) { gridMobile.setVisible(estMobileMoney); gridMobile.setManaged(estMobileMoney); }
            });
        }
    }

    private void chargerDonneesRef() {
        if (idEcoleCourante == null) return;

        // 1. Charger les départements
        departementService.chargerTousParEcoleAsync(idEcoleCourante,
                depts -> Platform.runLater(() -> {
                    this.tousLesDepartements = depts;
                    if (comboDepartement != null) {
                        comboDepartement.setItems(FXCollections.observableArrayList(depts));
                        if (personnelEnEdition != null && personnelEnEdition.getIdDepartement() != null) {
                            depts.stream().filter(d -> d.getIdDepartement().equals(personnelEnEdition.getIdDepartement())).findFirst().ifPresent(comboDepartement::setValue);
                        }
                    }
                }), err -> {});

        // 2. Charger les postes
        posteService.chargerTousParEcoleAsync(idEcoleCourante,
                postes -> Platform.runLater(() -> {
                    this.tousLesPostes = postes;
                    if (comboPoste != null) {
                        // On initialise avec tous les postes, l'écouteur du département filtrera si besoin
                        comboPoste.setItems(FXCollections.observableArrayList(postes));

                        if (personnelEnEdition != null && personnelEnEdition.getPosteOuFonction() != null) {
                            postes.stream().filter(post -> post.getIdPoste() != null && post.getIntitule().equalsIgnoreCase(personnelEnEdition.getPosteOuFonction())).findFirst().ifPresent(comboPoste::setValue);
                        }
                    }
                }), err -> {});

        // 3. Charger les personnels (responsables)
        personnelService.chargerParEcoleAsync(idEcoleCourante,
                liste -> Platform.runLater(() -> {
                    if (comboResponsable != null) {
                        ObservableList<Personnel> obs = FXCollections.observableArrayList(liste);
                        if (personnelEnEdition != null && personnelEnEdition.getIdPersonnel() != null) obs.removeIf(p -> p.getIdPersonnel().equals(personnelEnEdition.getIdPersonnel()));
                        comboResponsable.setItems(obs);

                        if (personnelEnEdition != null && personnelEnEdition.getIdResponsable() != null) {
                            obs.stream().filter(p -> p.getIdPersonnel().equals(personnelEnEdition.getIdResponsable())).findFirst().ifPresent(comboResponsable::setValue);
                        }
                    }
                }), err -> {});

        // 4. Charger les utilisateurs
        AppExecutor.get().submit(() -> {
            List<Utilisateur> users = utilisateurDAO.findByEcole(idEcoleCourante);
            Platform.runLater(() -> {
                if (comboUtilisateur != null) {
                    comboUtilisateur.setItems(FXCollections.observableArrayList(users));
                    if (personnelEnEdition != null && personnelEnEdition.getIdUtilisateur() != null) {
                        users.stream().filter(u -> u.getIdUtilisateur().equals(personnelEnEdition.getIdUtilisateur())).findFirst().ifPresent(comboUtilisateur::setValue);
                    }
                }
            });
        });

        if (personnelEnEdition != null) {
            Platform.runLater(() -> {
                remplirChamps(personnelEnEdition);
                chargerSubordonnesEtRendreOrganigramme();
            });
        }
    }

    @FXML
    private void handleToggleCreerCompte(ActionEvent event) {
        if (tglCreerCompte == null || boxFormulaireUser == null) return;
        boolean active = tglCreerCompte.isSelected();
        boxFormulaireUser.setVisible(active);
        boxFormulaireUser.setManaged(active);
        if (active) {
            if (comboUtilisateur != null) { comboUtilisateur.getSelectionModel().clearSelection(); comboUtilisateur.setDisable(true); }
            String prenom = txtPrenom != null && txtPrenom.getText() != null ? txtPrenom.getText().trim() : "";
            String nom = txtNom != null && txtNom.getText() != null ? txtNom.getText().trim() : "";
            String baseUser = "";
            if (!prenom.isEmpty() && !nom.isEmpty()) baseUser = (prenom.substring(0, 1) + nom).toLowerCase().replaceAll("\\s+", "");
            else if (!nom.isEmpty()) baseUser = nom.toLowerCase().replaceAll("\\s+", "");
            if (txtUserUsername != null) txtUserUsername.setText(baseUser);
            String matricule = txtMatricule != null && txtMatricule.getText() != null ? txtMatricule.getText().trim() : "";
            String mdpDefaut = (matricule + baseUser).toLowerCase().replaceAll("\\s+", "");
            if (mdpDefaut.isBlank()) mdpDefaut = UUID.randomUUID().toString().substring(0, 8);
            if (txtUserPassword != null) txtUserPassword.setText(mdpDefaut); System.out.println(mdpDefaut);
            if (chkDoitModifierPasse != null) chkDoitModifierPasse.setSelected(true);
        } else {
            if (comboUtilisateur != null) comboUtilisateur.setDisable(false);
        }
    }

    /* ============================================================
       ORGANIGRAMME EN ARBRE
       ============================================================ */
    private void configurerEcouteursOrganigramme() {
        if (txtNom != null) txtNom.textProperty().addListener((o, a, n) -> rafraichirOrganigrammeComplet());
        if (txtPrenom != null) txtPrenom.textProperty().addListener((o, a, n) -> rafraichirOrganigrammeComplet());
        if (txtPosteFonction != null) txtPosteFonction.textProperty().addListener((o, a, n) -> rafraichirOrganigrammeComplet());
        if (comboResponsable != null) comboResponsable.valueProperty().addListener((obs, oldVal, newVal) -> rafraichirOrganigrammeComplet());
        if (comboPoste != null) comboPoste.valueProperty().addListener((obs, oldVal, newVal) -> rafraichirOrganigrammeComplet());
    }

    private void chargerSubordonnesEtRendreOrganigramme() {
        if (personnelEnEdition != null && personnelEnEdition.getIdPersonnel() != null) {
            personnelService.chargerSubordonnesAsync(personnelEnEdition.getIdPersonnel(),
                    subordonnes -> Platform.runLater(() -> {
                        this.listeSubordonnesChargee = subordonnes;
                        rafraichirOrganigrammeComplet();
                    }), err -> rafraichirOrganigrammeComplet());
        } else {
            rafraichirOrganigrammeComplet();
        }
    }

    private void rafraichirOrganigrammeComplet() {
        if (organigrammeContainer == null) return;
        organigrammeContainer.getChildren().clear();

        String nomComplet = ((txtNom != null && txtNom.getText() != null ? txtNom.getText() : "") + " " + (txtPrenom != null && txtPrenom.getText() != null ? txtPrenom.getText() : "")).trim();
        if (nomComplet.isEmpty()) nomComplet = "Nouvel Employé";
        String poste = txtPosteFonction != null && txtPosteFonction.getText() != null && !txtPosteFonction.getText().isBlank() ? txtPosteFonction.getText() : "Poste non défini";
        String photoPath = fichierPhotoChoisi != null ? fichierPhotoChoisi.getAbsolutePath() : (personnelEnEdition != null ? personnelEnEdition.getPhoto() : null);

        Personnel responsable = comboResponsable != null ? comboResponsable.getValue() : null;

        if (responsable != null) {
            VBox nodeResp = creerNoeudOrganigramme(responsable.getNomComplet(), responsable.getPosteOuFonction(), responsable.getPhoto(), true);

            Region ligneConnect = new Region();
            ligneConnect.setStyle("-fx-background-color: #CBD5E1; -fx-min-width: 2; -fx-max-width: 2; -fx-min-height: 20; -fx-pref-height: 20;");
            VBox.setMargin(ligneConnect, new javafx.geometry.Insets(0, 0, 0, 21));

            organigrammeContainer.getChildren().addAll(nodeResp, ligneConnect);
        }

        int nbSubordonnes = (listeSubordonnesChargee != null) ? listeSubordonnesChargee.size() : 0;
        VBox nodeEmploye = creerNoeudOrganigramme(nomComplet, poste, photoPath, false, nbSubordonnes);
        organigrammeContainer.getChildren().add(nodeEmploye);

        if (personnelEnEdition != null && listeSubordonnesChargee != null && !listeSubordonnesChargee.isEmpty()) {
            for (Personnel sub : listeSubordonnesChargee) {
                HBox ligneEtSub = new HBox(0);
                ligneEtSub.setAlignment(Pos.TOP_LEFT);

                Region ligneL = new Region();
                ligneL.setStyle("-fx-border-color: #CBD5E1; -fx-border-width: 0 0 2 2; -fx-min-width: 24; -fx-max-width: 24; -fx-min-height: 22; -fx-pref-height: 22;");

                VBox nodeSub = creerNoeudOrganigramme(sub.getNomComplet(), sub.getPosteOuFonction(), sub.getPhoto(), false, 0);

                ligneEtSub.getChildren().addAll(ligneL, nodeSub);
                VBox.setMargin(ligneEtSub, new javafx.geometry.Insets(0, 0, 0, 22));

                organigrammeContainer.getChildren().add(ligneEtSub);
            }
        }
    }

    private VBox creerNoeudOrganigramme(String nom, String poste, String photoPath, boolean estSuperieur) {
        return creerNoeudOrganigramme(nom, poste, photoPath, estSuperieur, 0);
    }

    private VBox creerNoeudOrganigramme(String nom, String poste, String photoPath, boolean estSuperieur, int badgeCount) {
        HBox contentHBox = new HBox(12);
        contentHBox.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarPane = new StackPane();
        String borderColor = estSuperieur ? "#F5A524" : "#0EA5E9";
        avatarPane.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 50%; -fx-min-width: 44; -fx-min-height: 44; -fx-max-width: 44; -fx-max-height: 44; -fx-border-color: " + borderColor + "; -fx-border-radius: 50%; -fx-border-width: 2;");

        Label init = new Label(extraireInitiales(nom, ""));
        init.setStyle("-fx-font-weight: 800; -fx-text-fill: #334155; -fx-font-size: 14px;");
        avatarPane.getChildren().add(init);

        if (photoPath != null && !photoPath.isBlank()) {
            Image img = AvatarUtil.chargerImageSecurisee(photoPath);
            if (img != null) {
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(40); imgView.setFitHeight(40); imgView.setPreserveRatio(true);
                imgView.setClip(new Circle(20, 20, 20));
                avatarPane.getChildren().add(imgView);
            }
        }

        VBox info = new VBox(2);
        Label lblNom = new Label(nom);
        lblNom.setStyle("-fx-font-weight: 700; -fx-text-fill: #16213A; -fx-font-size: 13px;");
        Label lblPoste = new Label(poste != null ? poste : "");
        lblPoste.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        info.getChildren().addAll(lblNom, lblPoste);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label();
        if (badgeCount > 0) {
            badge.setText(String.valueOf(badgeCount));
            badge.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10; -fx-border-color: #CBD5E1; -fx-border-radius: 10; -fx-font-size: 10px;");
        } else {
            badge.setVisible(false);
            badge.setManaged(false);
        }

        contentHBox.getChildren().addAll(avatarPane, info, spacer, badge);

        VBox mainNode = new VBox();
        mainNode.getChildren().add(contentHBox);
        mainNode.setStyle("-fx-padding: 6 0 6 0;");
        return mainNode;
    }

    private String extraireInitiales(String nom, String prenom) {
        StringBuilder sb = new StringBuilder();
        if (prenom != null && !prenom.isBlank()) sb.append(Character.toUpperCase(prenom.charAt(0)));
        if (nom != null && !nom.isBlank()) sb.append(Character.toUpperCase(nom.charAt(0)));
        return !sb.isEmpty() ? sb.toString() : "?";
    }

    /* ============================================================
       ACTIONS & ENREGISTREMENT
       ============================================================ */
    @FXML
    private void handleChoisirPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner la photo du membre");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            this.fichierPhotoChoisi = file;
            if (imgPhoto != null) {
                Image img = AvatarUtil.chargerImageSecurisee(file.getAbsolutePath());
                if (img != null) {
                    imgPhoto.setImage(img);
                    if (photoPlaceholder != null) photoPlaceholder.setVisible(false);
                }
            }
            rafraichirOrganigrammeComplet();
        }
    }

    @FXML
    private void handleEnregistrer(ActionEvent event) {
        masquerMessage();
        if (!validerChamps()) return;
        Personnel personnel = (personnelEnEdition == null) ? new Personnel() : personnelEnEdition;
        mapparitionFormulaireVersObjet(personnel);

        if (btnEnregistrer != null) btnEnregistrer.setDisable(true);

        if (personnelEnEdition == null) {
            String idCreateur = SessionManager.getInstance().getIdUtilisateurCourant();
            personnelService.creerAsync(personnel, idEcoleCourante, idCreateur,
                    () -> Platform.runLater(() -> {
                        if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
                        auditService.tracerActionAsync("GRH", "CREATION_PERSONNEL", "Personnel créé : " + personnel.getNomComplet());
                        ToastNotification.succes(getStage(), "Personnel enregistré avec succès.");
                        if (onSucces != null) onSucces.run();
                        fermer();
                    }), err -> Platform.runLater(() -> {
                        if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
                        afficherErreur(err.getMessage());
                    }));
        } else {
            personnelService.modifierAsync(personnel,
                    () -> Platform.runLater(() -> {
                        if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
                        auditService.tracerActionAsync("GRH", "MODIFICATION_PERSONNEL", "Personnel modifié : " + personnel.getNomComplet());
                        ToastNotification.succes(getStage(), "Personnel mis à jour avec succès.");
                        if (onSucces != null) onSucces.run();
                        fermer();
                    }), err -> Platform.runLater(() -> {
                        if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
                        afficherErreur(err.getMessage());
                    }));
        }
    }

    @FXML private void handleAnnuler(ActionEvent event) { fermer(); }
    private void fermer() { Stage stage = getStage(); if (stage != null) stage.close(); }
    private Stage getStage() { return (btnAnnuler == null || btnAnnuler.getScene() == null) ? null : (Stage) btnAnnuler.getScene().getWindow(); }

    private boolean validerChamps() {
        if (!ValidationUtil.verifierNonVide(txtNom, "Nom", getStage())) return false;
        if (!ValidationUtil.verifierNonVide(txtPosteFonction, "Intitulé du poste", getStage())) return false;
        if (!ValidationUtil.verifierNonVide(txtMatricule, "Matricule interne", getStage())) return false;
        if (comboTypePersonnel != null && comboTypePersonnel.getValue() == null) { afficherErreur("Le type de personnel est obligatoire."); return false; }
        if (txtEmail != null && txtEmail.getText() != null && !txtEmail.getText().isBlank()) { if (!ValidationUtil.verifierEmail(txtEmail, getStage(), false)) return false; }
        if (txtTelephone != null && txtTelephone.getText() != null && !txtTelephone.getText().isBlank()) { if (!ValidationUtil.verifierTelephone(txtTelephone, getStage(), false)) return false; }
        return true;
    }

    private void mapparitionFormulaireVersObjet(Personnel personnel) {
        if (txtNom != null) personnel.setNom(txtNom.getText().trim());
        if (txtPrenom != null) personnel.setPrenom(txtPrenom.getText() != null ? txtPrenom.getText().trim() : null);
        if (txtPosteFonction != null) personnel.setPosteOuFonction(txtPosteFonction.getText().trim());
        if (comboPoste != null && comboPoste.getValue() != null) {
            personnel.setIdPoste(comboPoste.getValue().getIdPoste());
        } else {
            personnel.setIdPoste(null);
        }
        if (txtEmail != null) personnel.setEmail(txtEmail.getText() != null ? txtEmail.getText().trim() : null);
        if (txtTelephone != null) personnel.setTelephone(txtTelephone.getText() != null ? txtTelephone.getText().trim() : null);
        if (comboTypePersonnel != null) personnel.setTypePersonnel(comboTypePersonnel.getValue());
        if (comboDepartement != null && comboDepartement.getValue() != null) personnel.setIdDepartement(comboDepartement.getValue().getIdDepartement());
        if (comboResponsable != null && comboResponsable.getValue() != null && !comboResponsable.isDisable()) personnel.setIdResponsable(comboResponsable.getValue().getIdPersonnel());
        else personnel.setIdResponsable(null);
        if (txtMatricule != null) personnel.setMatriculeInterne(txtMatricule.getText().trim());
        if (txtMatriculeFp != null) personnel.setMatriculeFonctionPublique(txtMatriculeFp.getText());
        if (txtQualification != null) personnel.setQualification(txtQualification.getText());
        if (comboStatutContractuel != null) personnel.setStatutContractuel(comboStatutContractuel.getValue());
        if (txtQuota != null && !txtQuota.getText().isBlank()) personnel.setQuotaHeuresHebdo(new BigDecimal(txtQuota.getText().trim()));
        if (txtDecharge != null && !txtDecharge.getText().isBlank()) personnel.setDechargeHeures(new BigDecimal(txtDecharge.getText().trim()));
        if (dateEmbauche != null) personnel.setDateEmbauche(dateEmbauche.getValue());
        if (dateDepart != null) personnel.setDateDepart(dateDepart.getValue());
        if (txtMotifDepart != null) personnel.setMotifDepart(txtMotifDepart.getText());
        if (comboSexe != null && comboSexe.getValue() != null) personnel.setSexe(comboSexe.getValue().name());
        if (comboModePaiement != null) personnel.setModePaiement(comboModePaiement.getValue());
        if (comboStatutEmploi != null && comboStatutEmploi.getValue() != null) personnel.setStatutEmploi(comboStatutEmploi.getValue().getLibelle());
        if (comboUtilisateur != null && comboUtilisateur.getValue() != null) personnel.setIdUtilisateur(comboUtilisateur.getValue().getIdUtilisateur());
        if (fichierPhotoChoisi != null) personnel.setPhoto(fichierPhotoChoisi.getAbsolutePath());
    }

    private void remplirChamps(Personnel p) {
        if (txtNom != null) txtNom.setText(p.getNom());
        if (txtPrenom != null) txtPrenom.setText(p.getPrenom());
        if (txtPosteFonction != null) txtPosteFonction.setText(p.getPosteOuFonction());
        if (txtEmail != null) txtEmail.setText(p.getEmail());
        if (txtTelephone != null) txtTelephone.setText(p.getTelephone());
        if (comboTypePersonnel != null) comboTypePersonnel.setValue(p.getTypePersonnel());
        if (txtMatricule != null) txtMatricule.setText(p.getMatriculeInterne());
        if (txtMatriculeFp != null) txtMatriculeFp.setText(p.getMatriculeFonctionPublique());
        if (txtQualification != null) txtQualification.setText(p.getQualification());
        if (comboStatutContractuel != null) comboStatutContractuel.setValue(p.getStatutContractuel());
        if (txtQuota != null && p.getQuotaHeuresHebdo() != null) txtQuota.setText(p.getQuotaHeuresHebdo().toString());
        if (txtDecharge != null && p.getDechargeHeures() != null) txtDecharge.setText(p.getDechargeHeures().toString());
        if (dateEmbauche != null) dateEmbauche.setValue(p.getDateEmbauche());
        if (dateDepart != null) dateDepart.setValue(p.getDateDepart());
        if (txtMotifDepart != null) txtMotifDepart.setText(p.getMotifDepart());
        if (comboSexe != null && p.getSexe() != null) { try { comboSexe.setValue(Sexe.valueOf(p.getSexe())); } catch (IllegalArgumentException ignored) {} }
        if (comboModePaiement != null && p.getModePaiement() != null) comboModePaiement.setValue(p.getModePaiement());
        if (comboDepartement != null && p.getIdDepartement() != null && comboDepartement.getItems() != null) {
            comboDepartement.getItems().stream().filter(d -> d.getIdDepartement().equals(p.getIdDepartement())).findFirst().ifPresent(comboDepartement::setValue);
        }
        if (comboPoste != null && p.getPosteOuFonction() != null && comboPoste.getItems() != null) {
            comboPoste.getItems().stream().filter(post -> post.getIdPoste() != null && post.getIntitule().equalsIgnoreCase(p.getPosteOuFonction())).findFirst().ifPresent(comboPoste::setValue);
        }
        if (comboStatutEmploi != null && p.getStatutEmploi() != null) { try { comboStatutEmploi.setValue(StatutEmploi.valueOf(p.getStatutEmploi())); } catch (IllegalArgumentException ignored) {} }
        if (comboUtilisateur != null && p.getIdUtilisateur() != null && comboUtilisateur.getItems() != null) {
            comboUtilisateur.getItems().stream().filter(u -> u.getIdUtilisateur().equals(p.getIdUtilisateur())).findFirst().ifPresent(comboUtilisateur::setValue);
        }
        if (comboResponsable != null && p.getIdResponsable() != null && comboResponsable.getItems() != null) {
            comboResponsable.getItems().stream().filter(resp -> resp.getIdPersonnel().equals(p.getIdResponsable())).findFirst().ifPresent(comboResponsable::setValue);
        }
        if (p.getPhoto() != null && !p.getPhoto().isBlank() && imgPhoto != null) {
            Image img = AvatarUtil.chargerImageSecurisee(p.getPhoto());
            if (img != null) { imgPhoto.setImage(img); if (photoPlaceholder != null) photoPlaceholder.setVisible(false); }
        }
    }

    private void initialiserFormulaireVide() {
        if (comboStatutEmploi != null) comboStatutEmploi.setValue(StatutEmploi.ACTIF);
        if (comboTypePersonnel != null) comboTypePersonnel.setValue("ENSEIGNANT");
        if (comboStatutContractuel != null) comboStatutContractuel.setValue("PERMANENT");
        if (comboModePaiement != null) comboModePaiement.setValue("ESPECES");
        if (dateEmbauche != null) dateEmbauche.setValue(LocalDate.now());
    }

    private void afficherErreur(String msg) {
        if (lblMessage != null) lblMessage.setText(msg);
        if (bandeauMessage != null) { bandeauMessage.setVisible(true); bandeauMessage.setManaged(true); }
    }

    private void masquerMessage() {
        if (bandeauMessage != null) { bandeauMessage.setVisible(false); bandeauMessage.setManaged(false); }
    }


}